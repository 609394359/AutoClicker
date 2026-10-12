package com.autoclicker.app.util

import android.content.Context
import android.content.SharedPreferences
import com.autoclicker.app.model.ClickConfig

/**
 * SharedPreferences-backed persistence for [ClickConfig].
 *
 * Single instance per process. Use [getInstance].
 */
class PrefsManager private constructor(private val prefs: SharedPreferences) {

    fun loadClickConfig(): ClickConfig = ClickConfig(
        x = prefs.getInt(KEY_X, 500),
        y = prefs.getInt(KEY_Y, 500),
        intervalMs = prefs.getInt(KEY_INTERVAL, 200),
        repeatCount = prefs.getInt(KEY_REPEAT, 0),
        randomizeJitter = prefs.getBoolean(KEY_JITTER, true),
        screenWidth = prefs.getInt(KEY_SCREEN_W, 0),
        screenHeight = prefs.getInt(KEY_SCREEN_H, 0),
    ).sanitized()

    fun saveClickConfig(cfg: ClickConfig) {
        prefs.edit().apply {
            putInt(KEY_X, cfg.x)
            putInt(KEY_Y, cfg.y)
            putInt(KEY_INTERVAL, cfg.intervalMs)
            putInt(KEY_REPEAT, cfg.repeatCount)
            putBoolean(KEY_JITTER, cfg.randomizeJitter)
            putInt(KEY_SCREEN_W, cfg.screenWidth)
            putInt(KEY_SCREEN_H, cfg.screenHeight)
            apply()
        }
    }

    fun saveLastRunsCount(count: Int) {
        prefs.edit().putInt(KEY_LAST_RUN_COUNT, count).apply()
    }

    fun lastRunCount(): Int = prefs.getInt(KEY_LAST_RUN_COUNT, 0)

    fun clear() = prefs.edit().clear().apply()

    companion object {
        private const val FILE_NAME = "autoclicker_prefs"
        private const val KEY_X = "click_x"
        private const val KEY_Y = "click_y"
        private const val KEY_INTERVAL = "interval_ms"
        private const val KEY_REPEAT = "repeat_count"
        private const val KEY_JITTER = "random_jitter"
        private const val KEY_SCREEN_W = "screen_w"
        private const val KEY_SCREEN_H = "screen_h"
        private const val KEY_LAST_RUN_COUNT = "last_run_count"

        @Volatile private var instance: PrefsManager? = null

        fun getInstance(ctx: Context): PrefsManager =
            instance ?: synchronized(this) {
                instance ?: PrefsManager(
                    ctx.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
                ).also { instance = it }
            }
    }
}
