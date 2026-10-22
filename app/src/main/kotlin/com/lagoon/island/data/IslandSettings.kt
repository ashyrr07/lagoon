package com.lagoon.island.data

import kotlinx.serialization.Serializable

/**
 * Every user preference, in one serializable value. Stored as JSON through DataStore so that
 * export/import is the same file, and new fields only need a default to migrate.
 *
 * Each area lives in its own file (FeatureSettings.kt, GestureSettings.kt, ...) so that work on
 * different areas does not collide.
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
    val features: FeatureSettings = FeatureSettings(),
    val gestures: GestureSettings = GestureSettings(),
    val controls: ControlSettings = ControlSettings(),
    val enhanced: EnhancedSettings = EnhancedSettings(),
    val keepAlive: KeepAliveSettings = KeepAliveSettings(),
)

/** Adjustments on top of the detected cutout, in dp. */
@Serializable
data class Calibration(
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    /** Ring thickness around the cutout; null = automatic. */
    val padX: Float? = null,
    val padY: Float? = null,
    /** Idle pill width in dp; null = automatic (at least [IslandGeometryDefaults.IDLE_MIN_WIDTH_DP]). */
    val idleWidth: Float? = null,
    /**
     * Standard mode only: extend the idle pill below the status bar so it can be tapped
     * (SystemUI owns touches inside the status bar). Off by default: looks cleaner.
     */
    val touchBand: Boolean = false,
    /** When the system reports no cutout, where to put the pill. */
    val placement: Placement = Placement.Center,
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
    val cornerSmoothing: Float = 0.6f,
    /** Seconds an expanded view stays open without interaction. */
    val expandedTimeoutSec: Int = 6,
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
