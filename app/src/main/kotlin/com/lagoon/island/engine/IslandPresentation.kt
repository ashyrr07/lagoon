package com.lagoon.island.engine

/** What the island shows right now. */
sealed interface IslandPresentation {
    data object Hidden : IslandPresentation
    data object Idle : IslandPresentation

    /** Primary attached to the cutout; optional secondary as a detached bubble. */
    data class Compact(val primary: IslandActivity, val secondary: IslandActivity?) : IslandPresentation

    /** User opened it (tap/long-press). */
    data class Expanded(val activity: IslandActivity) : IslandPresentation

    /** A system event took the island for a moment (charging, earbuds, incoming call). */
    data class Alert(val activity: IslandActivity) : IslandPresentation

    /** Long-press on an idle island: controls and widgets. */
    data object ControlDeck : IslandPresentation

    val activityOrNull: IslandActivity?
        get() = when (this) {
            is Compact -> primary
            is Expanded -> activity
            is Alert -> activity
            else -> null
        }
}
