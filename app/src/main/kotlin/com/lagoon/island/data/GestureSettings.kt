package com.lagoon.island.data

import kotlinx.serialization.Serializable

@Serializable
enum class GestureAction {
    None, Expand, Collapse, OpenApp, Dismiss, ControlDeck, NextActivity, PlayPause, NextTrack, PreviousTrack, Torch,
}

/** What each gesture does, per island state. Owned by the island UI work. */
@Serializable
data class GestureSettings(
    val idleTap: GestureAction = GestureAction.ControlDeck,
    val idleLongPress: GestureAction = GestureAction.ControlDeck,
    val compactTap: GestureAction = GestureAction.Expand,
    val compactLongPress: GestureAction = GestureAction.OpenApp,
    val compactSwipeLeft: GestureAction = GestureAction.NextActivity,
    val compactSwipeRight: GestureAction = GestureAction.NextActivity,
    val compactSwipeUp: GestureAction = GestureAction.Dismiss,
    val compactSwipeDown: GestureAction = GestureAction.ControlDeck,
    val expandedSwipeUp: GestureAction = GestureAction.Collapse,
)
