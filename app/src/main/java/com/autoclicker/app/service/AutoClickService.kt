package com.autoclicker.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Path
import android.os.Build
import android.os.SystemClock
import android.content.pm.ServiceInfo
import android.util.Log
import androidx.core.app.NotificationCompat
import com.autoclicker.app.MainActivity
import com.autoclicker.app.R
import com.autoclicker.app.model.ClickConfig
import com.autoclicker.app.util.CoordinateHelper
import com.autoclicker.app.util.PrefsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.random.Random

/**
 * Core clicker service.
 *
 * The service exposes:
 *  - [startLoop] / [stopLoop] : control the click loop
 *  - [updateConfig] : live-update the configuration
 *  - [Listener] : notified of state changes so the host UI can reflect status
 *
 * Click dispatch is implemented via [dispatchGesture] with a [GestureDescription].
 * Because gesture dispatch is async, we wait on a callback before scheduling the next click.
 */
class AutoClickService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var loopJob: Job? = null

    @Volatile private var config: ClickConfig = ClickConfig.DEFAULT.let { _ ->
        ClickConfig()
    }

    /** Total clicks executed in the current run (monotonically increasing until stop). */
    @Volatile private var performedCount: Int = 0

    /** Running flag. Set true while a loop should keep going. */
    @Volatile private var running: Boolean = false

    /** Listeners (kept as a small mutable set; access only on Main thread). */
    private val listeners = mutableSetOf<Listener>()

    private val notificationManager: NotificationManager by lazy {
        getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        // Restore last config from prefs so the service starts in a sane state.
        config = PrefsManager.getInstance(this).loadClickConfig()
        ensureNotificationChannel()
        Log.i(TAG, "Service connected, instance=$this")
    }

    /**
     * Required by [AccessibilityService]. We don't react to incoming events — our work is
     * driven entirely by [startLoop]. We just keep this override to satisfy the abstract API.
     */
    override fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent?) {
        // No-op: the service is gesture-dispatcher only, event consumption is unnecessary here.
    }

    /**
     * Required by [AccessibilityService]. Called when the system interrupts us (e.g. another
     * a11y service takes focus). We stop the click loop to be a good citizen.
     */
    override fun onInterrupt() {
        Log.i(TAG, "Service interrupted by system, stopping loop")
        stopLoop()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        stopLoop()
        scope.cancel()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        stopLoop()
        instance = null
        super.onDestroy()
    }

    // ------------------------------------------------------------------------
    // Public control surface
    // ------------------------------------------------------------------------

    fun addListener(l: Listener) {
        listeners.add(l)
        // Always sync the new subscriber immediately, even if it was already registered,
        // so a host that comes back to foreground sees the current state right away.
        l.onStateChanged(running, performedCount, config)
    }

    fun removeListener(l: Listener) { listeners.remove(l) }

    fun updateConfig(cfg: ClickConfig) {
        val sanitized = cfg.sanitized()
        config = sanitized
        PrefsManager.getInstance(this).saveClickConfig(sanitized)
        notifyState()
    }

    /** Start the click loop. If already running, this is a no-op. */
    fun startLoop() {
        if (running) return
        val cfg = config.sanitized()
        if (!cfg.isValid()) {
            Log.w(TAG, "Refusing to start: invalid config $cfg")
            return
        }
        running = true
        performedCount = 0
        startForegroundOrNotify()
        loopJob = scope.launch {
            try {
                runClickLoop(cfg)
            } catch (t: Throwable) {
                Log.e(TAG, "Click loop crashed: ${t.message}", t)
            } finally {
                running = false
                notifyState()
            }
        }
        notifyState()
    }

    /** Stop the click loop. Idempotent. */
    fun stopLoop() {
        running = false
        loopJob?.cancel()
        loopJob = null
        notifyState()
    }

    fun isRunning(): Boolean = running
    fun performedCount(): Int = performedCount

    /**
     * Snapshot of the last config the service is using. Returned by-value (ClickConfig is
     * immutable) so callers can read it without worrying about concurrent mutation.
     */
    fun currentConfig(): ClickConfig = config

    /**
     * Run a single click immediately, regardless of the loop state.
     *
     * Used by the "测试单击" button in the home screen to verify that the user has
     * granted the accessibility permission and the service can actually dispatch a
     * gesture at the current (x, y).
     *
     * @return true if dispatchGesture was accepted by the system (does not guarantee
     *  the gesture visually hit anything — it returns true as long as the system
     *  scheduled the gesture).
     */
    fun testOnce(): Boolean {
        val cfg = config.sanitized()
        if (!cfg.isValid()) {
            Log.w(TAG, "testOnce: invalid config $cfg")
            return false
        }
        val current = CoordinateHelper.currentScreenSize(this)
        val reference = CoordinateHelper.ScreenSize(cfg.screenWidth, cfg.screenHeight)
        val (sx, sy) = CoordinateHelper.scale(cfg.x, cfg.y, reference, current)
        Log.i(TAG, "testOnce: dispatching at ($sx, $sy) on ${current.width}x${current.height}")
        val ok = dispatchSingleTap(sx, sy)
        if (ok) {
            performedCount++
            PrefsManager.getInstance(this).saveLastRunsCount(performedCount)
            notifyState()
        } else {
            Log.w(TAG, "testOnce: dispatchGesture returned false")
        }
        return ok
    }

    /**
     * Synchronous single-shot dispatch. Returns immediately after the system has
     * scheduled the gesture (or rejected it).
     */
    private fun dispatchSingleTap(x: Int, y: Int): Boolean {
        val path = Path().apply { moveTo(x.toFloat(), y.toFloat()) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, 50L))
            .build()
        return try {
            dispatchGesture(gesture, null, null)
        } catch (t: Throwable) {
            Log.e(TAG, "dispatchSingleTap threw: ${t.message}", t)
            false
        }
    }

    // ------------------------------------------------------------------------
    // Click loop
    // ------------------------------------------------------------------------

    private suspend fun runClickLoop(cfg0: ClickConfig) {
        var cfg = cfg0
        // Pre-compute actual screen size for coordinate scaling.
        val current = CoordinateHelper.currentScreenSize(this)
        val reference = CoordinateHelper.ScreenSize(cfg.screenWidth, cfg.screenHeight)
        val (sx, sy) = CoordinateHelper.scale(cfg.x, cfg.y, reference, current)

        val infinite = cfg.repeatCount == 0
        val targetCount = cfg.repeatCount
        while (running && (infinite || performedCount < targetCount)) {
            val interval = if (cfg.randomizeJitter) {
                jitter(cfg.intervalMs)
            } else cfg.intervalMs.toLong()

            // Wait the interval up front so the user has time to release the start button.
            // Subtract the typical gesture dispatch latency.
            val waited = delayInterruptible(interval - 30L)
            if (!running) break

            val dispatched = dispatchTap(sx, sy)
            if (dispatched) {
                performedCount++
                PrefsManager.getInstance(this).saveLastRunsCount(performedCount)
                notifyState()
            } else {
                Log.w(TAG, "dispatchGesture returned false")
            }

            if (waited == -1L) break
        }
    }

    /**
     * Dispatch a single tap at (x,y). Suspends until the system has processed the gesture
     * or the operation times out.
     */
    private suspend fun dispatchTap(x: Int, y: Int): Boolean = suspendCancellableCoroutine { cont ->
        val path = Path().apply { moveTo(x.toFloat(), y.toFloat()) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, 50L))
            .build()
        val ok = dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                if (cont.isActive) cont.resumeWith(Result.success(true))
            }
            override fun onCancelled(gestureDescription: GestureDescription?) {
                if (cont.isActive) cont.resumeWith(Result.success(false))
            }
        }, null)
        if (!ok && cont.isActive) cont.resumeWith(Result.success(false))
    }

    /**
     * Sleep for [ms] but break early when [running] flips to false.
     * Returns the time elapsed, or -1 if interrupted by stopLoop.
     */
    private suspend fun delayInterruptible(ms: Long): Long {
        val start = SystemClock.elapsedRealtime()
        var remaining = ms
        while (remaining > 0 && running) {
            val slice = minOf(remaining, 50L)
            delay(slice)
            remaining -= slice
        }
        return if (running) SystemClock.elapsedRealtime() - start else -1L
    }

    private fun jitter(baseMs: Int): Long {
        // ±15% randomization
        val delta = (baseMs * 0.15).toInt().coerceAtLeast(1)
        return (baseMs + Random.nextInt(-delta, delta + 1)).toLong().coerceAtLeast(10)
    }

    // ------------------------------------------------------------------------
    // Notification
    // ------------------------------------------------------------------------

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notif_channel_desc)
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(ch)
        }
    }

    private fun startForegroundOrNotify() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+: must declare foreground service type. The matching
            // `android:foregroundServiceType="specialUse"` and
            // `android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE` properties are set in
            // AndroidManifest.xml. Without those, startForeground throws on Android 14+.
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(): Notification {
        val cfg = config
        val text = getString(
            R.string.notif_text,
            performedCount,
            cfg.intervalMs
        )
        val openIntent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        // Stop action — broadcast to our own in-process receiver so the user can pause
        // the click loop from the notification shade, even if the floating panel is gone.
        val stopIntent = Intent(ACTION_STOP).setPackage(packageName)
        val stopPi = PendingIntent.getBroadcast(
            this, 1, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(text)
            .setOngoing(true)
            .setContentIntent(pi)
            .setOnlyAlertOnce(true)
            .addAction(
                R.drawable.ic_notification,
                getString(R.string.notif_action_stop),
                stopPi,
            )
            .build()
    }

    private fun notifyState() {
        val run = running
        val count = performedCount
        val cfg = config
        listeners.toList().forEach { it.onStateChanged(run, count, cfg) }
        if (running) notificationManager.notify(NOTIFICATION_ID, buildNotification())
    }

    /** Listener interface used by UI to react to state changes. */
    interface Listener {
        fun onStateChanged(running: Boolean, performedCount: Int, config: ClickConfig)
    }

    companion object {
        private const val TAG = "AutoClickService"
        private const val CHANNEL_ID = "auto_click_running"
        private const val NOTIFICATION_ID = 0xACE1

        /** Action broadcast by the notification's "停止" button; handled by [StopReceiver]. */
        const val ACTION_STOP = "com.autoclicker.app.ACTION_STOP"

        @Volatile private var instance: AutoClickService? = null

        /** Get the live service instance, or null if not yet connected. */
        fun get(): AutoClickService? = instance
    }
}
