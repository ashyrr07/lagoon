package com.lagoon.island.service

import android.content.ComponentName
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import com.lagoon.island.sources.NotificationBus
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * The system binds this service while Notification access is granted, so it is the natural home
 * for the island: no foreground service, no permanent notification, rebound after process death.
 */
@AndroidEntryPoint
class IslandNotificationListener : NotificationListenerService() {

    @Inject lateinit var controller: IslandController
    @Inject lateinit var bus: NotificationBus

    override fun onListenerConnected() {
        bus.onConnected(this)
        controller.onListenerConnected()
    }

    override fun onListenerDisconnected() {
        bus.onDisconnected()
        controller.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        bus.emit(NotificationBus.Event.Posted(sbn))
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification, rankingMap: RankingMap?, reason: Int) {
        bus.emit(NotificationBus.Event.Removed(sbn, reason))
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
