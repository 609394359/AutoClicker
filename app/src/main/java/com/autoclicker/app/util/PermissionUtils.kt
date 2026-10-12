package com.autoclicker.app.util

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils

/**
 * Helpers for checking and requesting the permissions the app needs at runtime.
 *
 * - Accessibility service: queried via [AccessibilityServiceInfo.SERVICE_INTERFACE].
 * - Overlay (SYSTEM_ALERT_WINDOW): checked via Settings.canDrawOverlays().
 */
object PermissionUtils {

    /** Returns true if our accessibility service is currently enabled for the user. */
    fun isAccessibilityServiceEnabled(ctx: Context): Boolean {
        val expected = ctx.packageName + "/" + AutoClickServiceClassName
        val enabled = Settings.Secure.getString(
            ctx.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabled)
        while (splitter.hasNext()) {
            val componentName = splitter.next()
            if (componentName.equals(expected, ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    /** Returns true if the app can draw overlays over other apps. */
    fun canDrawOverlays(ctx: Context): Boolean = Settings.canDrawOverlays(ctx)

    /** Build an Intent that opens the system accessibility settings page. */
    fun accessibilitySettingsIntent(): Intent =
        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /**
     * Build an Intent that opens the screen the user can use to grant the overlay permission.
     * Falls back to the main app settings page if the OEM does not expose the specific screen.
     */
    fun overlaySettingsIntent(ctx: Context): Intent =
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:${ctx.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    // We can't reference the actual service class here without creating a circular dep,
    // so use the well-known FQCN. Update if you rename the package.
    const val AutoClickServiceClassName = "com.autoclicker.app.service.AutoClickService"
}
