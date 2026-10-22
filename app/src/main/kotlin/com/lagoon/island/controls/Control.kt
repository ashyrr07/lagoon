package com.lagoon.island.controls

import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.flow.StateFlow

/** How a control behaves when the user touches it. */
enum class ControlKind {
    /** Tap flips a boolean (torch, DND, rotation lock). */
    Toggle,

    /** Drag sets a value between 0 and [ControlState.max] (brightness, volume). */
    Slider,

    /** Tap launches an app or activity (camera). */
    Launch,

    /** Tap opens a system settings panel (Wi-Fi, Bluetooth). */
    Panel,

    /** Tap performs an accessibility global action (screenshot, lock, recents). Enhanced mode only. */
    Global,
}

/**
 * Live state of one control.
 *
 * @property active   On/off for toggles; "feature currently on" for panels (Wi-Fi enabled).
 * @property value    Current value for sliders, in 0..[max].
 * @property max      Upper bound for sliders. 1 for toggles.
 * @property enabled  False while the control cannot act right now (no flash unit, DND access missing
 *                    and the tile will open the grant screen instead).
 * @property needsPermission True when tapping opens a system permission screen instead of acting.
 * @property detail   Optional short text for the tile (e.g. "Priority").
 */
data class ControlState(
    val active: Boolean = false,
    val value: Float = 0f,
    val max: Float = 1f,
    val enabled: Boolean = true,
    val needsPermission: Boolean = false,
    val detail: String? = null,
)

/**
 * One tile or slider in the control deck. Everything the UI needs is here; the registry owns the
 * plumbing behind [state] and [perform].
 *
 * @property isAvailable Whether this control exists on this device / build at all (flash unit present,
 *                       Enhanced mode active). Unavailable controls are hidden from the deck and the
 *                       Controls screen.
 * @property perform     Toggle: value ignored (null). Slider: the new value in 0..max. Others: null.
 *                       Runs on the main thread.
 */
class Control(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val kind: ControlKind,
    val isAvailable: () -> Boolean,
    val state: StateFlow<ControlState>,
    val perform: (value: Float?) -> Unit,
    /** Alternate icon while [ControlState.active] (torch off vs on). Null = same icon. */
    val activeIcon: ImageVector? = null,
) {
    override fun toString(): String = "Control($id)"
}
