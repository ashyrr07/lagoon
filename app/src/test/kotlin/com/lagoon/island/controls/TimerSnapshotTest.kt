package com.lagoon.island.controls

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerSnapshotTest {
    private val fiveMin = 5 * 60_000L
    private val t0 = 1_000_000L

    @Test
    fun `idle timer reports its full length`() {
        val s = TimerSnapshot(durationMs = fiveMin)
        assertTrue(s.idle)
        assertEquals(fiveMin, s.remainingMs(t0))
        assertEquals(0f, s.progress(t0))
        assertFalse(s.isDone(t0))
    }

    @Test
    fun `start counts down from now`() {
        val s = TimerSnapshot(durationMs = fiveMin).start(t0)
        assertTrue(s.running)
        assertEquals(t0 + fiveMin, s.endAt)
        assertEquals(t0, s.baseAt)
        assertEquals(fiveMin - 60_000L, s.remainingMs(t0 + 60_000L))
        assertEquals(0.2f, s.progress(t0 + 60_000L), 0.0001f)
        assertEquals(0L, s.remainingMs(t0 + fiveMin + 10))
        assertTrue(s.isDone(t0 + fiveMin))
        assertFalse(s.isDone(t0 + fiveMin - 1))
    }

    @Test
    fun `pause freezes the remaining time and resume continues from it`() {
        val running = TimerSnapshot(durationMs = fiveMin).start(t0)
        val paused = running.pause(t0 + 90_000L)
        assertTrue(paused.paused)
        assertFalse(paused.running)
        assertEquals(fiveMin - 90_000L, paused.remainingMs(t0 + 500_000L))
        assertNull(paused.endAt)

        val resumed = paused.start(t0 + 200_000L)
        assertTrue(resumed.running)
        assertEquals(t0 + 200_000L + fiveMin - 90_000L, resumed.endAt)
        assertNull(resumed.pausedRemainingMs)
    }

    @Test
    fun `toggle alternates between running and paused`() {
        val s = TimerSnapshot(durationMs = fiveMin)
        val a = s.toggle(t0)
        assertTrue(a.running)
        val b = a.toggle(t0 + 1_000L)
        assertTrue(b.paused)
        val c = b.toggle(t0 + 2_000L)
        assertTrue(c.running)
        assertEquals(fiveMin - 1_000L, c.remainingMs(t0 + 2_000L))
    }

    @Test
    fun `reset returns to idle with the same length`() {
        val s = TimerSnapshot(durationMs = fiveMin).start(t0).pause(t0 + 10).reset()
        assertTrue(s.idle)
        assertEquals(fiveMin, s.durationMs)
        assertEquals(fiveMin, s.remainingMs(t0 + 999_999L))
    }

    @Test
    fun `start is a no-op while running or with zero length`() {
        val running = TimerSnapshot(durationMs = fiveMin).start(t0)
        assertSame(running, running.start(t0 + 5))
        val zero = TimerSnapshot(durationMs = 0L)
        assertSame(zero, zero.start(t0))
    }

    @Test
    fun `pause is a no-op when not running`() {
        val idle = TimerSnapshot(durationMs = fiveMin)
        assertSame(idle, idle.pause(t0))
    }

    @Test
    fun `withDuration stops any run and clamps negatives`() {
        val s = TimerSnapshot(durationMs = fiveMin).start(t0).withDuration(-5)
        assertTrue(s.idle)
        assertEquals(0L, s.durationMs)
        assertEquals(60_000L, TimerSnapshot().withDuration(60_000L).durationMs)
    }

    @Test
    fun `format rounds up to the next whole second`() {
        assertEquals("5:00", TimerSnapshot.format(fiveMin))
        assertEquals("1:00", TimerSnapshot.format(59_200L))
        assertEquals("0:59", TimerSnapshot.format(59_000L))
        assertEquals("0:00", TimerSnapshot.format(0L))
        assertEquals("0:00", TimerSnapshot.format(-10L))
        assertEquals("1:05:00", TimerSnapshot.format(65 * 60_000L))
        assertEquals("25:00", TimerSnapshot.format(25 * 60_000L))
    }

    @Test
    fun `presets are the documented four`() {
        assertEquals(listOf(1, 5, 10, 25), TimerSnapshot.PRESETS_MIN)
    }
}
