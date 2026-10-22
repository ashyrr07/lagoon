package com.lagoon.island.ui.app.position

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lagoon.island.R
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
        title = stringResource(R.string.setup_position_title),
        subtitle = stringResource(R.string.setup_position_subtitle),
        onBack = onBack,
        actions = { TextButton(onClick = { set { Calibration() } }) { Text(stringResource(R.string.setup_position_reset)) } },
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
    val dpPattern = stringResource(R.string.setup_position_dp)
    val dp: (Float) -> String = { dpPattern.format(it.roundToInt()) }
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Note(stringResource(R.string.setup_position_note))
        SettingGroup(stringResource(R.string.setup_position_placement)) {
            row {
                SliderRow(stringResource(R.string.setup_position_horizontal), cal.offsetX, -60f..60f, { set { c -> c.copy(offsetX = it.roundToInt().toFloat()) } }, format = dp)
            }
            row {
                SliderRow(stringResource(R.string.setup_position_vertical), cal.offsetY, -30f..30f, { set { c -> c.copy(offsetY = it.roundToInt().toFloat()) } }, format = dp)
            }
        }
        SettingGroup(stringResource(R.string.setup_position_shape)) {
            row {
                SliderRow(
                    stringResource(R.string.setup_position_width),
                    cal.idleWidth ?: IslandGeometryDefaults.IDLE_MIN_WIDTH_DP,
                    40f..220f,
                    { set { c -> c.copy(idleWidth = it.roundToInt().toFloat()) } },
                    format = dp,
                )
            }
            row {
                SliderRow(
                    stringResource(R.string.setup_position_ring),
                    cal.padY ?: IslandGeometryDefaults.PAD_Y_DP,
                    0f..24f,
                    { set { c -> c.copy(padY = it.roundToInt().toFloat(), padX = it.roundToInt().toFloat()) } },
                    format = dp,
                )
            }
            row {
                SwitchRow(
                    stringResource(R.string.setup_position_touch_band),
                    checked = cal.touchBand,
                    onCheckedChange = { set { c -> c.copy(touchBand = it) } },
                    subtitle = stringResource(R.string.setup_position_touch_band_detail),
                )
            }
        }
        if (cutout?.cutout == null) {
            SettingGroup(stringResource(R.string.setup_position_no_cutout)) {
                row {
                    val left = stringResource(R.string.setup_position_left)
                    val center = stringResource(R.string.setup_position_center)
                    val right = stringResource(R.string.setup_position_right)
                    ChoiceRow(
                        stringResource(R.string.setup_position_where),
                        options = Placement.entries,
                        selected = cal.placement,
                        label = {
                            when (it) {
                                Placement.Left -> left
                                Placement.Center -> center
                                Placement.Right -> right
                            }
                        },
                        onSelect = { set { c -> c.copy(placement = it) } },
                    )
                }
            }
        }
        cutout?.let {
            Note(
                stringResource(
                    R.string.setup_position_detected,
                    stringResource(it.shape.describe()),
                    (it.statusBarHeight / it.density).roundToInt(),
                    it.screenWidth,
                    it.screenHeight,
                ),
                Modifier.padding(bottom = 8.dp),
            )
        }
    }
}

@StringRes
private fun CutoutShape.describe(): Int = when (this) {
    CutoutShape.Hole -> R.string.setup_position_shape_hole
    CutoutShape.Pill -> R.string.setup_position_shape_pill
    CutoutShape.Notch -> R.string.setup_position_shape_notch
    CutoutShape.None -> R.string.setup_position_shape_none
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
