package com.lagoon.island.ui.app

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/*
 * TEMPORARY STUB: delete this file when merging with the branches that declare these keys for real
 * (demo, apps, features, controls and enhanced-mode work declare them in Navigation.kt). It only
 * lets the settings-app branch compile and run on its own; Navigation.kt's fallback entry shows a
 * "not in this build" screen for them until the real entries exist.
 */
@Serializable data object DemoKey : NavKey
@Serializable data object AppsKey : NavKey
@Serializable data object FeaturesKey : NavKey
@Serializable data object ControlsKey : NavKey
@Serializable data object EnhancedKey : NavKey
