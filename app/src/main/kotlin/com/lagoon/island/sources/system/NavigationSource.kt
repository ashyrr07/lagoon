package com.lagoon.island.sources.system

import android.app.Notification
import android.service.notification.StatusBarNotification
import com.lagoon.island.data.SettingsRepository
import com.lagoon.island.engine.ActivityContent
import com.lagoon.island.engine.ActivityRegistry
import com.lagoon.island.engine.IslandActivity
import com.lagoon.island.engine.Priority
import com.lagoon.island.engine.Source
import com.lagoon.island.sources.IslandSource
import com.lagoon.island.sources.NotificationBus
import com.lagoon.island.util.IntentLauncher
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Turn-by-turn navigation from Google Maps and Waze (and anything that sets CATEGORY_NAVIGATION):
 * next instruction, distance to it and ETA from the notification text, the turn arrow from its
 * large icon.
 */
@Singleton
class NavigationSource @Inject constructor(
    private val registry: ActivityRegistry,
    private val bus: NotificationBus,
    private val launcher: IntentLauncher,
    settingsRepository: SettingsRepository,
) : IslandSource {
    private val features = FeatureWatcher(settingsRepository)
    private val keys = HashMap<String, String>()

    override fun start(scope: CoroutineScope) {
        // Replays the shade on start and whenever a switch flips, so cards appear and vanish with it.
        features.watch(scope) { bus.activeNotifications().forEach(::onPosted) }
        scope.launch {
            bus.events.collect { event ->
                when (event) {
                    is NotificationBus.Event.Posted -> onPosted(event.sbn)
                    is NotificationBus.Event.Removed -> onRemoved(event.sbn)
                }
            }
        }
    }

    override fun stop() {
        keys.values.toSet().forEach { registry.remove(it) }
        keys.clear()
    }

    private fun onPosted(sbn: StatusBarNotification) {
        if (!features.current.navigation || !isNavigation(sbn)) {
            onRemoved(sbn)
            return
        }
        val n = sbn.notification
        val info = NavigationTextParser.parse(n.title, n.text, n.subText) ?: return
        val id = idFor(sbn.packageName)
        val now = System.currentTimeMillis()
        val existing = registry[id]
        keys[sbn.key] = id
        registry.post(
            IslandActivity(
                id = id,
                source = Source.Navigation,
                priority = Priority.Navigation,
                content = ActivityContent.Navigation(
                    instruction = info.instruction,
                    distance = info.distance,
                    eta = info.eta,
                    icon = n.largeIconSource() ?: n.smallIconSource(),
                ),
                startedAt = existing?.startedAt ?: now,
                updatedAt = now,
                actions = sbn.islandActions(launcher),
                onTap = sbn.openAction(launcher),
                dismissible = false,
            ),
        )
    }

    private fun onRemoved(sbn: StatusBarNotification) {
        val id = keys.remove(sbn.key) ?: return
        if (id !in keys.values) registry.remove(id)
    }

    companion object {
        val NAV_PACKAGES = setOf("com.google.android.apps.maps", "com.waze", "com.google.android.apps.mapslite")

        fun idFor(packageName: String) = "nav:$packageName"

        fun isNavigation(sbn: StatusBarNotification): Boolean {
            val n = sbn.notification
            if (!n.isOngoing) return false
            if (n.category == Notification.CATEGORY_NAVIGATION) return true
            if (sbn.packageName !in NAV_PACKAGES) return false
            // Maps also keeps ongoing notifications for offline downloads and location sharing.
            if (n.extras?.containsKey(Notification.EXTRA_PROGRESS_MAX) == true && n.extras.getInt(Notification.EXTRA_PROGRESS_MAX) > 0) return false
            val text = listOfNotNull(n.title, n.text, n.subText)
            return text.any { NavigationTextParser.isOnlyDistance(it) || NavigationTextParser.isEtaLine(it) } ||
                n.getLargeIcon() != null
        }
    }
}
