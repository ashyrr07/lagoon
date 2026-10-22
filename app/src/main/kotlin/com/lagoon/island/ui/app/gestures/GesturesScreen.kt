package com.lagoon.island.ui.app.gestures

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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    val title: String,
    val get: (GestureSettings) -> GestureAction,
    val put: (GestureSettings, GestureAction) -> GestureSettings,
)

private val IdleSlots = listOf(
    GestureSlot("Tap", { it.idleTap }) { g, a -> g.copy(idleTap = a) },
    GestureSlot("Hold", { it.idleLongPress }) { g, a -> g.copy(idleLongPress = a) },
)
private val CompactSlots = listOf(
    GestureSlot("Tap", { it.compactTap }) { g, a -> g.copy(compactTap = a) },
    GestureSlot("Hold", { it.compactLongPress }) { g, a -> g.copy(compactLongPress = a) },
    GestureSlot("Swipe left", { it.compactSwipeLeft }) { g, a -> g.copy(compactSwipeLeft = a) },
    GestureSlot("Swipe right", { it.compactSwipeRight }) { g, a -> g.copy(compactSwipeRight = a) },
    GestureSlot("Swipe up", { it.compactSwipeUp }) { g, a -> g.copy(compactSwipeUp = a) },
    GestureSlot("Swipe down", { it.compactSwipeDown }) { g, a -> g.copy(compactSwipeDown = a) },
)
private val ExpandedSlots = listOf(
    GestureSlot("Swipe up", { it.expandedSwipeUp }) { g, a -> g.copy(expandedSwipeUp = a) },
)

@Composable
private fun GesturesContent(
    gestures: GestureSettings,
    onBack: () -> Unit,
    set: ((GestureSettings) -> GestureSettings) -> Unit,
) {
    var editing by remember { mutableStateOf<GestureSlot?>(null) }

    ScreenScaffold(
        title = "Gestures",
        subtitle = "What a tap, a hold and each swipe do, depending on what the island is showing.",
        onBack = onBack,
    ) {
        GestureGroup("Idle pill", IdleSlots, gestures) { editing = it }
        GestureGroup("Showing something", CompactSlots, gestures) { editing = it }
        GestureGroup("Expanded", ExpandedSlots, gestures) { editing = it }
        Note("Tapping outside an expanded island always closes it.")
    }

    editing?.let { slot ->
        ActionPickerSheet(
            title = slot.title,
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
                    Text(slot.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Text(
                        slot.get(gestures).label(),
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
                        Text(action.label(), style = MaterialTheme.typography.titleMedium)
                        Text(
                            action.detail(),
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

fun GestureAction.label(): String = when (this) {
    GestureAction.None -> "Nothing"
    GestureAction.Expand -> "Expand"
    GestureAction.Collapse -> "Collapse"
    GestureAction.OpenApp -> "Open the app"
    GestureAction.Dismiss -> "Dismiss"
    GestureAction.ControlDeck -> "Control deck"
    GestureAction.NextActivity -> "Next activity"
    GestureAction.PlayPause -> "Play / pause"
    GestureAction.NextTrack -> "Next track"
    GestureAction.PreviousTrack -> "Previous track"
    GestureAction.Torch -> "Torch"
}

private fun GestureAction.detail(): String = when (this) {
    GestureAction.None -> "The gesture is ignored"
    GestureAction.Expand -> "Open the full view of what is showing"
    GestureAction.Collapse -> "Back to the compact pill"
    GestureAction.OpenApp -> "Jump to the app behind the activity"
    GestureAction.Dismiss -> "Remove the activity from the island"
    GestureAction.ControlDeck -> "Tiles, sliders and widgets"
    GestureAction.NextActivity -> "Cycle to the next thing waiting"
    GestureAction.PlayPause -> "Toggle playback"
    GestureAction.NextTrack -> "Skip forward"
    GestureAction.PreviousTrack -> "Skip back"
    GestureAction.Torch -> "Toggle the flashlight"
}

@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun GesturesPreview() {
    LagoonPreviewTheme {
        GesturesContent(GestureSettings(), onBack = {}, set = {})
    }
}
