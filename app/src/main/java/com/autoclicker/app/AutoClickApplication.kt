package com.autoclicker.app

import android.app.Application

/**
 * Application class. Currently a placeholder; reserved for global initialization
 * (e.g. crash reporting, analytics opt-in) once such features are added.
 */
class AutoClickApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // No-op for MVP.
    }
}
