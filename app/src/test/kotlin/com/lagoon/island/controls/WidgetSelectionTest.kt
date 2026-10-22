package com.lagoon.island.controls

import com.lagoon.island.engine.ActivityContent
import com.lagoon.island.engine.IslandActivity
import com.lagoon.island.engine.Priority
import com.lagoon.island.engine.Source
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WidgetSelectionTest {
    private fun media(id: String, playing: Boolean, updatedAt: Long) = IslandActivity(
        id = "media:$id",
        source = Source.Media,
        priority = Priority.Media,
        content = ActivityContent.Media(
            title = id, subtitle = "", appName = id, packageName = id, art = null,
            isPlaying = playing, positionMs = 0, durationMs = 0, positionSampledAt = updatedAt,
        ),
        startedAt = updatedAt,
        updatedAt = updatedAt,
    )

    private fun bt(id: String, connected: Boolean, battery: Int?, left: Int? = null) = IslandActivity(
        id = "bt:$id",
        source = Source.Bluetooth,
        priority = Priority.Ambient,
        content = ActivityContent.Bluetooth(
            deviceName = id, connected = connected, battery = battery, left = left, right = null, case = null,
            kind = ActivityContent.Bluetooth.Kind.Earbuds,
        ),
        startedAt = 0,
    )

    private fun generic(id: String) = IslandActivity(
        id = id,
        source = Source.Notification,
        priority = Priority.Notification,
        content = ActivityContent.Generic(null, id, null),
        startedAt = 0,
    )

    @Test
    fun `now playing prefers a playing session over a newer paused one`() {
        val playing = media("spotify", playing = true, updatedAt = 10)
        val pausedNewer = media("yt", playing = false, updatedAt = 20)
        assertEquals(playing, WidgetSelection.selectNowPlaying(listOf(pausedNewer, playing, generic("n:1"))))
    }

    @Test
    fun `now playing falls back to the most recently updated session`() {
        val older = media("a", playing = false, updatedAt = 10)
        val newer = media("b", playing = false, updatedAt = 30)
        assertEquals(newer, WidgetSelection.selectNowPlaying(listOf(older, newer)))
    }

    @Test
    fun `now playing ignores non-media activities and empty sets`() {
        assertNull(WidgetSelection.selectNowPlaying(listOf(generic("n:1"), bt("buds", true, 50))))
        assertNull(WidgetSelection.selectNowPlaying(emptyList()))
    }

    @Test
    fun `bluetooth battery needs a connected device with a level`() {
        assertNull(WidgetSelection.selectBluetooth(listOf(bt("buds", connected = false, battery = 50))))
        assertNull(WidgetSelection.selectBluetooth(listOf(bt("buds", connected = true, battery = null))))
        val perSide = bt("pods", connected = true, battery = null, left = 40)
        assertEquals(perSide.content, WidgetSelection.selectBluetooth(listOf(bt("x", true, null), perSide)))
    }
}
