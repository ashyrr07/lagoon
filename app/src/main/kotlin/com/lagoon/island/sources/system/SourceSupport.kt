package com.lagoon.island.sources.system

import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Icon
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.lagoon.island.data.FeatureSettings
import com.lagoon.island.data.SettingsRepository
import com.lagoon.island.engine.IconSource
import com.lagoon.island.engine.IslandAction
import com.lagoon.island.engine.IslandActivity
import com.lagoon.island.util.IntentLauncher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Keeps the latest [FeatureSettings] at hand for a source while its scope lives. */
internal class FeatureWatcher(private val repository: SettingsRepository) {
    @Volatile
    var current: FeatureSettings = FeatureSettings()
        private set

    /** Collects the settings; [onChange] runs for the first value and every change after it. */
    fun watch(scope: CoroutineScope, onChange: (FeatureSettings) -> Unit = {}): Job = scope.launch {
        repository.settings.map { it.features }.distinctUntilChanged().collect {
            current = it
            onChange(it)
        }
    }
}

/**
 * Same value as [Notification.EXTRA_CALL_TYPE], which only became public API in 34 although
 * CallStyle has written it since 31 (our minSdk).
 */
internal const val EXTRA_CALL_TYPE = "android.callType"

/** How long a system alert holds the island, in millis. */
internal fun FeatureSettings.alertMs(): Long = alertSeconds.coerceIn(1, 30) * 1000L

/**
 * Runtime-registered receivers for one source. Every action we listen to is a protected system
 * broadcast (only the platform or its privileged components can send it), so exporting the receiver
 * is safe and guarantees delivery from the Bluetooth stack, which runs under its own uid.
 */
internal class Receivers(private val context: Context) {
    private val registered = mutableListOf<BroadcastReceiver>()

    /** Registers and returns the sticky intent, if the filter matches a sticky broadcast. */
    fun register(filter: IntentFilter, onReceive: (Intent) -> Unit): Intent? {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                runCatching { onReceive(intent) }.onFailure { Log.w(TAG, "Receiver failed for ${intent.action}", it) }
            }
        }
        val sticky = runCatching {
            ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
        }.onFailure { Log.w(TAG, "registerReceiver failed", it) }.getOrNull()
        registered += receiver
        return sticky
    }

    fun register(vararg actions: String, onReceive: (Intent) -> Unit): Intent? =
        register(IntentFilter().apply { actions.forEach(::addAction) }, onReceive)

    fun unregisterAll() {
        registered.forEach { runCatching { context.unregisterReceiver(it) } }
        registered.clear()
    }

    private companion object {
        const val TAG = "Receivers"
    }
}

/**
 * Re-posts [activity] with new content without re-triggering its alert: the alert deadline is kept,
 * and the expiry is pushed to at least [minTtlMs] from [now] so the fresh content is actually seen.
 */
internal fun IslandActivity.refreshed(now: Long, minTtlMs: Long): IslandActivity {
    val alertUntil = alertUntil()
    val expiresAt = expiresAt()
    return copy(
        updatedAt = now,
        alertMs = alertUntil?.let { (it - now).coerceAtLeast(0L) },
        ttlMs = expiresAt?.let { maxOf(it, now + minTtlMs) - now },
    )
}

internal fun Icon?.toBitmapOrNull(context: Context): Bitmap? = runCatching {
    val drawable = this?.loadDrawable(context) ?: return null
    (drawable as? BitmapDrawable)?.bitmap ?: drawable.toBitmap()
}.getOrNull()

internal fun Notification.string(key: String): String? = extras?.getCharSequence(key)?.toString()?.takeIf { it.isNotBlank() }

internal val Notification.title: String? get() = string(Notification.EXTRA_TITLE)
internal val Notification.text: String? get() = string(Notification.EXTRA_TEXT)
internal val Notification.subText: String? get() = string(Notification.EXTRA_SUB_TEXT)
internal val Notification.bigText: String? get() = string(Notification.EXTRA_BIG_TEXT)
internal val Notification.showsChronometer: Boolean get() = extras?.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER) == true
internal val Notification.chronometerCountsDown: Boolean get() = extras?.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN) == true
internal val Notification.template: String? get() = extras?.getString(Notification.EXTRA_TEMPLATE)
internal val Notification.isOngoing: Boolean get() = flags and Notification.FLAG_ONGOING_EVENT != 0

internal fun Notification.largeIconSource(): IconSource? = getLargeIcon()?.let { IconSource.Platform(it) }
internal fun Notification.smallIconSource(): IconSource? = smallIcon?.let { IconSource.Platform(it) }

/** The notification's own buttons as island actions; reply-style actions are skipped. */
internal fun StatusBarNotification.islandActions(
    launcher: IntentLauncher,
    relabel: (String) -> String = { it },
): List<IslandAction> = notification.actions.orEmpty().mapIndexedNotNull { index, action ->
    val intent = action.actionIntent ?: return@mapIndexedNotNull null
    if (action.remoteInputs?.isNotEmpty() == true) return@mapIndexedNotNull null
    val label = action.title?.toString()?.trim().orEmpty()
    IslandAction(
        id = "$key:action:$index",
        label = relabel(label),
        icon = action.getIcon()?.let { IconSource.Platform(it) },
        perform = { launcher.send(intent) },
    )
}

/** A Clock app's buttons as island actions, with known ones renamed in Lagoon's language. */
internal fun StatusBarNotification.clockActions(launcher: IntentLauncher, context: Context): List<IslandAction> =
    islandActions(launcher) { label -> TimerActionLabels.shorten(label) { context.getString(it.labelRes) } }

internal fun StatusBarNotification.openAction(launcher: IntentLauncher): (() -> Unit)? =
    notification.contentIntent?.let { intent -> { launcher.send(intent) } }
