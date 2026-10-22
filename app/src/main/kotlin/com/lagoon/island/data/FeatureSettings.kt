package com.lagoon.island.data

import kotlinx.serialization.Serializable

/** Per-source switches and options. Owned by the sources work. */
@Serializable
data class FeatureSettings(
    val media: Boolean = true,
    val notifications: Boolean = true,
    /** Packages whose notifications show in the island. Empty = ask during onboarding. */
    val notificationApps: Set<String> = emptySet(),
    /** Remove the system notification after the island showed it. */
    val dismissSystemNotification: Boolean = false,
    val calls: Boolean = true,
    val timers: Boolean = true,
    val charging: Boolean = true,
    val lowBattery: Boolean = true,
    val bluetooth: Boolean = true,
    val navigation: Boolean = true,
    val liveUpdates: Boolean = true,
    val alarms: Boolean = true,
    val ringerMode: Boolean = true,
    val torch: Boolean = true,
    val screenRecording: Boolean = true,
    val noInternet: Boolean = false,
    /** Seconds a transient notification stays in the island. */
    val notificationSeconds: Int = 6,
    /** Seconds a system alert (charging, earbuds) holds the island. */
    val alertSeconds: Int = 3,
)
