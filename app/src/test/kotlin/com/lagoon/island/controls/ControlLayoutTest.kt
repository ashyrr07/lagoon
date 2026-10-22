package com.lagoon.island.controls

import com.lagoon.island.data.ControlSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class ControlLayoutTest {
    private val available = listOf("torch", "dnd", "rotation", "camera", "wifi", "bluetooth", "screenshot")

    @Test
    fun `resolve keeps the saved order and drops unknown and duplicate ids`() {
        val saved = listOf("wifi", "ghost", "torch", "wifi", "dnd")
        assertEquals(listOf("wifi", "torch", "dnd"), ControlLayout.resolve(saved, available))
    }

    @Test
    fun `resolve never appends ids the user did not choose`() {
        assertEquals(listOf("torch"), ControlLayout.resolve(listOf("torch"), available))
        assertEquals(emptyList<String>(), ControlLayout.resolve(emptyList(), available))
    }

    @Test
    fun `default settings resolve against the registry ids unchanged`() {
        val defaults = ControlSettings()
        assertEquals(defaults.tiles, ControlLayout.resolve(defaults.tiles, ControlsRegistry.TILE_IDS))
        assertEquals(defaults.sliders, ControlLayout.resolve(defaults.sliders, ControlsRegistry.SLIDER_IDS))
        assertEquals(defaults.widgets, ControlLayout.resolve(defaults.widgets, listOf("now_playing", "next_alarm", "battery", "timer", "calendar")))
    }

    @Test
    fun `remaining lists unchosen ids in registry order`() {
        val saved = listOf("wifi", "torch")
        assertEquals(listOf("dnd", "rotation", "camera", "bluetooth", "screenshot"), ControlLayout.remaining(saved, available))
    }

    @Test
    fun `move shifts an item and ignores bad indices`() {
        val list = listOf("a", "b", "c", "d")
        assertEquals(listOf("b", "a", "c", "d"), ControlLayout.move(list, 0, 1))
        assertEquals(listOf("a", "b", "d", "c"), ControlLayout.move(list, 3, 2))
        assertEquals(listOf("d", "a", "b", "c"), ControlLayout.move(list, 3, 0))
        assertSame(list, ControlLayout.move(list, 0, 4))
        assertSame(list, ControlLayout.move(list, -1, 0))
        assertSame(list, ControlLayout.move(list, 2, 2))
    }

    @Test
    fun `toggle adds when absent and removes when present`() {
        assertEquals(listOf("a", "b"), ControlLayout.toggle(listOf("a"), "b"))
        assertEquals(listOf("a"), ControlLayout.toggle(listOf("a", "b"), "b"))
    }

    @Test
    fun `remove drops every occurrence`() {
        assertEquals(listOf("a"), ControlLayout.remove(listOf("a", "b", "b"), "b"))
    }

    @Test
    fun `rows chunk into the grid width`() {
        assertEquals(listOf(listOf(1, 2, 3), listOf(4)), ControlLayout.rows(listOf(1, 2, 3, 4), 3))
        assertEquals(emptyList<List<Int>>(), ControlLayout.rows(listOf(1), 0))
        assertEquals(emptyList<List<Int>>(), ControlLayout.rows(emptyList<Int>(), 3))
    }
}
