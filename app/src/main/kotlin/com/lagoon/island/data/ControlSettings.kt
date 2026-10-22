package com.lagoon.island.data

import kotlinx.serialization.Serializable

/** Control deck layout. Owned by the controls work. Ids are defined in controls/ControlsRegistry.kt. */
@Serializable
data class ControlSettings(
    val tiles: List<String> = listOf("torch", "dnd", "rotation", "camera", "wifi", "bluetooth"),
    val sliders: List<String> = listOf("brightness", "media_volume"),
    val widgets: List<String> = listOf("now_playing", "next_alarm", "battery"),
    /** Package names shown as app shortcuts. */
    val shortcuts: List<String> = emptyList(),
)
