package com.autoclicker.app.model

/**
 * Configuration for a single click point.
 *
 * @param x X coordinate in pixels (screen coordinate system at recording time).
 * @param y Y coordinate in pixels.
 * @param intervalMs Milliseconds between two clicks. Clamped to >= 10ms.
 * @param repeatCount Number of clicks to perform. Use 0 for "infinite loop".
 * @param randomizeJitter When true, the actual interval is randomized within ±15% to avoid detection.
 * @param screenWidth Reference screen width captured when (x,y) were picked. Use 0 if unknown.
 * @param screenHeight Reference screen height captured when (x,y) were picked. Use 0 if unknown.
 */
data class ClickConfig(
    val x: Int = 500,
    val y: Int = 500,
    val intervalMs: Int = 200,
    val repeatCount: Int = 0,
    val randomizeJitter: Boolean = true,
    val screenWidth: Int = 0,
    val screenHeight: Int = 0,
) {
    companion object {
        const val MIN_INTERVAL_MS = 10L
        const val MAX_INTERVAL_MS = 60_000L
        const val DEFAULT = 0  // infinite
    }

    /**
     * Validate fields and return a sanitized copy. Throws nothing; bad values are clamped.
     */
    fun sanitized(): ClickConfig = copy(
        intervalMs = intervalMs.coerceIn(MIN_INTERVAL_MS.toInt(), MAX_INTERVAL_MS.toInt()),
        repeatCount = repeatCount.coerceAtLeast(0),
        x = x.coerceAtLeast(0),
        y = y.coerceAtLeast(0),
    )

    /**
     * Returns true if the configuration is structurally valid for execution.
     */
    fun isValid(): Boolean =
        x >= 0 && y >= 0 &&
            intervalMs in MIN_INTERVAL_MS.toInt()..MAX_INTERVAL_MS.toInt() &&
            repeatCount >= 0
}
