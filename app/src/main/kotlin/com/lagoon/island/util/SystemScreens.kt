package com.lagoon.island.util

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import com.lagoon.island.service.IslandAccessibilityService
import com.lagoon.island.service.IslandNotificationListener

/**
 * Every jump into system Settings goes through here. Each screen is a chain: the exact page for
 * Lagoon's own toggle first, then progressively more general pages, so a phone that lacks the
 * precise page still lands somewhere useful instead of doing nothing.
 */
object SystemScreens {

    private fun packageUri(context: Context): Uri = Uri.fromParts("package", context.packageName, null)

    /** "Display over other apps", Lagoon's own switch. */
    fun overlay(context: Context) = open(
        context,
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, packageUri(context)),
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION),
    )

    /** Notification access, straight to Lagoon's switch (API 30+), else the list. */
    fun notificationAccess(context: Context) {
        val component = IslandNotificationListener.componentName(context).flattenToString()
        open(
            context,
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, component),
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).highlight(component),
        )
    }

    /** Accessibility, straight to Lagoon's service page, else the list with Lagoon highlighted. */
    fun accessibility(context: Context) {
        val component = ComponentName(context, IslandAccessibilityService::class.java).flattenToString()
        open(
            context,
            Intent(ACTION_ACCESSIBILITY_DETAILS_SETTINGS).putExtra(Intent.EXTRA_COMPONENT_NAME, component),
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).highlight(component),
        )
    }

    /**
     * Battery: the system "Stop optimising battery usage?" dialog for Lagoon, else Lagoon's own
     * battery page (Unrestricted / Optimised), else App info.
     */
    @SuppressLint("BatteryLife")
    fun battery(context: Context) = open(
        context,
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, packageUri(context)),
        Intent(ACTION_VIEW_ADVANCED_POWER_USAGE_DETAIL, packageUri(context)),
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri(context)),
    )

    /** Lagoon's App info page (where "Allow restricted settings" lives on Android 13+). */
    fun appInfo(context: Context) = open(
        context,
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri(context)),
        Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS),
    )

    /** "Modify system settings" for Lagoon (brightness, rotation tiles). */
    fun writeSettings(context: Context) = open(
        context,
        Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, packageUri(context)),
        Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS),
    )

    /** System per-app language page (Android 13+). */
    fun appLanguage(context: Context) = open(
        context,
        Intent(Settings.ACTION_APP_LOCALE_SETTINGS, packageUri(context)),
        Intent(Settings.ACTION_LOCALE_SETTINGS),
    )

    /** Starts the first intent that resolves. Returns false if none does. */
    fun open(context: Context, vararg candidates: Intent): Boolean {
        val pm = context.packageManager
        for (intent in candidates) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (intent.resolveActivity(pm) == null) continue
            val started = runCatching { context.startActivity(intent) }
                .onFailure { Log.w(TAG, "Could not open ${intent.action}", it) }
                .isSuccess
            if (started) return true
        }
        Log.w(TAG, "No settings screen for ${candidates.firstOrNull()?.action}")
        return false
    }

    /** Asks Settings to scroll to and flash the given component in a list (AOSP and One UI honour it). */
    private fun Intent.highlight(key: String): Intent = putExtra(EXTRA_FRAGMENT_ARG_KEY, key)
        .putExtra(EXTRA_SHOW_FRAGMENT_ARGS, Bundle().apply { putString(EXTRA_FRAGMENT_ARG_KEY, key) })

    private const val TAG = "SystemScreens"
    private const val ACTION_ACCESSIBILITY_DETAILS_SETTINGS = "android.settings.ACCESSIBILITY_DETAILS_SETTINGS"
    private const val ACTION_VIEW_ADVANCED_POWER_USAGE_DETAIL = "android.settings.VIEW_ADVANCED_POWER_USAGE_DETAIL"
    private const val EXTRA_FRAGMENT_ARG_KEY = ":settings:fragment_args_key"
    private const val EXTRA_SHOW_FRAGMENT_ARGS = ":settings:show_fragment_args"
}
