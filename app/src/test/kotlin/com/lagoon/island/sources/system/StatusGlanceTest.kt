package com.lagoon.island.sources.system

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StatusGlanceTest {
    @Test
    fun `money amount wins over a count`() {
        assertEquals("−\$21.37", StatusGlance.pick("15 open · uPnL −\$21.37"))
    }

    @Test
    fun `signed percentage and plain positive amount`() {
        assertEquals("+4.2%", StatusGlance.pick("BTC · +4.2%"))
        assertEquals("€12.00", StatusGlance.pick("Balance €12.00 · 3 trades"))
    }

    @Test
    fun `no number falls back to the shortest dot-separated part`() {
        assertEquals("Running", StatusGlance.pick("Sync service · Running"))
    }

    @Test
    fun `short title is used as is and blank gives null`() {
        assertEquals("Online", StatusGlance.pick("Online"))
        assertNull(StatusGlance.pick("   "))
    }

    @Test
    fun `long single phrase is cut to a glance`() {
        val g = StatusGlance.pick("Backing up your photos to the cloud right now")!!
        assertEquals(true, g.length <= StatusGlance.MAX_CHARS + 1)
        assertEquals(true, g.endsWith("…"))
    }

    @Test
    fun `sign tells the tone`() {
        assertEquals(StatusGlance.Tone.Negative, StatusGlance.tone("−\$21.37"))
        assertEquals(StatusGlance.Tone.Negative, StatusGlance.tone("-3%"))
        assertEquals(StatusGlance.Tone.Positive, StatusGlance.tone("+\$5"))
        assertEquals(StatusGlance.Tone.Neutral, StatusGlance.tone("15 open"))
    }
}
