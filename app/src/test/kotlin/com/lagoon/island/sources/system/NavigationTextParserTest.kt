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

    @Test
    fun `russian maps text`() {
        val info = NavigationTextParser.parse("Через 200 м поверните направо на ул. Ленина", "12 мин · 4,5 км · 14:05", null)!!
        assertEquals("Поверните направо на ул. Ленина", info.instruction)
        assertEquals("200 м", info.distance)
        assertEquals("14:05 · 12 мин · 4,5 км", info.eta)
        assertTrue(NavigationTextParser.isOnlyDistance("350 м"))
        assertTrue(NavigationTextParser.isOnlyDistance("1,2 км"))
        assertFalse(NavigationTextParser.isOnlyDistance("5 мин"))
        assertTrue(NavigationTextParser.isEtaLine("Прибытие 14:05"))
    }

    @Test
    fun `german french and spanish maps text`() {
        val de = NavigationTextParser.parse("Nach 300 m links abbiegen", "1 Std. 5 Min. · 80 km · 15:10", null)!!
        assertEquals("Links abbiegen", de.instruction)
        assertEquals("300 m", de.distance)
        assertEquals("15:10 · 1 Std. 5 Min. · 80 km", de.eta)
        val fr = NavigationTextParser.parse("Dans 150 m, tournez à gauche", null, null)!!
        assertEquals("Tournez à gauche", fr.instruction)
        assertEquals("150 m", fr.distance)
        assertTrue(NavigationTextParser.isEtaLine("Llegada 18:42"))
        assertTrue(NavigationTextParser.isEtaLine("Ankunft 18:42"))
    }
}
