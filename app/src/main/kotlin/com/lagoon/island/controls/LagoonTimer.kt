package com.lagoon.island.controls

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.lagoon.island.engine.ActivityContent
import com.lagoon.island.engine.ActivityRegistry
import com.lagoon.island.engine.IslandAction
import com.lagoon.island.engine.IslandActivity
import com.lagoon.island.engine.Priority
import com.lagoon.island.engine.Source
import com.lagoon.island.ui.app.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The deck's own countdown. State lives in [snapshot]; every transition also
 *  - (re)schedules an exact alarm clock to [TimerDoneReceiver] so the chime fires with the
 *    process asleep (setAlarmClock needs no special permission), and
 *  - posts/updates the `timer:lagoon` activity so the island shows the countdown.
 *
 * All calls on the main thread.
 */
@Singleton
class LagoonTimer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val registry: ActivityRegistry,
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val notificationManager = context.getSystemService(NotificationManager::class.java)

    private val _snapshot = MutableStateFlow(TimerSnapshot(durationMs = TimerSnapshot.DEFAULT_MS))
    val snapshot: StateFlow<TimerSnapshot> = _snapshot.asStateFlow()

    fun now(): Long = System.currentTimeMillis()

    fun setDuration(ms: Long) = apply(_snapshot.value.withDuration(ms))

    fun setPresetMinutes(minutes: Int) = setDuration(minutes * 60_000L)

    fun start() = apply(_snapshot.value.start(now()))

    fun pause() = apply(_snapshot.value.pause(now()))

    fun toggle() = apply(_snapshot.value.toggle(now()))

    fun reset() = apply(_snapshot.value.reset())

    /** Called by [TimerDoneReceiver] when the alarm fires. */
    fun onFired() {
        val s = _snapshot.value
        if (!s.running) return
        postDoneNotification(s)
        registry.post(
            IslandActivity(
                id = ACTIVITY_ID,
                source = Source.Timer,
                priority = Priority.Alert,
                content = ActivityContent.Timer(
                    label = "Timer done",
                    endAt = s.endAt,
                    baseAt = s.baseAt,
                    running = false,
                    pausedRemainingMs = 0L,
                ),
                startedAt = now(),
                ttlMs = DONE_TTL_MS,
                alertMs = DONE_ALERT_MS,
                tint = TINT,
                actions = listOf(IslandAction(id = "dismiss", label = "Dismiss", perform = { dismissDone() })),
                onTap = { dismissDone() },
            ),
        )
        _snapshot.value = s.reset()
    }

    private fun dismissDone() {
        registry.remove(ACTIVITY_ID)
        notificationManager?.cancel(NOTIFICATION_ID)
    }

    private fun apply(next: TimerSnapshot) {
        val previous = _snapshot.value
        if (next == previous) return
        _snapshot.value = next
        when {
            next.running -> {
                schedule(next.endAt ?: return)
                postActivity(next)
            }
            next.paused -> {
                cancelAlarm()
                postActivity(next)
            }
            else -> {
                cancelAlarm()
                registry.remove(ACTIVITY_ID)
            }
        }
    }

    private fun postActivity(s: TimerSnapshot) {
        registry.post(
            IslandActivity(
                id = ACTIVITY_ID,
                source = Source.Timer,
                priority = Priority.Timer,
                content = ActivityContent.Timer(
                    label = "Timer",
                    endAt = s.endAt,
                    baseAt = s.baseAt,
                    running = s.running,
                    pausedRemainingMs = s.pausedRemainingMs,
                ),
                startedAt = s.baseAt.takeIf { it > 0 } ?: now(),
                updatedAt = now(),
                tint = TINT,
                actions = listOf(
                    IslandAction(id = "play_pause", label = if (s.running) "Pause" else "Resume", perform = { toggle() }),
                    IslandAction(id = "reset", label = "Reset", perform = { reset() }),
                ),
                dismissible = true,
            ),
        )
    }

    private fun schedule(endAt: Long) {
        val am = alarmManager ?: return
        val show = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        runCatching { am.setAlarmClock(AlarmManager.AlarmClockInfo(endAt, show), firePendingIntent()) }
            .onFailure { Log.w(TAG, "setAlarmClock failed", it) }
    }

    private fun cancelAlarm() {
        runCatching { alarmManager?.cancel(firePendingIntent()) }
    }

    private fun firePendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_FIRE,
        Intent(context, TimerDoneReceiver::class.java).setAction(TimerDoneReceiver.ACTION_FIRE),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun postDoneNotification(s: TimerSnapshot) {
        val nm = notificationManager ?: return
        runCatching {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Timer", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Rings when the island timer finishes"
                },
            )
            val open = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val notification = Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Timer done")
                .setContentText("${TimerSnapshot.format(s.durationMs)} is up")
                .setCategory(Notification.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(open)
                .build()
            nm.notify(NOTIFICATION_ID, notification)
        }.onFailure { Log.w(TAG, "Timer notification failed (POST_NOTIFICATIONS not granted?)", it) }
    }

    companion object {
        private const val TAG = "LagoonTimer"
        const val ACTIVITY_ID = "timer:lagoon"
        const val CHANNEL_ID = "lagoon_timer"
        const val NOTIFICATION_ID = 4101
        private const val REQUEST_FIRE = 4102
        private const val DONE_TTL_MS = 60_000L
        private const val DONE_ALERT_MS = 8_000L
        /** Orange, the timer accent from the design spec. */
        private const val TINT = 0xFFFF9F0A.toInt()
    }
}
