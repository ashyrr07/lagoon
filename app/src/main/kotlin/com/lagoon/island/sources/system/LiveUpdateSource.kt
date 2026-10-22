package com.lagoon.island.sources.system

import android.app.Notification
import android.content.Context
import android.os.Build
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
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** What a progress-bearing notification says, independent of how it said it. */
data class ProgressSnapshot(
    val progress: Int,
    val max: Int,
    val indeterminate: Boolean,
    val shortCriticalText: String? = null,
) {
    /** 0..1 fraction, or null while indeterminate or when the bounds make no sense. */
    val fraction: Float?
        get() = if (indeterminate || max <= 0) null else (progress.toFloat() / max).coerceIn(0f, 1f)
}

/**
 * Android 16 Live Updates: promoted ongoing notifications and `Notification.ProgressStyle`
 * (deliveries, rides, downloads). On older releases, or when a notification merely carries a
 * classic progress bar, the same card is produced from the progress extras.
 */
@Singleton
class LiveUpdateSource @Inject constructor(
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
        if (!features.current.liveUpdates || sbn.packageName == context.packageName || !qualifies(sbn)) {
            if (tracked.remove(id)) registry.remove(id)
            return
        }
        val n = sbn.notification
        val snapshot = snapshot(n)
        val now = System.currentTimeMillis()
        val existing = registry[id]
        tracked += id
        registry.post(
            IslandActivity(
                id = id,
                source = Source.LiveUpdate,
                priority = Priority.Progress,
                content = ActivityContent.Progress(
                    title = n.title ?: sbn.packageName,
                    text = snapshot?.shortCriticalText ?: n.text ?: n.subText,
                    progress = snapshot?.fraction,
                    icon = n.largeIconSource() ?: n.smallIconSource(),
                ),
                startedAt = existing?.startedAt ?: now,
                updatedAt = now,
                tint = n.color.takeIf { it != 0 },
                actions = sbn.islandActions(launcher),
                onTap = sbn.openAction(launcher),
            ),
        )
    }

    private fun onRemoved(sbn: StatusBarNotification) {
        val id = idFor(sbn.key)
        if (tracked.remove(id)) registry.remove(id)
    }

    private fun qualifies(sbn: StatusBarNotification): Boolean {
        val n = sbn.notification
        if (sbn.packageName in EXCLUDED_PACKAGES) return false
        if (n.template?.contains("MediaStyle") == true || n.template?.contains("CallStyle") == true) return false
        if (n.category == Notification.CATEGORY_CALL || n.showsChronometer) return false
        if (isPromoted(n)) return true
        return n.isOngoing && hasProgressExtras(n)
    }

    private fun snapshot(n: Notification): ProgressSnapshot? {
        val critical = if (Build.VERSION.SDK_INT >= 36) runCatching { n.shortCriticalText }.getOrNull() else null
        styleSnapshot(n, critical)?.let { return it }
        if (!hasProgressExtras(n)) return critical?.let { ProgressSnapshot(0, 0, true, it) }
        val extras = n.extras
        return ProgressSnapshot(
            progress = extras.getInt(Notification.EXTRA_PROGRESS),
            max = extras.getInt(Notification.EXTRA_PROGRESS_MAX),
            indeterminate = extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE),
            shortCriticalText = critical,
        )
    }

    /** Reads a ProgressStyle back out of a posted notification on Android 16+; null elsewhere or on failure. */
    private fun styleSnapshot(n: Notification, critical: String?): ProgressSnapshot? {
        if (Build.VERSION.SDK_INT < 36) return null
        if (n.template?.contains("ProgressStyle") != true) return null
        val style = runCatching { Notification.Builder.recoverBuilder(context, n).style as? Notification.ProgressStyle }.getOrNull()
            ?: return null
        val segmentsTotal = style.progressSegments.sumOf { it.length }
        val max = if (segmentsTotal > 0) segmentsTotal else style.progressMax
        return ProgressSnapshot(style.progress, max, style.isProgressIndeterminate, critical)
    }

    companion object {
        private val EXCLUDED_PACKAGES = setOf("com.android.systemui")

        fun idFor(key: String) = "live:$key"

        fun hasProgressExtras(n: Notification): Boolean {
            val extras = n.extras ?: return false
            return extras.getInt(Notification.EXTRA_PROGRESS_MAX) > 0 || extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE)
        }

        fun isPromoted(n: Notification): Boolean {
            if (Build.VERSION.SDK_INT < 36) return false
            if (n.flags and Notification.FLAG_PROMOTED_ONGOING != 0) return true
            return runCatching { n.hasPromotableCharacteristics() }.getOrDefault(false)
        }
    }
}
