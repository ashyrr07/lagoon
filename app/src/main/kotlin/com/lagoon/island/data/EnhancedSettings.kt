package com.lagoon.island.data

import kotlinx.serialization.Serializable

/** Enhanced (accessibility) mode. Owned by the enhanced-mode work. */
@Serializable
data class EnhancedSettings(
    /** The user read the prominent disclosure and consented. */
    val consented: Boolean = false,
    val showOnLockScreen: Boolean = true,
)
