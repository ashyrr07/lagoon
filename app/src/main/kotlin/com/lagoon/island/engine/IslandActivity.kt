package com.lagoon.island.engine

import android.graphics.Bitmap
import android.graphics.drawable.Icon

/** Who produced the activity. */
enum class Source { Media, Call, Timer, Navigation, LiveUpdate, Notification, Charging, Bluetooth, Alarm, System, Demo }

/** Lower rank wins the island. */
enum class Priority(val rank: Int) {
    Alert(0), Call(1), Timer(2), Navigation(3), Progress(4), Media(5), Notification(6), Ambient(7),
}

/** An icon from any source. Compose resolves it lazily. */
sealed interface IconSource {
    data class Res(val resId: Int) : IconSource
    data class Platform(val icon: Icon) : IconSource
    data class Image(val bitmap: Bitmap) : IconSource
}

/** Something the user can trigger from the island. [perform] runs on the main thread. */
data class IslandAction(
    val id: String,
    val label: String,
    val icon: IconSource? = null,
    val acceptsText: Boolean = false,
    val perform: (text: String?) -> Unit,
)

sealed interface ActivityContent {
    data class Media(
        val title: String,
        val subtitle: String,
        val appName: String,
        val packageName: String,
        val art: Bitmap?,
        val isPlaying: Boolean,
        val positionMs: Long,
        val durationMs: Long,
        /** Wall-clock time at which [positionMs] was sampled, for extrapolation. */
        val positionSampledAt: Long,
        val speed: Float = 1f,
    ) : ActivityContent

    data class Call(
        val name: String,
        val detail: String?,
        val photo: Bitmap?,
        val state: State,
        val startedAt: Long,
    ) : ActivityContent {
        enum class State { Ringing, Active, Ended }
    }

    data class Timer(
        val label: String,
        /** Countdown target, or null for a stopwatch. */
        val endAt: Long?,
        /** Stopwatch origin (or countdown base) in wall-clock millis. */
        val baseAt: Long,
        val running: Boolean,
        /** Frozen remaining millis when paused. */
        val pausedRemainingMs: Long? = null,
    ) : ActivityContent

    data class Notification(
        val key: String,
        val packageName: String,
        val appName: String,
        val appIcon: IconSource?,
        val title: String,
        val text: String,
        val largeIcon: IconSource?,
        val isConversation: Boolean,
    ) : ActivityContent

    data class Progress(
        val title: String,
        val text: String?,
        /** 0..1, or null while indeterminate. */
        val progress: Float?,
        val icon: IconSource?,
    ) : ActivityContent

    data class Charging(
        val level: Int,
        val watts: Float?,
        val state: State,
    ) : ActivityContent {
        enum class State { Connected, Disconnected, Low, Full }
    }

    data class Bluetooth(
        val deviceName: String,
        val connected: Boolean,
        val battery: Int?,
        val left: Int?,
        val right: Int?,
        val case: Int?,
        val kind: Kind,
    ) : ActivityContent {
        enum class Kind { Earbuds, Headphones, Watch, Speaker, Other }
    }

    data class Navigation(
        val instruction: String,
        val distance: String,
        val eta: String?,
        val icon: IconSource?,
    ) : ActivityContent

    data class Generic(
        val icon: IconSource?,
        val title: String,
        val text: String?,
    ) : ActivityContent
}

/**
 * One thing worth showing. Sources create these; the resolver decides which ones the island shows.
 * Identity is [id]; a repost with the same id replaces the previous one.
 */
data class IslandActivity(
    val id: String,
    val source: Source,
    val priority: Priority,
    val content: ActivityContent,
    val startedAt: Long,
    val updatedAt: Long = startedAt,
    /** Remove automatically this long after [updatedAt]; null = until the source removes it. */
    val ttlMs: Long? = null,
    /** Take over the island (expanded, no interaction needed) this long after [updatedAt]. */
    val alertMs: Long? = null,
    /** ARGB accent. Null = per-source default. */
    val tint: Int? = null,
    val actions: List<IslandAction> = emptyList(),
    val onTap: (() -> Unit)? = null,
    val dismissible: Boolean = true,
) {
    fun expiresAt(): Long? = ttlMs?.let { updatedAt + it }
    fun alertUntil(): Long? = alertMs?.let { updatedAt + it }
}
