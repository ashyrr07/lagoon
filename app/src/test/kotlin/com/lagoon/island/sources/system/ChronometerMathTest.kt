package com.lagoon.island.sources.system

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChronometerMathTest {
    private val now = 1_700_000_000_000L

    @Test
    fun `running countdown ends at when`() {
        val t = ChronometerMath.content("Timer", showChronometer = true, countDown = true, whenMs = now + 90_000, now = now, text = null)!!
        assertEquals(now + 90_000, t.endAt)
        assertEquals(now, t.baseAt)
        assertTrue(t.running)
        assertNull(t.pausedRemainingMs)
        val endAt = t.endAt!!
        assertEquals(90_000, ChronometerMath.remainingMs(endAt, now))
        assertEquals(0, ChronometerMath.remainingMs(endAt, now + 100_000))
    }

    @Test
    fun `countdown keeps its original base across re-posts`() {
        val t = ChronometerMath.content("Timer", true, true, now + 30_000, now, null, previousBaseAt = now - 60_000)!!
        assertEquals(now - 60_000, t.baseAt)
    }

    @Test
    fun `running stopwatch counts up from when`() {
        val t = ChronometerMath.content("Stopwatch", showChronometer = true, countDown = false, whenMs = now - 12_000, now = now, text = null)!!
        assertNull(t.endAt)
        assertEquals(now - 12_000, t.baseAt)
        assertTrue(t.running)
    }

    @Test
    fun `future when without the count-down flag is still a countdown`() {
        val t = ChronometerMath.content("Timer", true, false, now + 50_000, now, null)!!
        assertEquals(now + 50_000, t.endAt)
    }

    @Test
    fun `paused timer parses the remaining time from text`() {
        val t = ChronometerMath.content("Timer", showChronometer = false, countDown = false, whenMs = 0, now = now, text = "Paused · 1:23")!!
        assertFalse(t.running)
        assertEquals(83_000L, t.pausedRemainingMs)
        assertNull(t.endAt)
    }

    @Test
    fun `paused without a time still yields a paused card`() {
        val t = ChronometerMath.content("Timer", false, false, 0, now, "Paused")!!
        assertFalse(t.running)
        assertNull(t.pausedRemainingMs)
    }

    @Test
    fun `nothing time-like gives no content`() {
        assertNull(ChronometerMath.content("Upcoming alarm", false, false, 0, now, "Mon 7:00 AM"))
        assertNull(ChronometerMath.content("Clock", false, false, 0, now, null))
    }

    @Test
    fun `duration parsing`() {
        assertEquals(83_000L, ChronometerMath.parseDuration("1:23"))
        assertEquals(3_723_000L, ChronometerMath.parseDuration("01:02:03"))
        assertEquals(83_000L, ChronometerMath.parseDuration("1:23 remaining"))
        assertEquals(3_723_000L, ChronometerMath.parseDuration("1h 2m 3s"))
        assertEquals(150_000L, ChronometerMath.parseDuration("2 min 30 sec"))
        assertEquals(90_000L, ChronometerMath.parseDuration("90 sec"))
        assertEquals(300_000L, ChronometerMath.parseDuration("5 minutes"))
        assertNull(ChronometerMath.parseDuration("7:00 AM"))
        assertNull(ChronometerMath.parseDuration("Tomorrow at 7:00 pm"))
        assertNull(ChronometerMath.parseDuration(""))
        assertNull(ChronometerMath.parseDuration(null))
    }

    @Test
    fun `paused words in several languages`() {
        assertTrue(ChronometerMath.isPaused("Paused"))
        assertTrue(ChronometerMath.isPaused("Timer pausiert"))
        assertTrue(ChronometerMath.isPaused("En pause"))
        assertTrue(ChronometerMath.isPaused("Таймер на паузе"))
        assertTrue(ChronometerMath.isPaused("Приостановлено"))
        assertTrue(ChronometerMath.isPaused("Temporizador en pausa"))
        assertFalse(ChronometerMath.isPaused("Running"))
        assertFalse(ChronometerMath.isPaused(null))
    }

    /** The island's own names are string resources; the kind's name stands in for them here. */
    private fun shorten(label: String) = TimerActionLabels.shorten(label) { it.name }

    @Test
    fun `action labels shorten to island size`() {
        assertEquals("+1:00", shorten("Add 1 minute"))
        assertEquals("+1:00", shorten("+1 min"))
        assertEquals("+1:00", shorten("+1:00"))
        assertEquals("Pause", shorten("Pause"))
        assertEquals("Resume", shorten("Start"))
        assertEquals("Stop", shorten("Stop timer"))
        assertEquals("Stop", shorten("Dismiss"))
        assertEquals("Snooze", shorten("Snooze"))
        assertEquals("Lap", shorten("Lap"))
        assertEquals("Reset", shorten("Reset"))
        assertEquals("Delete", shorten("Cancel"))
        assertEquals("Share", shorten("Share"))
        assertEquals("Something…", shorten("Something longer"))
        assertEquals("", shorten(""))
    }

    @Test
    fun `clock buttons in german, french, spanish and russian`() {
        assertEquals(TimerActionKind.Pause, TimerActionLabels.classify("Pausieren"))
        assertEquals(TimerActionKind.Resume, TimerActionLabels.classify("Fortsetzen"))
        assertEquals(TimerActionKind.Reset, TimerActionLabels.classify("Zurücksetzen"))
        assertEquals(TimerActionKind.Lap, TimerActionLabels.classify("Runde"))
        assertEquals(TimerActionKind.Resume, TimerActionLabels.classify("Reprendre"))
        assertEquals(TimerActionKind.Delete, TimerActionLabels.classify("Supprimer"))
        assertEquals(TimerActionKind.Snooze, TimerActionLabels.classify("Répéter"))
        assertEquals(TimerActionKind.Pause, TimerActionLabels.classify("Pausar"))
        assertEquals(TimerActionKind.Resume, TimerActionLabels.classify("Reanudar"))
        assertEquals(TimerActionKind.Reset, TimerActionLabels.classify("Reiniciar"))
        assertEquals(TimerActionKind.Snooze, TimerActionLabels.classify("Posponer"))
        assertEquals(TimerActionKind.Lap, TimerActionLabels.classify("Vuelta"))
        assertEquals(TimerActionKind.Pause, TimerActionLabels.classify("Пауза"))
        assertEquals(TimerActionKind.Resume, TimerActionLabels.classify("Продолжить"))
        assertEquals(TimerActionKind.Stop, TimerActionLabels.classify("Остановить"))
        assertEquals(TimerActionKind.Reset, TimerActionLabels.classify("Сбросить"))
        assertEquals(TimerActionKind.Lap, TimerActionLabels.classify("Круг"))
        assertEquals(TimerActionKind.Snooze, TimerActionLabels.classify("Отложить"))
        assertEquals(TimerActionKind.Delete, TimerActionLabels.classify("Удалить"))
        assertEquals("+1:00", shorten("+1 мин"))
        assertEquals("+1:00", shorten("+1 Min."))
        assertEquals(null, TimerActionLabels.classify("Teilen"))
    }
}
