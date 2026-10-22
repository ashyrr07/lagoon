package com.lagoon.island.ui.island

import com.lagoon.island.engine.ActivityContent

/** Clock-style formatting for timers, calls and media positions. */
object TimeFormat {
    /** "m:ss", or "h:mm:ss" from one hour. Negative values clamp to zero. */
    fun clock(ms: Long): String {
        val totalSec = (ms.coerceAtLeast(0) + 500) / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }

    /** Media position: floor to the second (so 0:59 does not read 1:00 early). */
    fun position(ms: Long): String = clock((ms.coerceAtLeast(0) / 1000) * 1000)

    /** Remaining millis of a countdown, or elapsed millis of a stopwatch, at [now]. */
    fun timerMillis(timer: ActivityContent.Timer, now: Long): Long {
        val endAt = timer.endAt
        return if (endAt != null) {
            if (!timer.running) timer.pausedRemainingMs ?: (endAt - timer.baseAt)
            else (endAt - now).coerceAtLeast(0)
        } else {
            if (!timer.running) timer.pausedRemainingMs ?: 0L
            else (now - timer.baseAt).coerceAtLeast(0)
        }
    }

    /** Extrapolated playback position at [now]. */
    fun mediaPosition(media: ActivityContent.Media, now: Long): Long {
        if (!media.isPlaying) return media.positionMs.coerceIn(0, maxOf(0, media.durationMs))
        val elapsed = (now - media.positionSampledAt).coerceAtLeast(0)
        val pos = media.positionMs + (elapsed * media.speed).toLong()
        return if (media.durationMs > 0) pos.coerceIn(0, media.durationMs) else pos.coerceAtLeast(0)
    }

    /** "27 W" style wattage, one decimal below 10 W. */
    fun watts(watts: Float?): String? = watts?.let {
        if (it >= 10f) "${it.toInt()} W" else "%.1f W".format(it)
    }
}
