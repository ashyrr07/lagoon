package com.lagoon.island.data

import kotlinx.serialization.Serializable

/** Keep-alive and health. Owned by the app UI work. */
@Serializable
data class KeepAliveSettings(
    val foregroundService: Boolean = false,
    /** Number of times the listener was observed to disconnect; drives the Health screen. */
    val disconnectCount: Int = 0,
    val lastDisconnectAt: Long = 0,
)
