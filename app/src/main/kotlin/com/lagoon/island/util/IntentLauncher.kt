package com.lagoon.island.util

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Every activity launch from the island goes through here. The island runs in a process with no
 * Activity, so each launch is a background activity start; holding SYSTEM_ALERT_WINDOW is the
 * exemption we rely on, and the options below ask the platform to honour it.
 */
@Singleton
class IntentLauncher @Inject constructor(@ApplicationContext private val context: Context) {

    fun launch(intent: Intent): Boolean = try {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent, backgroundStartOptions().toBundle())
        true
    } catch (e: Exception) {
        Log.w(TAG, "Launch failed: $intent", e)
        false
    }

    fun send(pendingIntent: PendingIntent, fillIn: Intent? = null): Boolean = try {
        pendingIntent.send(context, 0, fillIn, null, null, null, backgroundStartOptions().toBundle())
        true
    } catch (e: Exception) {
        Log.w(TAG, "PendingIntent send failed", e)
        false
    }

    private fun backgroundStartOptions(): ActivityOptions {
        val options = ActivityOptions.makeBasic()
        if (Build.VERSION.SDK_INT >= 34) {
            @Suppress("DEPRECATION")
            options.setPendingIntentBackgroundActivityStartMode(
                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED,
            )
        }
        return options
    }

    companion object {
        private const val TAG = "IntentLauncher"
    }
}
