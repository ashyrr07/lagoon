package com.lagoon.island.sources.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaRulesTest {
    private data class Session(val pkg: String, val playing: Boolean)

    private fun pick(vararg sessions: Session) = MediaRules.pickPrioritySession(sessions.toList()) { it.playing }

    @Test
    fun `index 0 wins when nothing plays`() {
        assertEquals("a", pick(Session("a", false), Session("b", false))?.pkg)
    }

    @Test
    fun `a playing session beats an idle one ahead of it`() {
        assertEquals("b", pick(Session("a", false), Session("b", true))?.pkg)
    }

    @Test
    fun `first playing session wins among several`() {
        assertEquals("b", pick(Session("a", false), Session("b", true), Session("c", true))?.pkg)
    }

    @Test
    fun `no sessions means no pick`() {
        assertNull(pick())
    }

    @Test
    fun `elapsed timestamps map onto the wall clock`() {
        // Sampled 1.5 s ago on the elapsed clock -> 1.5 s ago on the wall clock.
        assertEquals(10_000L - 1_500L, MediaRules.elapsedToWallClock(elapsedAt = 8_500L, nowElapsed = 10_000L, nowWall = 10_000L))
        assertEquals(1_000_000L - 1_500L, MediaRules.elapsedToWallClock(elapsedAt = 8_500L, nowElapsed = 10_000L, nowWall = 1_000_000L))
    }

    @Test
    fun `seek text parses to non-negative millis`() {
        assertEquals(83_500L, MediaRules.parseSeekMillis("83500"))
        assertEquals(0L, MediaRules.parseSeekMillis(" 0 "))
        assertNull(MediaRules.parseSeekMillis("-5"))
        assertNull(MediaRules.parseSeekMillis("1:23"))
        assertNull(MediaRules.parseSeekMillis(null))
    }

    @Test
    fun `playback state classification`() {
        // PlaybackState constants: NONE=0, STOPPED=1, PAUSED=2, PLAYING=3, BUFFERING=6, ERROR=7.
        assertTrue(MediaRules.isStopped(null))
        assertTrue(MediaRules.isStopped(0))
        assertTrue(MediaRules.isStopped(1))
        assertTrue(MediaRules.isStopped(7))
        assertFalse(MediaRules.isStopped(2))
        assertFalse(MediaRules.isStopped(3))
        assertTrue(MediaRules.isPlaying(3))
        assertTrue(MediaRules.isPlaying(6))
        assertFalse(MediaRules.isPlaying(2))
        assertFalse(MediaRules.isPlaying(null))
    }

    @Test
    fun `activity id is namespaced by package`() {
        assertEquals("media:com.spotify.music", MediaRules.activityId("com.spotify.music"))
    }
}
