package com.lagoon.island.sources.system

import android.app.AlarmManager
import android.app.Notification
import android.content.Context
import android.service.notification.StatusBarNotification
import com.lagoon.island.R
import com.lagoon.island.data.SettingsRepository
import com.lagoon.island.engine.ActivityContent
import com.lagoon.island.engine.ActivityRegistry
import com.lagoon.island.engine.IconSource
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The next scheduled alarm (for the control-deck widget) and the alarm that is ringing right now,
 * mirrored from the Clock app's full-screen notification with its snooze / dismiss buttons.
 */
@Singleton
class AlarmSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val registry: ActivityRegistry,
    private val bus: NotificationBus,
    private val launcher: IntentLauncher,
    settingsRepository: SettingsRepository,
) : IslandSource {
    private val features = FeatureWatcher(settingsRepository)
    private val receivers = Receivers(context)

    private val _nextAlarmAt = MutableStateFlow(readNextAlarm())
    /** Wall-clock millis of the next alarm clock, or null when none is set. */
    val nextAlarmAt: StateFlow<Long?> = _nextAlarmAt.asStateFlow()

    private var ringingKey: String? = null

    override fun start(scope: CoroutineScope) {
        // Replays the shade on start and whenever a switch flips, so cards appear and vanish with it.
        features.watch(scope) { bus.activeNotifications().forEach(::onPosted) }
        _nextAlarmAt.value = readNextAlarm()
        receivers.register(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED) { _nextAlarmAt.value = readNextAlarm() }
        scope.launch {
            bus.events.collect { event ->
                when (event) {
                    is NotificationBus.Event.Posted -> onPosted(event.sbn)
                    is NotificationBus.Event.Removed -> if (event.sbn.key == ringingKey) clearRinging()
                }
            }
        }
    }

    override fun stop() {
        receivers.unregisterAll()
        clearRinging()
    }

    private fun onPosted(sbn: StatusBarNotification) {
        if (!features.current.alarms) return
        if (!isRingingAlarm(sbn)) return
        val n = sbn.notification
        val now = System.currentTimeMillis()
        ringingKey = sbn.key
        registry.post(
            IslandActivity(
                id = ID_RINGING,
                source = Source.Alarm,
                priority = Priority.Alert,
                content = ActivityContent.Generic(
                    icon = n.largeIconSource() ?: IconSource.Res(R.drawable.ic_sys_alarm),
                    title = n.title ?: "Alarm",
                    text = n.text,
                ),
                startedAt = now,
                alertMs = RINGING_ALERT_MS,
                actions = sbn.islandActions(launcher) { TimerActionLabels.shorten(it) },
                onTap = sbn.openAction(launcher),
                dismissible = false,
            ),
        )
    }

    private fun clearRinging() {
        ringingKey = null
        registry.remove(ID_RINGING)
    }

    private fun readNextAlarm(): Long? = runCatching {
        context.getSystemService(AlarmManager::class.java)?.nextAlarmClock?.triggerTime
    }.getOrNull()

    companion object {
        const val ID_RINGING = "alarm"
        private const val RINGING_ALERT_MS = 10 * 60_000L

        val CLOCK_PACKAGES = setOf(
            "com.google.android.deskclock",
            "com.android.deskclock",
            "com.sec.android.app.clockpackage",
            "com.android.alarmclock",
            "com.oneplus.deskclock",
            "com.coloros.alarmclock",
            "com.oppo.alarmclock",
            "com.vivo.alarmclock",
            "com.miui.clock",
            "com.android.BBKClock",
            "com.huawei.deskclock",
            "com.asus.deskclock",
            "com.motorola.timeweatherwidget",
            "com.sonyericsson.alarm",
            "com.lge.clock",
            "com.urbandroid.sleep",
            "com.alarmclock.xtreme.free",
            "com.simplemobiletools.clock",
            "org.fossify.clock",
        )

        /**
         * A ringing alarm is CATEGORY_ALARM with a full-screen intent (the platform requires one for an
         * alarm to show over the lock screen) and no chronometer (expired timers count up instead).
         */
        fun isRingingAlarm(sbn: StatusBarNotification): Boolean {
            val n = sbn.notification
            if (n.category != Notification.CATEGORY_ALARM) return false
            if (n.showsChronometer) return false
            val hasSnooze = n.actions.orEmpty().any { it.title?.toString()?.contains("snooze", ignoreCase = true) == true }
            return n.fullScreenIntent != null || hasSnooze
        }
    }
}
