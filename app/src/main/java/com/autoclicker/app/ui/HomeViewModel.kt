package com.autoclicker.app.ui

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.autoclicker.app.floating.FloatingController
import com.autoclicker.app.floating.PickerController
import com.autoclicker.app.model.ClickConfig
import com.autoclicker.app.service.AutoClickService
import com.autoclicker.app.util.CoordinateHelper
import com.autoclicker.app.util.PermissionUtils
import com.autoclicker.app.util.PrefsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Owns the home screen state and orchestrates service / floating window interactions.
 */
class HomeViewModel(app: Application) : AndroidViewModel(app), AutoClickService.Listener {

    data class UiState(
        val running: Boolean = false,
        val performedCount: Int = 0,
        val accessibilityGranted: Boolean = false,
        val overlayGranted: Boolean = false,
    )

    private val prefs by lazy { PrefsManager.getInstance(getApplication()) }
    private val _config = MutableStateFlow(prefs.loadClickConfig())
    val config: StateFlow<ClickConfig> = _config.asStateFlow()

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _snackMessage = MutableStateFlow<String?>(null)
    val snackMessage: StateFlow<String?> = _snackMessage.asStateFlow()

    init {
        refreshPermissionStatuses()
        attachIfReady()
        observePickerResults()
    }

    fun refreshPermissionStatuses() {
        val ctx = getApplication<Application>()
        val accessibility = PermissionUtils.isAccessibilityServiceEnabled(ctx)
        val overlay = PermissionUtils.canDrawOverlays(ctx)
        _state.update { it.copy(accessibilityGranted = accessibility, overlayGranted = overlay) }
    }

    fun update(cfg: ClickConfig) {
        _config.value = cfg.sanitized()
    }

    fun save() {
        prefs.saveClickConfig(_config.value)
        AutoClickService.get()?.updateConfig(_config.value)
        _snackMessage.value = "配置已保存"
    }

    /** Persist current config whenever config StateFlow emits. */
    fun persist() {
        prefs.saveClickConfig(_config.value)
    }

    /**
     * Spawn the independent floating picker marker. The picker runs in its own
     * window so the user can keep this app's control panel visible while dragging.
     * The picked (x, y) comes back via [observePickerResults] and lands in [config].
     */
    fun startPicker() {
        val ctx = getApplication<Application>()
        if (!PermissionUtils.canDrawOverlays(ctx)) {
            _snackMessage.value = "请先在权限设置中开启悬浮窗权限"
            return
        }
        FloatingController.requestPick()
    }

    /**
     * Collect [PickerController.lastResult] and write the result back into the
     * click config + persisted prefs + service. Runs for the entire VM lifetime.
     */
    private fun observePickerResults() {
        viewModelScope.launch {
            PickerController.lastResult.collect { result ->
                when (result) {
                    null -> Unit
                    is PickerController.Result.Confirmed -> {
                        val size = CoordinateHelper.currentScreenSize(getApplication())
                        val newCfg = _config.value.copy(
                            x = result.x,
                            y = result.y,
                            screenWidth = size.width,
                            screenHeight = size.height,
                        ).sanitized()
                        _config.value = newCfg
                        prefs.saveClickConfig(newCfg)
                        AutoClickService.get()?.updateConfig(newCfg)
                        _snackMessage.value = "已选择坐标 (${result.x}, ${result.y})"
                        Toast.makeText(getApplication(), _snackMessage.value, Toast.LENGTH_SHORT).show()
                    }
                    is PickerController.Result.Cancelled -> {
                        _snackMessage.value = "已取消坐标选择"
                    }
                }
            }
        }
    }

    /**
     * Show the floating control panel. Independent of start/stop — the user can keep
     * the panel on top of any other app and use it to toggle the clicker.
     */
    fun showFloating() {
        val ctx = getApplication<Application>()
        if (!PermissionUtils.canDrawOverlays(ctx)) {
            _snackMessage.value = "请先在权限设置中开启悬浮窗权限"
            return
        }
        FloatingController.show(ctx)
        _snackMessage.value = "悬浮控制台已打开"
    }

    fun hideFloating() {
        FloatingController.hide()
        _snackMessage.value = "悬浮控制台已关闭"
    }

    fun isFloatingShown(): Boolean = FloatingController.isShown()

    /**
     * Dispatch a single test click using the current coordinates.
     * This is the single best way to verify the user has correctly enabled the
     * accessibility service and the click actually lands at the desired (x, y).
     */
    fun testClick() {
        val ctx = getApplication<Application>()
        if (!PermissionUtils.isAccessibilityServiceEnabled(ctx)) {
            _snackMessage.value = "请先开启无障碍服务"
            return
        }
        val svc = AutoClickService.get()
        if (svc == null) {
            _snackMessage.value = "服务尚未就绪，请稍后重试或重启 App"
            return
        }
        val current = _config.value
        val size = CoordinateHelper.currentScreenSize(ctx)
        svc.updateConfig(current.copy(screenWidth = size.width, screenHeight = size.height))
        val ok = svc.testOnce()
        _snackMessage.value = if (ok) {
            "已尝试在 (${current.x}, ${current.y}) 单击 — 切换到目标 App 后再点启动可循环"
        } else {
            "单击失败：dispatchGesture 被系统拒绝。请确认无障碍服务已开启且已重启 App。"
        }
        Toast.makeText(ctx, _snackMessage.value, Toast.LENGTH_SHORT).show()
    }

    fun start() {
        val ctx = getApplication<Application>()
        if (!PermissionUtils.isAccessibilityServiceEnabled(ctx)) {
            _snackMessage.value = "请先开启无障碍服务"
            return
        }
        val svc = AutoClickService.get()
        if (svc == null) {
            _snackMessage.value = "服务尚未启动，请稍后重试（在系统设置中开启无障碍后请重新打开 App）"
            return
        }
        val current = _config.value
        val size = CoordinateHelper.currentScreenSize(ctx)
        svc.updateConfig(current.copy(screenWidth = size.width, screenHeight = size.height))
        svc.startLoop()

        if (PermissionUtils.canDrawOverlays(ctx)) {
            FloatingController.show(ctx, forceRecreate = true)
        }
        _snackMessage.value = "已开始，请切换到目标 App。"
        Toast.makeText(ctx, _snackMessage.value, Toast.LENGTH_LONG).show()
    }

    fun stop() {
        AutoClickService.get()?.stopLoop()
        FloatingController.hide()
        _snackMessage.value = "已停止"
    }

    override fun onStateChanged(running: Boolean, performedCount: Int, config: ClickConfig) {
        viewModelScope.launch {
            _state.update { it.copy(running = running, performedCount = performedCount) }
            _config.update { config }
        }
    }

    override fun onCleared() {
        AutoClickService.get()?.removeListener(this)
        super.onCleared()
    }

    private fun attachIfReady() {
        viewModelScope.launch {
            AutoClickService.get()?.addListener(this@HomeViewModel)
        }
    }
}
