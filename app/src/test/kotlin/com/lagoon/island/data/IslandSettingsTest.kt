package com.lagoon.island.data

import org.junit.Assert.assertEquals
import org.junit.Test

class IslandSettingsTest {
    @Test
    fun `settings survive a json round trip`() {
        val original = IslandSettings(
            islandEnabled = false,
            calibration = Calibration(offsetX = 3.5f, placement = Placement.Left, touchBand = true),
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
        // Everything else falls back to defaults; the stored schema version is kept so the
        // DataStore migration can upgrade it.
        assertEquals(IslandSettings(schemaVersion = 1), decoded)
    }

    @Test
    fun `schema 1 settings migrate to the tap band and no keep-alive service`() = kotlinx.coroutines.test.runTest {
        val old = IslandSettings(
            schemaVersion = 1,
            calibration = Calibration(touchBand = false, offsetX = 4f),
            keepAlive = KeepAliveSettings(foregroundService = true),
        )
        org.junit.Assert.assertTrue(SettingsMigration.shouldMigrate(old))
        val migrated = SettingsMigration.migrate(old)
        assertEquals(CURRENT_SCHEMA, migrated.schemaVersion)
        org.junit.Assert.assertTrue(migrated.calibration.touchBand)
        assertEquals(4f, migrated.calibration.offsetX, 0f)
        org.junit.Assert.assertFalse(migrated.keepAlive.foregroundService)
        org.junit.Assert.assertFalse(SettingsMigration.shouldMigrate(migrated))
    }
}
