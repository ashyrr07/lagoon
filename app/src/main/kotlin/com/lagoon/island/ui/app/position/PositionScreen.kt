package com.lagoon.island.ui.app.position

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lagoon.island.cutout.IslandGeometry
import com.lagoon.island.data.Calibration
import com.lagoon.island.data.Placement
import com.lagoon.island.ui.app.SettingsViewModel
import kotlin.math.roundToInt

/** Live calibration: every slider moves the real island as you drag. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PositionScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val island by viewModel.islandState.collectAsStateWithLifecycle()
    val cal = settings.calibration
    fun set(transform: (Calibration) -> Calibration) =
        viewModel.update { it.copy(calibration = transform(it.calibration)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Position and size") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = { TextButton(onClick = { set { Calibration() } }) { Text("Reset") } },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                "Look at the top of the screen while you adjust. The pill should wrap the camera with an even ring.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            LabeledSlider("Horizontal offset", cal.offsetX, -60f..60f, " dp") { set { c -> c.copy(offsetX = it) } }
            LabeledSlider("Vertical offset", cal.offsetY, -30f..30f, " dp") { set { c -> c.copy(offsetY = it) } }
            LabeledSlider(
                "Side padding",
                cal.padX ?: IslandGeometry.PAD_X_DP,
                0f..40f,
                " dp",
            ) { set { c -> c.copy(padX = it) } }
            LabeledSlider(
                "Top and bottom padding",
                cal.padY ?: IslandGeometry.PAD_Y_DP,
                0f..24f,
                " dp",
            ) { set { c -> c.copy(padY = it) } }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Hug the camera", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Tight ring around the cutout. Looks closest to iPhone, but the idle pill is not tappable " +
                            "without Enhanced mode.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = cal.hugCamera, onCheckedChange = { set { c -> c.copy(hugCamera = it) } })
            }

            if (island.cutout?.cutout == null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("No camera cutout reported. Where should the island sit?", style = MaterialTheme.typography.titleMedium)
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        Placement.entries.forEachIndexed { index, placement ->
                            SegmentedButton(
                                selected = cal.placement == placement,
                                onClick = { set { c -> c.copy(placement = placement) } },
                                shape = SegmentedButtonDefaults.itemShape(index, Placement.entries.size),
                            ) { Text(placement.name) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    unit: String,
    onChange: (Float) -> Unit,
) {
    Column {
        Row(Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text("${value.roundToInt()}$unit", style = MaterialTheme.typography.labelLarge)
        }
        Slider(value = value, onValueChange = { onChange(it.roundToInt().toFloat()) }, valueRange = range)
    }
}
