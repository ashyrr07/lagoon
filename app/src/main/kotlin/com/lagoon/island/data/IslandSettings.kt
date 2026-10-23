package com.lagoon.island.data

import kotlinx.serialization.Serializable

/**
 * Every user preference, in one serializable value. Stored as JSON through DataStore so that
 * export/import is the same file, and new fields only need a default to migrate.
 *
 * Each area lives in its own file (FeatureSettings.kt, GestureSettings.kt, ...) so that work on
 * different areas does not collide.
 */
/** Bump when a default changes for existing users; see [SettingsMigration]. */
const val CURRENT_SCHEMA = 2

@Serializable
data class IslandSettings(
    val schemaVersion: Int = CURRENT_SCHEMA,
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
    /**
     * In-app language as a BCP 47 tag ("" = follow the phone). Android 13+ applies it through the
     * system per-app language; this copy lets the overlay follow it on Android 12.
     */
    val language: String = "",
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
     * Standard mode only: extend the idle pill a little below the status bar so it can be tapped
     * (SystemUI owns touches inside the status bar). On by default; ignored in Enhanced mode,
     * where the whole pill is tappable.
     */
    val touchBand: Boolean = true,
    /** When the system reports no cutout, where to put the pill. */
    val placement: Placement = Placement.Center,
)

@Serializable
enum class Placement { Left, Center, Right }

@Serializable
data class Appearance(
    /** Follows the wallpaper (Material You) by default. */
    val tint: Tint = Tint.MaterialYou,
    val customColor: Long = 0xFF000000,
    val scale: Float = 1f,
    val reduceMotion: Boolean = false,
    val haptics: Boolean = true,
    /** Hidden until something happens (a notification, music, a call...), then it drops in. */
    val idleMode: IdleMode = IdleMode.OnlyWithActivity,
    val cornerSmoothing: Float = 0.6f,
    /** Corner radius as a fraction of half the pill height: 1 = full pill, 0.35 = rounded rectangle. */
    val roundness: Float = 1f,
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
