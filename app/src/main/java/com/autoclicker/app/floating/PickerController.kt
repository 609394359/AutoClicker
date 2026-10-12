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
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.autoclicker.app.ui.theme.AutoClickerTheme
import com.autoclicker.app.util.CoordinateHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Independent floating "选点" window — a draggable circular marker that the user
 * moves to the desired (x, y) on screen.
 *
 * This is decoupled from [FloatingController]:
 *  - The control panel may stay parked in a corner while the user drags the
 *    picker around — they don't share a window.
 *  - Pressing 📍 in the control panel calls [show]; the picker is a separate
 *    window managed by its own [WindowManager.LayoutParams].
 *  - On ✓ the picker publishes [Result.Confirmed] with the marker's CENTER
 *    coordinates and removes itself. On ✕ it publishes [Result.Cancelled].
 *
 * Callers collect [lastResult] to react (e.g. write the picked (x, y) into
 * the click config).
 */
object PickerController {

    private const val TAG = "PickerController"
    /** Edge length of the picker marker, in dp. Must match [PickerPanel]'s .size(). */
    private const val PICKER_DP_SIZE = 96

    sealed class Result {
        data class Confirmed(val x: Int, val y: Int) : Result()
        data object Cancelled : Result()
    }

    private val _lastResult = MutableStateFlow<Result?>(null)
    val lastResult: StateFlow<Result?> = _lastResult.asStateFlow()

    /** True while the picker window is currently attached to the WindowManager. */
    private val _isShownFlow = MutableStateFlow(false)
    val isShownFlow: StateFlow<Boolean> = _isShownFlow.asStateFlow()

    @SuppressLint("StaticFieldLeak")
    private var composeView: ComposeView? = null
    private var layoutParams: LayoutParams? = null
    private var windowManager: WindowManager? = null
    private var lifecycleOwner: FloatingLifecycleOwner? = null

    fun isShown(): Boolean = composeView?.isAttachedToWindow == true

    /**
     * Open the picker window. If [initialX] / [initialY] look valid (within screen
     * bounds) the marker opens there; otherwise it opens at the screen center.
     */
    fun show(ctx: Context, initialX: Int, initialY: Int) {
        if (isShown()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            !android.provider.Settings.canDrawOverlays(ctx)
        ) {
            Log.w(TAG, "Refusing to show picker: overlay not granted")
            return
        }

        val appCtx = ctx.applicationContext
        val wm = appCtx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager = wm

        val density = appCtx.resources.displayMetrics.density
        val sizePx = (PICKER_DP_SIZE * density).toInt()
        val halfPx = sizePx / 2

        val screenSize = CoordinateHelper.currentScreenSize(appCtx)
        val cx = if (initialX in 1 until screenSize.width) initialX else screenSize.width / 2
        val cy = if (initialY in 1 until screenSize.height) initialY else screenSize.height / 2

        val params = LayoutParams().apply {
            width = sizePx
            height = sizePx
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                LayoutParams.TYPE_PHONE
            }
            // The picker is intentionally not FLAG_WATCH_OUTSIDE_TOUCH: while
            // it's open the user drags the marker itself, and the underlying
            // app should keep receiving normal touches through FLAG_NOT_TOUCH_MODAL.
            flags = LayoutParams.FLAG_NOT_FOCUSABLE or
                LayoutParams.FLAG_NOT_TOUCH_MODAL or
                LayoutParams.FLAG_LAYOUT_NO_LIMITS
            format = PixelFormat.TRANSLUCENT
            gravity = Gravity.TOP or Gravity.START
            x = (cx - halfPx).coerceAtLeast(0)
            y = (cy - halfPx).coerceAtLeast(0)
        }
        layoutParams = params

        val owner = FloatingLifecycleOwner()
        lifecycleOwner = owner

        val cv = ComposeView(appCtx).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setContent {
                AutoClickerTheme(dynamicColor = false) {
                    PickerPanel(
                        onConfirm = { confirm() },
                        onCancel = { cancel() },
                        onDragDelta = { dx, dy -> applyDrag(dx, dy) },
                    )
                }
            }
        }
        composeView = cv
        owner.markReady()

        try {
            wm.addView(cv, params)
            _isShownFlow.value = true
            Log.i(TAG, "Picker window added at (${params.x}, ${params.y}), size ${sizePx}px")
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to add picker window: ${t.message}", t)
            composeView = null
            owner.markDestroyed()
        }
    }

    fun hide() {
        if (composeView == null) return
        val view = composeView ?: return
        composeView = null
        val owner = lifecycleOwner
        lifecycleOwner = null
        _isShownFlow.value = false
        try {
            val wm = windowManager
                ?: view.context.applicationContext
                    .getSystemService(Context.WINDOW_SERVICE) as WindowManager
            if (view.isAttachedToWindow) wm.removeView(view)
        } catch (t: Throwable) {
            Log.w(TAG, "Error removing picker window: ${t.message}")
        }
        owner?.markDestroyed()
    }

    private fun applyDrag(dx: Float, dy: Float) {
        val v = composeView ?: return
        val params = layoutParams ?: return
        val wm = windowManager ?: return
        params.x = (params.x + dx.toInt()).coerceAtLeast(0)
        params.y = (params.y + dy.toInt()).coerceAtLeast(0)
        try {
            if (v.isAttachedToWindow) wm.updateViewLayout(v, params)
        } catch (_: Throwable) { /* view not attached yet */ }
    }

    /**
     * Read the marker's center coordinates and publish [Result.Confirmed].
     * The center is (params.x + sizePx/2, params.y + sizePx/2) — that's the
     * point the user visually targeted.
     */
    private fun confirm() {
        val params = layoutParams ?: return
        val view = composeView ?: return
        val density = view.resources.displayMetrics.density
        val halfPx = (PICKER_DP_SIZE * density / 2).toInt()
        val cx = params.x + halfPx
        val cy = params.y + halfPx
        Log.i(TAG, "Picker confirmed at ($cx, $cy)")
        _lastResult.value = Result.Confirmed(cx, cy)
        hide()
    }

    private fun cancel() {
        Log.i(TAG, "Picker cancelled")
        _lastResult.value = Result.Cancelled
        hide()
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
