package com.lagoon.island.data

import org.junit.Assert.assertEquals
import org.junit.Test

class IslandSettingsTest {
    @Test
    fun `settings survive a json round trip`() {
        val original = IslandSettings(
            islandEnabled = false,
            calibration = Calibration(offsetX = 3.5f, placement = Placement.Left, hugCamera = true),
            appearance = Appearance(tint = Tint.AlbumArt, scale = 1.2f),
            visibility = Visibility(hiddenInApps = setOf("com.example.game")),
        )
        val json = SettingsJson.encodeToString(IslandSettings.serializer(), original)
        val decoded = SettingsJson.decodeFromString(IslandSettings.serializer(), json)
        assertEquals(original, decoded)
    }

    @Test
    fun `unknown keys and missing fields fall back to defaults`() {
        val decoded = SettingsJson.decodeFromString(
            IslandSettings.serializer(),
            """{"schemaVersion":1,"islandEnabled":true,"futureField":42}""",
        )
        assertEquals(IslandSettings(), decoded)
    }
}
