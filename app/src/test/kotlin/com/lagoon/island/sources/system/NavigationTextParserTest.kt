package com.lagoon.island.sources.system

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationTextParserTest {
    @Test
    fun `google maps distance title, instruction text, eta sub-text`() {
        val info = NavigationTextParser.parse("350 m", "Turn left onto Main St", "12 min · 4.5 km · 14:05")!!
        assertEquals("Turn left onto Main St", info.instruction)
        assertEquals("350 m", info.distance)
        assertEquals("14:05 · 12 min · 4.5 km", info.eta)
    }

    @Test
    fun `instruction carrying its own leading distance`() {
        val info = NavigationTextParser.parse("In 200 m, turn right onto Elm St", "15 min · 10:42 ETA", null)!!
        assertEquals("Turn right onto Elm St", info.instruction)
        assertEquals("200 m", info.distance)
        assertEquals("15 min · 10:42 ETA", info.eta)
    }

    @Test
    fun `waze style trailing distance and arrival line`() {
        val info = NavigationTextParser.parse("Exit right in 1.2 km", "Arrive 14:05 · 12 min", null)!!
        assertEquals("Exit right", info.instruction)
        assertEquals("1.2 km", info.distance)
        assertEquals("Arrive 14:05 · 12 min", info.eta)
    }

    @Test
    fun `single field with separator`() {
        val info = NavigationTextParser.parse("Turn left · 500 ft", null, null)!!
        assertEquals("Turn left", info.instruction)
        assertEquals("500 ft", info.distance)
        assertNull(info.eta)
    }

    @Test
    fun `imperial and decimal comma units`() {
        assertTrue(NavigationTextParser.isOnlyDistance("0.3 mi"))
        assertTrue(NavigationTextParser.isOnlyDistance("1,5 km"))
        assertTrue(NavigationTextParser.isOnlyDistance("800 feet"))
        assertFalse(NavigationTextParser.isOnlyDistance("12 min"))
        assertFalse(NavigationTextParser.isOnlyDistance("Turn left"))
    }

    @Test
    fun `eta lines need a time and some structure`() {
        assertTrue(NavigationTextParser.isEtaLine("12 min · 4.5 km · 14:05"))
        assertTrue(NavigationTextParser.isEtaLine("ETA 2:15 PM"))
        assertTrue(NavigationTextParser.isEtaLine("25 min (12 km)"))
        assertFalse(NavigationTextParser.isEtaLine("Turn left onto Main St"))
        assertFalse(NavigationTextParser.isEtaLine("350 m"))
    }

    @Test
    fun `no text gives nothing, plain text survives as the instruction`() {
        assertNull(NavigationTextParser.parse(null, "", "  "))
        val info = NavigationTextParser.parse("Head north", null, null)!!
        assertEquals("Head north", info.instruction)
        assertEquals("", info.distance)
        assertNull(info.eta)
    }
}
