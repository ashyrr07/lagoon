package com.lagoon.island.data

import kotlinx.serialization.Serializable

/**
 * Every user preference, in one serializable value. Stored as JSON through DataStore so that
 * export/import is the same file, and new fields only need a default to migrate.
 */
@Serializable
data class IslandSettings(
    val schemaVersion: Int = 1,
    /** Master switch: the island is shown while the notification listener is bound and this is true. */
    val islandEnabled: Boolean = true,
    val onboardingDone: Boolean = false,
    val calibration: Calibration = Calibration(),
    val appearance: Appearance = Appearance(),
    val visibility: Visibility = Visibility(),
)

/** Adjustments on top of the detected cutout, in dp. */
@Serializable
data class Calibration(
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    /** Extra width beyond the cutout on each side; null = automatic. */
    val padX: Float? = null,
    val padY: Float? = null,
    /** When the system reports no cutout, where to put the pill. */
    val placement: Placement = Placement.Center,
    /** Smaller "hug the camera" idle pill that is not tappable in Standard mode. */
    val hugCamera: Boolean = false,
)

@Serializable
enum class Placement { Left, Center, Right }

@Serializable
data class Appearance(
    val tint: Tint = Tint.Black,
    val customColor: Long = 0xFF000000,
    val scale: Float = 1f,
    val reduceMotion: Boolean = false,
    val haptics: Boolean = true,
    val idleMode: IdleMode = IdleMode.Always,
)

@Serializable
enum class Tint { Black, MaterialYou, AlbumArt, Custom }

@Serializable
enum class IdleMode { Always, OnlyWithActivity, Never }

@Serializable
data class Visibility(
    val hideInLandscape: Boolean = true,
    val hideInFullscreen: Boolean = true,
    val hideWithKeyboard: Boolean = true,
    val hiddenInApps: Set<String> = emptySet(),
)
