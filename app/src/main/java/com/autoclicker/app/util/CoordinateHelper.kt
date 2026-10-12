package com.autoclicker.app.util

import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager

/**
 * Translates between "recorded" coordinates and the current device's screen coordinates.
 *
 * When a user picks a click point on one device and then runs the app on a device with
 * a different resolution, the absolute (x,y) coordinates would land in the wrong place.
 * To handle this we persist a reference screen size alongside the (x,y), and on launch we
 * scale proportionally to the actual screen size.
 */
object CoordinateHelper {

    data class ScreenSize(val width: Int, val height: Int) {
        companion object {
            val ZERO = ScreenSize(0, 0)
        }
        fun isValid(): Boolean = width > 0 && height > 0
    }

    /** Current screen size in pixels (excludes system decorations). */
    fun currentScreenSize(ctx: Context): ScreenSize {
        val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        // WindowMetrics is the API 30+ replacement for Display.getRealSize; we keep both for compatibility.
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            val metrics = wm.currentWindowMetrics
            val bounds = metrics.bounds
            ScreenSize(bounds.width(), bounds.height())
        } else {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(metrics)
            ScreenSize(metrics.widthPixels, metrics.heightPixels)
        }
    }

    /**
     * Convert (x,y) recorded on [reference] into coordinates appropriate for [actual].
     * If reference is invalid, returns (x,y) unchanged.
     */
    fun scale(
        x: Int,
        y: Int,
        reference: ScreenSize,
        actual: ScreenSize,
    ): Pair<Int, Int> {
        if (!reference.isValid() || !actual.isValid()) return x to y
        val rx = x.toFloat() / reference.width
        val ry = y.toFloat() / reference.height
        return (rx * actual.width).toInt().coerceIn(0, actual.width - 1) to
            (ry * actual.height).toInt().coerceIn(0, actual.height - 1)
    }
}
