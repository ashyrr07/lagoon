package com.lagoon.island.service

import android.content.ComponentName
import android.content.Context
import android.service.notification.NotificationListenerService
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * The system binds this service while Notification access is granted, so it is the natural home
 * for the island: no foreground service, no permanent notification, rebound after process death.
 */
@AndroidEntryPoint
class IslandNotificationListener : NotificationListenerService() {

    @Inject lateinit var controller: IslandController

    override fun onListenerConnected() {
        controller.onListenerConnected(this)
    }

    override fun onListenerDisconnected() {
        controller.onListenerDisconnected()
    }

    companion object {
        fun componentName(context: Context) = ComponentName(context, IslandNotificationListener::class.java)

        fun isEnabled(context: Context): Boolean =
            NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

        fun requestRebind(context: Context) {
            if (isEnabled(context)) requestRebind(componentName(context))
        }
    }
}
