package com.lagoon.island.ui.app.position

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lagoon.island.cutout.CutoutInfo
import com.lagoon.island.cutout.CutoutShape
import com.lagoon.island.cutout.IslandGeometryDefaults
import com.lagoon.island.data.Calibration
import com.lagoon.island.data.IslandSettings
import com.lagoon.island.data.Placement
import com.lagoon.island.ui.app.SettingsViewModel
import com.lagoon.island.ui.app.components.ChoiceRow
import com.lagoon.island.ui.app.components.IslandPreview
import com.lagoon.island.ui.app.components.Note
import com.lagoon.island.ui.app.components.ScreenScaffold
import com.lagoon.island.ui.app.components.SettingGroup
import com.lagoon.island.ui.app.components.SliderRow
import com.lagoon.island.ui.app.components.SwitchRow
import com.lagoon.island.ui.theme.LagoonPreviewTheme
import kotlin.math.roundToInt

/** Live calibration: every slider moves the real island as you drag, and the preview with it. */
@Composable
fun PositionScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val island by viewModel.islandState.collectAsStateWithLifecycle()
    fun set(transform: (Calibration) -> Calibration) =
        viewModel.update { it.copy(calibration = transform(it.calibration)) }

    ScreenScaffold(
        title = "Position",
        subtitle = "Wrap the camera with an even ring",
        onBack = onBack,
        actions = { TextButton(onClick = { set { Calibration() } }) { Text("Reset") } },
        pinned = { IslandPreview(settings) },
    ) {
        PositionControls(settings, island.cutout, ::set)
    }
}

/** The calibration controls alone, shared with onboarding. */
@Composable
fun PositionControls(
    settings: IslandSettings,
    cutout: CutoutInfo?,
    set: ((Calibration) -> Calibration) -> Unit,
) {
    val cal = settings.calibration
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Note(
            "Look at the top of your screen while you drag. The pill should hug the camera with the same " +
                "gap all around.",
        )
        SettingGroup("Placement") {
            row {
                SliderRow("Horizontal", cal.offsetX, -60f..60f, { set { c -> c.copy(offsetX = it.roundToInt().toFloat()) } }, format = ::dp)
            }
            row {
                SliderRow("Vertical", cal.offsetY, -30f..30f, { set { c -> c.copy(offsetY = it.roundToInt().toFloat()) } }, format = ::dp)
            }
        }
        SettingGroup("Shape") {
            row {
                SliderRow(
                    "Width",
                    cal.idleWidth ?: IslandGeometryDefaults.IDLE_MIN_WIDTH_DP,
                    40f..220f,
                    { set { c -> c.copy(idleWidth = it.roundToInt().toFloat()) } },
                    format = ::dp,
                )
            }
            row {
                SliderRow(
                    "Ring thickness",
                    cal.padY ?: IslandGeometryDefaults.PAD_Y_DP,
                    0f..24f,
                    { set { c -> c.copy(padY = it.roundToInt().toFloat(), padX = it.roundToInt().toFloat()) } },
                    format = ::dp,
                )
            }
            row {
                SwitchRow(
                    "Touch band",
                    checked = cal.touchBand,
                    onCheckedChange = { set { c -> c.copy(touchBand = it) } },
                    subtitle = "Extends the pill a little below the status bar so it can be tapped. " +
                        "Not needed with Enhanced mode.",
                )
            }
        }
        if (cutout?.cutout == null) {
            SettingGroup("No camera cutout reported") {
                row {
                    ChoiceRow(
                        "Where should the island sit?",
                        options = Placement.entries,
                        selected = cal.placement,
                        label = { it.name },
                        onSelect = { set { c -> c.copy(placement = it) } },
                    )
                }
            }
        }
        cutout?.let {
            Note(
                "Detected: ${it.shape.describe()}, status bar ${(it.statusBarHeight / it.density).roundToInt()} dp, " +
                    "${it.screenWidth} × ${it.screenHeight} px.",
                Modifier.padding(bottom = 8.dp),
            )
        }
    }
}

private fun dp(v: Float) = "${v.roundToInt()} dp"

private fun CutoutShape.describe() = when (this) {
    CutoutShape.Hole -> "round camera hole"
    CutoutShape.Pill -> "pill cutout"
    CutoutShape.Notch -> "notch"
    CutoutShape.None -> "no cutout"
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun PositionControlsPreview() {
    LagoonPreviewTheme {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            IslandPreview(IslandSettings())
            PositionControls(IslandSettings(), null) {}
            Text("Live", style = MaterialTheme.typography.labelSmall)
        }
    }
}
