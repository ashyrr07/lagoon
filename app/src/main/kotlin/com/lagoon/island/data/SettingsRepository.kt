package com.lagoon.island.data

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

val SettingsJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    prettyPrint = true
}

object IslandSettingsSerializer : Serializer<IslandSettings> {
    override val defaultValue: IslandSettings = IslandSettings()

    override suspend fun readFrom(input: InputStream): IslandSettings = try {
        SettingsJson.decodeFromString(IslandSettings.serializer(), input.readBytes().decodeToString())
    } catch (e: SerializationException) {
        throw CorruptionException("Cannot read settings", e)
    }

    override suspend fun writeTo(t: IslandSettings, output: OutputStream) {
        output.write(SettingsJson.encodeToString(IslandSettings.serializer(), t).encodeToByteArray())
    }
}

/**
 * Schema 1 -> 2 (2026-10-23, real-device feedback): the idle pill gets the tap band by default, and
 * the keep-alive foreground service (permanent "Lagoon is active" notification) is switched off.
 */
object SettingsMigration : DataMigration<IslandSettings> {
    override suspend fun shouldMigrate(currentData: IslandSettings) = currentData.schemaVersion < CURRENT_SCHEMA

    override suspend fun migrate(currentData: IslandSettings): IslandSettings = currentData.copy(
        schemaVersion = CURRENT_SCHEMA,
        calibration = currentData.calibration.copy(touchBand = true),
        keepAlive = currentData.keepAlive.copy(foregroundService = false),
    )

    override suspend fun cleanUp() = Unit
}

@Singleton
class SettingsRepository @Inject constructor(@ApplicationContext context: Context) {
    private val store: DataStore<IslandSettings> = DataStoreFactory.create(
        serializer = IslandSettingsSerializer,
        corruptionHandler = ReplaceFileCorruptionHandler { IslandSettings() },
        migrations = listOf(SettingsMigration),
        produceFile = { context.dataStoreFile("island_settings.json") },
    )

    val settings: Flow<IslandSettings> = store.data

    suspend fun update(transform: (IslandSettings) -> IslandSettings): IslandSettings =
        store.updateData(transform)

    /** Export as the same JSON the store keeps on disk. */
    suspend fun exportJson(current: IslandSettings): String =
        SettingsJson.encodeToString(IslandSettings.serializer(), current)

    suspend fun importJson(json: String): IslandSettings =
        store.updateData { SettingsJson.decodeFromString(IslandSettings.serializer(), json) }
}
