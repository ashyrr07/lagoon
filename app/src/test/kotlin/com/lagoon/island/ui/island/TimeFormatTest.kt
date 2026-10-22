package com.lagoon.island.ui.island

import com.lagoon.island.engine.ActivityContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimeFormatTest {
    @Test
    fun `clock formats minutes and hours`() {
        assertEquals("0:00", TimeFormat.clock(0))
        assertEquals("0:05", TimeFormat.clock(5_000))
        assertEquals("4:59", TimeFormat.clock(299_000))
        assertEquals("1:00:00", TimeFormat.clock(3_600_000))
        assertEquals("1:02:03", TimeFormat.clock(3_723_000))
    }

    @Test
    fun `clock rounds to the nearest second and clamps negatives`() {
        assertEquals("0:01", TimeFormat.clock(600))
        assertEquals("0:00", TimeFormat.clock(400))
        assertEquals("0:00", TimeFormat.clock(-5_000))
    }

    @Test
    fun `position floors so 59_9s reads 0_59`() {
        assertEquals("0:59", TimeFormat.position(59_900))
        assertEquals("1:00", TimeFormat.position(60_000))
    }

    @Test
    fun `countdown remaining and stopwatch elapsed`() {
        val countdown = ActivityContent.Timer("t", endAt = 10_000, baseAt = 0, running = true)
        assertEquals(7_000, TimeFormat.timerMillis(countdown, now = 3_000))
        assertEquals(0, TimeFormat.timerMillis(countdown, now = 12_000))
        val paused = countdown.copy(running = false, pausedRemainingMs = 4_200)
        assertEquals(4_200, TimeFormat.timerMillis(paused, now = 99_000))

        val stopwatch = ActivityContent.Timer("s", endAt = null, baseAt = 1_000, running = true)
        assertEquals(2_500, TimeFormat.timerMillis(stopwatch, now = 3_500))
        val stoppedWatch = stopwatch.copy(running = false, pausedRemainingMs = 8_000)
        assertEquals(8_000, TimeFormat.timerMillis(stoppedWatch, now = 50_000))
    }

    @Test
    fun `media position extrapolates while playing and freezes when paused`() {
        val media = ActivityContent.Media(
            title = "", subtitle = "", appName = "", packageName = "", art = null,
            isPlaying = true, positionMs = 10_000, durationMs = 60_000, positionSampledAt = 1_000,
        )
        assertEquals(14_000, TimeFormat.mediaPosition(media, now = 5_000))
        assertEquals(60_000, TimeFormat.mediaPosition(media, now = 500_000))
        assertEquals(10_000, TimeFormat.mediaPosition(media.copy(isPlaying = false), now = 5_000))
        assertEquals(18_000, TimeFormat.mediaPosition(media.copy(speed = 2f), now = 5_000))
    }

    @Test
    fun `watts formatting`() {
        assertEquals("27", TimeFormat.watts(27.4f))
        assertEquals(String.format("%.1f", 7.5f), TimeFormat.watts(7.5f))
        assertNull(TimeFormat.watts(null))
    }
}
