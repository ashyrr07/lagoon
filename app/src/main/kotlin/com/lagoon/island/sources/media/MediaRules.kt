package com.lagoon.island.sources.media

/**
 * Pure helpers behind [MediaSource]. No Android types so they can be unit-tested on the JVM.
 *
 * Action ids posted on the media activity:
 * - [ACTION_PREV], [ACTION_PLAY_PAUSE], [ACTION_NEXT]: plain transport controls.
 * - [ACTION_SEEK]: `acceptsText = true`; the text is the target position in milliseconds
 *   (for example `"83500"`), see [parseSeekMillis]. The UI drives the seek bar with it.
 */
object MediaRules {
    const val ACTION_PREV = "prev"
    const val ACTION_PLAY_PAUSE = "play_pause"
    const val ACTION_NEXT = "next"
    const val ACTION_SEEK = "seek"

    /** Keep a stopped session in the island this long before removing it. Paused sessions stay. */
    const val STOPPED_GRACE_MS = 3_000L

    /** Longest edge of the album art bitmap we keep in memory. */
    const val ART_MAX_PX = 256

    fun activityId(packageName: String) = "media:$packageName"

    /**
     * The session the island follows: the system orders sessions by priority (index 0 first),
     * but a session that is actually playing beats an idle one ahead of it.
     */
    fun <T> pickPrioritySession(sessions: List<T>, isPlaying: (T) -> Boolean): T? =
        sessions.firstOrNull(isPlaying) ?: sessions.firstOrNull()

    /**
     * `PlaybackState.lastPositionUpdateTime` is on the `SystemClock.elapsedRealtime()` timeline;
     * the activity model wants wall-clock millis so the UI can extrapolate with `currentTimeMillis`.
     */
    fun elapsedToWallClock(elapsedAt: Long, nowElapsed: Long, nowWall: Long): Long =
        nowWall - (nowElapsed - elapsedAt)

    /** Parses the text of a [ACTION_SEEK] action; null when it is not a non-negative integer. */
    fun parseSeekMillis(text: String?): Long? =
        text?.trim()?.toLongOrNull()?.takeIf { it >= 0 }

    /** Whether a `PlaybackState.STATE_*` value counts as "stopped" (candidate for removal). */
    fun isStopped(state: Int?): Boolean = state == null || state in STOPPED_STATES

    fun isPlaying(state: Int?): Boolean = state != null && state in PLAYING_STATES

    // Values mirror android.media.session.PlaybackState so this file stays free of Android.
    private const val STATE_NONE = 0
    private const val STATE_STOPPED = 1
    private const val STATE_PLAYING = 3
    private const val STATE_FAST_FORWARDING = 4
    private const val STATE_REWINDING = 5
    private const val STATE_BUFFERING = 6
    private const val STATE_ERROR = 7
    private const val STATE_SKIPPING_TO_PREVIOUS = 9
    private const val STATE_SKIPPING_TO_NEXT = 10
    private const val STATE_SKIPPING_TO_QUEUE_ITEM = 11

    private val STOPPED_STATES = setOf(STATE_NONE, STATE_STOPPED, STATE_ERROR)
    private val PLAYING_STATES = setOf(
        STATE_PLAYING, STATE_FAST_FORWARDING, STATE_REWINDING, STATE_BUFFERING,
        STATE_SKIPPING_TO_PREVIOUS, STATE_SKIPPING_TO_NEXT, STATE_SKIPPING_TO_QUEUE_ITEM,
    )
}
