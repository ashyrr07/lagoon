package com.lagoon.island.sources.system

import com.lagoon.island.engine.ActivityContent

/**
 * Turns a Clock app's chronometer notification into timer content. A running countdown sets
 * EXTRA_CHRONOMETER_COUNT_DOWN with `when` = the moment it reaches zero; a running stopwatch has
 * `when` = its start. Paused timers drop the chronometer and print the remaining time as text.
 */
object ChronometerMath {
    private val PAUSED_WORDS = listOf("paused", "pausiert", "en pause", "in pausa", "pausado", "pausada", "gepauzeerd", "一時停止", "已暂停", "일시정지")
    private val CLOCK_REGEX = Regex("""(?<![\d:])(\d{1,2}):(\d{2})(?::(\d{2}))?(?![\d:]|\s?[ap]\.?m)""", RegexOption.IGNORE_CASE)
    private val UNITS_REGEX = Regex("""(?:(\d+)\s?h(?:ours?|rs?)?\b)?\s*(?:(\d+)\s?m(?:in(?:ute)?s?)?\b)?\s*(?:(\d+)\s?s(?:ec(?:ond)?s?)?\b)?""", RegexOption.IGNORE_CASE)

    /**
     * Builds the content. Returns null when nothing time-like is present. [previousBaseAt] keeps a
     * countdown's origin stable across the Clock app's periodic re-posts.
     */
    fun content(
        label: String,
        showChronometer: Boolean,
        countDown: Boolean,
        whenMs: Long,
        now: Long,
        text: String?,
        previousBaseAt: Long? = null,
    ): ActivityContent.Timer? {
        if (showChronometer && whenMs > 0) {
            return if (countDown || whenMs > now + 1_000) {
                ActivityContent.Timer(label, endAt = whenMs, baseAt = previousBaseAt ?: now, running = true)
            } else {
                ActivityContent.Timer(label, endAt = null, baseAt = whenMs, running = true)
            }
        }
        val paused = isPaused(text) || isPaused(label)
        val remaining = parseDuration(text) ?: parseDuration(label)
        if (!paused && remaining == null) return null
        return ActivityContent.Timer(
            label = label,
            endAt = null,
            baseAt = previousBaseAt ?: now,
            running = false,
            pausedRemainingMs = remaining,
        )
    }

    fun isPaused(text: String?): Boolean {
        val lower = text?.lowercase() ?: return false
        return PAUSED_WORDS.any { it in lower }
    }

    /** "1:23", "01:02:03", "1h 2m 3s", "90 sec" -> millis; null when absent or a wall-clock time. */
    fun parseDuration(text: String?): Long? {
        if (text.isNullOrBlank()) return null
        CLOCK_REGEX.find(text)?.let { m ->
            val a = m.groupValues[1].toLong()
            val b = m.groupValues[2].toLong()
            val c = m.groupValues[3].takeIf { it.isNotEmpty() }?.toLong()
            return if (c != null) ((a * 60 + b) * 60 + c) * 1000 else (a * 60 + b) * 1000
        }
        UNITS_REGEX.findAll(text).forEach { m ->
            val h = m.groupValues[1].toLongOrNull()
            val min = m.groupValues[2].toLongOrNull()
            val s = m.groupValues[3].toLongOrNull()
            if (h != null || min != null || s != null) {
                return ((h ?: 0) * 3600 + (min ?: 0) * 60 + (s ?: 0)) * 1000
            }
        }
        return null
    }

    /** Remaining millis of a running countdown at [now], never negative. */
    fun remainingMs(endAt: Long, now: Long): Long = (endAt - now).coerceAtLeast(0L)
}

/** Shortens Clock apps' button labels to what fits in the island. */
object TimerActionLabels {
    fun shorten(label: String): String {
        val lower = label.trim().lowercase()
        return when {
            lower.isEmpty() -> label
            Regex("""(add|\+)\s?1\b.*min""").containsMatchIn(lower) || lower == "+1:00" || lower == "+1 min" -> "+1:00"
            lower.startsWith("pause") -> "Pause"
            lower.startsWith("resume") || lower.startsWith("start") || lower.startsWith("continue") -> "Resume"
            lower.startsWith("stop") || lower.startsWith("dismiss") -> "Stop"
            lower.startsWith("reset") -> "Reset"
            lower.startsWith("lap") -> "Lap"
            lower.startsWith("snooze") -> "Snooze"
            lower.startsWith("delete") || lower.startsWith("cancel") -> "Delete"
            label.length > 10 -> label.trim().take(9) + "…"
            else -> label.trim()
        }
    }
}
