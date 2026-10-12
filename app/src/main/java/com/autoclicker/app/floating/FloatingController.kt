package com.autoclicker.app.floating

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.autoclicker.app.model.ClickConfig
import com.autoclicker.app.service.AutoClickService
import com.autoclicker.app.ui.theme.AutoClickerTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Owns the floating control panel — the always-on-top "play/stop/pick/close" chip
 * that the user can drag around and use to toggle the clicker without leaving the
 * target app.
 *
 * The pick-coordinate functionality is intentionally **not** in this window anymore;
 * it's a separate draggable marker owned by [PickerController]. The 📍 button in
 * this panel just hands control over by calling [requestPick].
 */
object FloatingController {

    private const val TAG = "FloatingController"

    @SuppressLint("StaticFieldLeak")
    private var composeView: ComposeView? = null
    private var layoutParams: LayoutParams? = null
    private var windowManager: WindowManager? = null
    private var lifecycleOwner: FloatingLifecycleOwner? = null

    /** Cached application context, set when [show] runs. Used to launch [PickerController] later. */
    private var appContext: Context? = null

    // ---- UI state ----
    private val _isRunning = MutableStateFlow(AutoClickService.get()?.isRunning() ?: false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    // ---- Service listener (stable singleton) ----
    private val serviceListener: AutoClickService.Listener = object : AutoClickService.Listener {
        override fun onStateChanged(
            running: Boolean,
            performedCount: Int,
            config: ClickConfig,
        ) {
            _isRunning.value = running
        }
    }

    // ---- Public API ----

    fun isShown(): Boolean = composeView?.isAttachedToWindow == true

    fun show(ctx: Context, forceRecreate: Boolean = false) {
        if (!forceRecreate && isShown()) return
        if (isShown()) hideInternal()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            !android.provider.Settings.canDrawOverlays(ctx)
        ) {
            Log.w(TAG, "Refusing to show floating window: overlay not granted")
            return
        }
        val appCtx = ctx.applicationContext
        appContext = appCtx
        val wm = appCtx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager = wm

        val params = LayoutParams().apply {
            width = LayoutParams.WRAP_CONTENT
            height = LayoutParams.WRAP_CONTENT
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                LayoutParams.TYPE_PHONE
            }
            // - FLAG_NOT_FOCUSABLE: never steal input focus
            // - FLAG_NOT_TOUCH_MODAL: touches OUTSIDE the panel pass through to the
            //   underlying app (without this, dispatchGesture on the target app
            //   would silently fail because the click would be captured by us)
            // - FLAG_LAYOUT_NO_LIMITS: allow the panel to be positioned at the edges
            //
            // NOTE: we deliberately do NOT set FLAG_WATCH_OUTSIDE_TOUCH here.
            // Coordinate selection is now handled by an independent PickerController
            // window, not by tapping through this one.
            flags = LayoutParams.FLAG_NOT_FOCUSABLE or
                LayoutParams.FLAG_NOT_TOUCH_MODAL or
                LayoutParams.FLAG_LAYOUT_NO_LIMITS
            format = PixelFormat.TRANSLUCENT
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 200
        }
        layoutParams = params

        val owner = FloatingLifecycleOwner()
        lifecycleOwner = owner

        val cv = ComposeView(appCtx).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setContent {
                AutoClickerTheme(dynamicColor = false) {
                    FloatingContent(
                        onClose = { hide() },
                        onDragDelta = { dx, dy -> applyDrag(dx, dy) },
                    )
                }
            }
        }
        composeView = cv
        owner.markReady()

        try {
            wm.addView(cv, params)
            AutoClickService.get()?.addListener(serviceListener)
            _isRunning.value = AutoClickService.get()?.isRunning() ?: false
            Log.i(TAG, "Floating window added")
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to add floating window: ${t.message}", t)
            composeView = null
            owner.markDestroyed()
        }
    }

    fun hide() {
        if (composeView == null) return
        hideInternal()
    }

    private fun hideInternal() {
        val view = composeView ?: return
        composeView = null
        val owner = lifecycleOwner
        lifecycleOwner = null
        AutoClickService.get()?.removeListener(serviceListener)
        try {
            val wm = windowManager
                ?: view.context.applicationContext
                    .getSystemService(Context.WINDOW_SERVICE) as WindowManager
            if (view.isAttachedToWindow) wm.removeView(view)
        } catch (t: Throwable) {
            Log.w(TAG, "Error removing floating window: ${t.message}")
        }
        owner?.markDestroyed()
    }

    /**
     * Hand control over to the independent [PickerController] floating window.
     * Seeds the picker with the current service config coordinates so the user
     * can see the existing point and either keep it or move it.
     *
     * No-op if the floating panel has never been shown (no application context cached)
     * or if the picker is already visible.
     */
    fun requestPick() {
        val ctx = appContext ?: run {
            Log.w(TAG, "requestPick: no cached appContext, panel never shown")
            return
        }
        if (PickerController.isShown()) return
        val svc = AutoClickService.get()
        val cfg = svc?.currentConfig()
        val seedX = if (cfg != null && cfg.x > 0) cfg.x else 0
        val seedY = if (cfg != null && cfg.y > 0) cfg.y else 0
        PickerController.show(ctx, seedX, seedY)
    }

    /**
     * Move the floating window by (dx, dy) in screen pixels, clamped so the panel
     * stays at least partially visible.
     */
    private fun applyDrag(dx: Float, dy: Float) {
        val v = composeView ?: return
        val params = layoutParams ?: return
        val wm = windowManager ?: return
        val newX = params.x + dx.toInt()
        val newY = params.y + dy.toInt()

        val metrics = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            wm.currentWindowMetrics.bounds
        } else {
            @Suppress("DEPRECATION")
            wm.defaultDisplay.let { android.graphics.Rect(0, 0, it.width, it.height) }
        }
        val maxX = metrics.width() - 50
        val minX = -(params.width.coerceAtLeast(0)) + 50
        val maxY = (metrics.height() - 100).coerceAtLeast(100)
        val minY = 0
        params.x = newX.coerceIn(minX, maxX)
        params.y = newY.coerceIn(minY, maxY)

        try {
            if (v.isAttachedToWindow) wm.updateViewLayout(v, params)
        } catch (_: Throwable) { /* view not attached yet */ }
    }

    private class FloatingLifecycleOwner :
        LifecycleOwner,
        SavedStateRegistryOwner {

        private val lifecycleRegistry = LifecycleRegistry(this)
        private val savedStateController = SavedStateRegistryController.create(this)

        init {
            savedStateController.performAttach()
            savedStateController.performRestore(null)
        }

        fun markReady() {
            if (lifecycleRegistry.currentState == Lifecycle.State.DESTROYED) return
            lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        }

        fun markDestroyed() {
            savedStateController.performSave(Bundle())
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        }

        override val lifecycle: Lifecycle get() = lifecycleRegistry
        override val savedStateRegistry: SavedStateRegistry
            get() = savedStateController.savedStateRegistry
    }
}

@Composable
private fun FloatingContent(
    onClose: () -> Unit,
    onDragDelta: (dx: Float, dy: Float) -> Unit,
) {
    val running by FloatingController.isRunning.collectAsState()
    val pickerShown by PickerController.isShownFlow.collectAsState()
    FloatingPanel(
        running = running,
        pickerShown = pickerShown,
        onToggle = {
            val svc = AutoClickService.get() ?: return@FloatingPanel
            if (svc.isRunning()) svc.stopLoop() else svc.startLoop()
        },
        onClose = onClose,
        onRequestPick = { FloatingController.requestPick() },
        onDragDelta = onDragDelta,
    )
}
