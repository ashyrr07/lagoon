package com.lagoon.island.controls

import java.util.Locale

/**
 * The built-in countdown as a value. [LagoonTimer] owns one of these and persists the transitions
 * to AlarmManager and the island; this type has no Android dependencies so the maths is testable.
 *
 * Exactly one of these holds:
 *  - idle: [endAt] == null && [pausedRemainingMs] == null
 *  - running: [endAt] != null
 *  - paused: [pausedRemainingMs] != null
 */
data class TimerSnapshot(
    /** The length the user picked, in millis. */
    val durationMs: Long = 0L,
    /** Wall-clock time the countdown reaches zero, while running. */
    val endAt: Long? = null,
    /** Frozen remaining millis while paused. */
    val pausedRemainingMs: Long? = null,
    /** Wall-clock time the current run started (countdown base for the island content). */
    val baseAt: Long = 0L,
) {
    val running: Boolean get() = endAt != null
    val paused: Boolean get() = pausedRemainingMs != null
    val idle: Boolean get() = endAt == null && pausedRemainingMs == null

    fun remainingMs(now: Long): Long = when {
        endAt != null -> (endAt - now).coerceAtLeast(0L)
        pausedRemainingMs != null -> pausedRemainingMs.coerceAtLeast(0L)
        else -> durationMs
    }

    /** 0..1 fraction elapsed; 0 when idle. */
    fun progress(now: Long): Float {
        if (durationMs <= 0L || idle) return 0f
        return (1f - remainingMs(now).toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    }

    fun isDone(now: Long): Boolean = endAt != null && now >= endAt

    /** Pick a new length. Stops any run. */
    fun withDuration(ms: Long): TimerSnapshot = TimerSnapshot(durationMs = ms.coerceAtLeast(0L))

    /** Start, or resume from a pause. No-op when already running or the length is zero. */
    fun start(now: Long): TimerSnapshot {
        if (running) return this
        val remaining = pausedRemainingMs ?: durationMs
        if (remaining <= 0L) return this
        return copy(endAt = now + remaining, pausedRemainingMs = null, baseAt = now)
    }

    fun pause(now: Long): TimerSnapshot {
        if (!running) return this
        return copy(endAt = null, pausedRemainingMs = remainingMs(now))
    }

    /** Back to idle with the same length. */
    fun reset(): TimerSnapshot = copy(endAt = null, pausedRemainingMs = null, baseAt = 0L)

    /** Running and paused flip; idle starts. */
    fun toggle(now: Long): TimerSnapshot = if (running) pause(now) else start(now)

    companion object {
        val PRESETS_MIN: List<Int> = listOf(1, 5, 10, 25)
        const val DEFAULT_MS: Long = 5 * 60_000L

        /** "m:ss" under an hour, "h:mm:ss" above. Rounds up so 59.2 s shows as 1:00, never 0:59. */
        fun format(ms: Long): String {
            val totalSec = ((ms.coerceAtLeast(0L) + 999L) / 1000L)
            val h = totalSec / 3600
            val m = (totalSec % 3600) / 60
            val s = totalSec % 60
            return if (h > 0) {
                String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s)
            } else {
                String.format(Locale.ROOT, "%d:%02d", m, s)
            }
        }
    }
}
