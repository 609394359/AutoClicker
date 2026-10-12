package com.autoclicker.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Receives the "stop" action broadcast by the foreground service notification's action button.
 *
 * Because this receiver is declared with `android:exported="false"` and an explicit package
 * filter, only our own process can deliver the action — no third-party app can trigger it.
 */
class StopReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.i(TAG, "Stop action received, stopping loop")
        AutoClickService.get()?.stopLoop()
    }

    companion object {
        private const val TAG = "StopReceiver"
    }
}
