package com.lagoon.island.sources.system

import android.app.Notification
import android.content.Context
import android.service.notification.StatusBarNotification
import com.lagoon.island.R
import com.lagoon.island.data.SettingsRepository
import com.lagoon.island.engine.ActivityContent
import com.lagoon.island.engine.ActivityRegistry
import com.lagoon.island.engine.IslandActivity
import com.lagoon.island.engine.Priority
import com.lagoon.island.engine.Source
import com.lagoon.island.sources.IslandSource
import com.lagoon.island.sources.NotificationBus
import com.lagoon.island.util.IntentLauncher
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Timers and stopwatches from any Clock app, read off their chronometer notifications. The
 * notification's own buttons (pause, resume, stop, +1:00, lap) become island actions.
 */
@Singleton
class TimerSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val registry: ActivityRegistry,
    private val bus: NotificationBus,
    private val launcher: IntentLauncher,
    settingsRepository: SettingsRepository,
) : IslandSource {
    private val features = FeatureWatcher(settingsRepository)
    private val tracked = HashSet<String>()

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
        tracked.forEach { registry.remove(it) }
        tracked.clear()
    }

    private fun onPosted(sbn: StatusBarNotification) {
        val id = idFor(sbn.key)
        if (!features.current.timers || !isTimerNotification(sbn)) {
            if (tracked.remove(id)) registry.remove(id)
            return
        }
        val n = sbn.notification
        val now = System.currentTimeMillis()
        val existing = registry[id]
        val previousBase = (existing?.content as? ActivityContent.Timer)?.baseAt
        val label = n.title ?: context.getString(if (n.chronometerCountsDown) R.string.island_timer else R.string.island_stopwatch)
        val content = ChronometerMath.content(
            label = label,
            showChronometer = n.showsChronometer,
            countDown = n.chronometerCountsDown,
            whenMs = n.`when`,
            now = now,
            text = n.text,
            previousBaseAt = previousBase,
        ) ?: run {
            if (tracked.remove(id)) registry.remove(id)
            return
        }
        tracked += id
        registry.post(
            IslandActivity(
                id = id,
                source = Source.Timer,
                priority = Priority.Timer,
                content = content,
                startedAt = existing?.startedAt ?: now,
                updatedAt = now,
                actions = sbn.clockActions(launcher, context),
                onTap = sbn.openAction(launcher),
                dismissible = false,
            ),
        )
    }

    private fun onRemoved(sbn: StatusBarNotification) {
        val id = idFor(sbn.key)
        if (tracked.remove(id)) registry.remove(id)
    }

    companion object {
        fun idFor(key: String) = "timer:$key"

        private val EXCLUDED_PACKAGES = setOf("com.android.systemui")

        /**
         * Any chronometer notification that is not a call or SystemUI's recorder, plus a known Clock
         * app's timer that is paused (no chronometer, remaining time printed as text).
         */
        fun isTimerNotification(sbn: StatusBarNotification): Boolean {
            val n = sbn.notification
            if (sbn.packageName in EXCLUDED_PACKAGES) return false
            if (n.category == Notification.CATEGORY_CALL || n.extras?.containsKey(EXTRA_CALL_TYPE) == true) return false
            if (n.template?.contains("MediaStyle") == true) return false
            if (n.showsChronometer) return true
            if (sbn.packageName !in AlarmSource.CLOCK_PACKAGES) return false
            if (n.category == Notification.CATEGORY_ALARM && AlarmSource.isRingingAlarm(sbn)) return false
            return ChronometerMath.isPaused(n.text) || ChronometerMath.isPaused(n.title)
        }
    }
}
