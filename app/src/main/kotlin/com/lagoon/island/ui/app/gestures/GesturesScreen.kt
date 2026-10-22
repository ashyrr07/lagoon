package com.lagoon.island.ui.app.gestures

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lagoon.island.R
import com.lagoon.island.data.GestureAction
import com.lagoon.island.data.GestureSettings
import com.lagoon.island.ui.app.SettingsViewModel
import com.lagoon.island.ui.app.components.Note
import com.lagoon.island.ui.app.components.ScreenScaffold
import com.lagoon.island.ui.app.components.SettingGroup
import com.lagoon.island.ui.theme.LagoonPreviewTheme

@Composable
fun GesturesScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    GesturesContent(
        gestures = settings.gestures,
        onBack = onBack,
        set = { transform -> viewModel.update { it.copy(gestures = transform(it.gestures)) } },
    )
}

/** One pickable gesture: where it happens, what it is, and how to read and write it. */
private data class GestureSlot(
    @StringRes val title: Int,
    val get: (GestureSettings) -> GestureAction,
    val put: (GestureSettings, GestureAction) -> GestureSettings,
)

private val IdleSlots = listOf(
    GestureSlot(R.string.screens_gesture_tap, { it.idleTap }) { g, a -> g.copy(idleTap = a) },
    GestureSlot(R.string.screens_gesture_hold, { it.idleLongPress }) { g, a -> g.copy(idleLongPress = a) },
)
private val CompactSlots = listOf(
    GestureSlot(R.string.screens_gesture_tap, { it.compactTap }) { g, a -> g.copy(compactTap = a) },
    GestureSlot(R.string.screens_gesture_hold, { it.compactLongPress }) { g, a -> g.copy(compactLongPress = a) },
    GestureSlot(R.string.screens_gesture_swipe_left, { it.compactSwipeLeft }) { g, a -> g.copy(compactSwipeLeft = a) },
    GestureSlot(R.string.screens_gesture_swipe_right, { it.compactSwipeRight }) { g, a -> g.copy(compactSwipeRight = a) },
    GestureSlot(R.string.screens_gesture_swipe_up, { it.compactSwipeUp }) { g, a -> g.copy(compactSwipeUp = a) },
    GestureSlot(R.string.screens_gesture_swipe_down, { it.compactSwipeDown }) { g, a -> g.copy(compactSwipeDown = a) },
)
private val ExpandedSlots = listOf(
    GestureSlot(R.string.screens_gesture_swipe_up, { it.expandedSwipeUp }) { g, a -> g.copy(expandedSwipeUp = a) },
)

@Composable
private fun GesturesContent(
    gestures: GestureSettings,
    onBack: () -> Unit,
    set: ((GestureSettings) -> GestureSettings) -> Unit,
) {
    var editing by remember { mutableStateOf<GestureSlot?>(null) }

    ScreenScaffold(
        title = stringResource(R.string.screens_gestures_title),
        subtitle = stringResource(R.string.screens_gestures_subtitle),
        onBack = onBack,
    ) {
        GestureGroup(stringResource(R.string.screens_gestures_group_idle), IdleSlots, gestures) { editing = it }
        GestureGroup(stringResource(R.string.screens_gestures_group_compact), CompactSlots, gestures) { editing = it }
        GestureGroup(stringResource(R.string.screens_gestures_group_expanded), ExpandedSlots, gestures) { editing = it }
        Note(stringResource(R.string.screens_gestures_note))
    }

    editing?.let { slot ->
        ActionPickerSheet(
            title = stringResource(slot.title),
            selected = slot.get(gestures),
            onSelect = { action ->
                set { slot.put(it, action) }
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun GestureGroup(
    title: String,
    slots: List<GestureSlot>,
    gestures: GestureSettings,
    onEdit: (GestureSlot) -> Unit,
) {
    SettingGroup(title) {
        slots.forEach { slot ->
            row {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) { onEdit(slot) }
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(slot.title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Text(
                        stringResource(slot.get(gestures).labelRes()),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionPickerSheet(
    title: String,
    selected: GestureAction,
    onSelect: (GestureAction) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp),
            )
            GestureAction.entries.forEach { action ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.RadioButton) { onSelect(action) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    RadioButton(selected = action == selected, onClick = null)
                    Column {
                        Text(stringResource(action.labelRes()), style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(action.detailRes()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@StringRes
fun GestureAction.labelRes(): Int = when (this) {
    GestureAction.None -> R.string.screens_action_none
    GestureAction.Expand -> R.string.screens_action_expand
    GestureAction.Collapse -> R.string.screens_action_collapse
    GestureAction.OpenApp -> R.string.screens_action_open_app
    GestureAction.Dismiss -> R.string.screens_action_dismiss
    GestureAction.ControlDeck -> R.string.screens_action_control_deck
    GestureAction.NextActivity -> R.string.screens_action_next_activity
    GestureAction.PlayPause -> R.string.screens_action_play_pause
    GestureAction.NextTrack -> R.string.screens_action_next_track
    GestureAction.PreviousTrack -> R.string.screens_action_previous_track
    GestureAction.Torch -> R.string.screens_action_torch
}

@StringRes
private fun GestureAction.detailRes(): Int = when (this) {
    GestureAction.None -> R.string.screens_action_none_detail
    GestureAction.Expand -> R.string.screens_action_expand_detail
    GestureAction.Collapse -> R.string.screens_action_collapse_detail
    GestureAction.OpenApp -> R.string.screens_action_open_app_detail
    GestureAction.Dismiss -> R.string.screens_action_dismiss_detail
    GestureAction.ControlDeck -> R.string.screens_action_control_deck_detail
    GestureAction.NextActivity -> R.string.screens_action_next_activity_detail
    GestureAction.PlayPause -> R.string.screens_action_play_pause_detail
    GestureAction.NextTrack -> R.string.screens_action_next_track_detail
    GestureAction.PreviousTrack -> R.string.screens_action_previous_track_detail
    GestureAction.Torch -> R.string.screens_action_torch_detail
}

@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun GesturesPreview() {
    LagoonPreviewTheme {
        GesturesContent(GestureSettings(), onBack = {}, set = {})
    }
}
