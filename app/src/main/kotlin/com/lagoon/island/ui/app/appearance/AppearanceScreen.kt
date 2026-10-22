package com.lagoon.island.ui.app.appearance

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lagoon.island.R
import com.lagoon.island.data.Appearance
import com.lagoon.island.data.IdleMode
import com.lagoon.island.data.IslandSettings
import com.lagoon.island.data.Tint
import com.lagoon.island.ui.app.SettingsViewModel
import com.lagoon.island.ui.app.components.ChoiceRow
import com.lagoon.island.ui.app.components.IslandPreview
import com.lagoon.island.ui.app.components.ScreenScaffold
import com.lagoon.island.ui.app.components.SettingGroup
import com.lagoon.island.ui.app.components.SliderRow
import com.lagoon.island.ui.app.components.SwitchRow
import com.lagoon.island.ui.app.components.TabularStyle
import com.lagoon.island.ui.theme.LagoonPreviewTheme
import kotlin.math.roundToInt

@Composable
fun AppearanceScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    AppearanceContent(
        settings = settings,
        onBack = onBack,
        set = { transform -> viewModel.update { it.copy(appearance = transform(it.appearance)) } },
    )
}

@Composable
private fun AppearanceContent(
    settings: IslandSettings,
    onBack: () -> Unit,
    set: ((Appearance) -> Appearance) -> Unit,
) {
    val a = settings.appearance
    val percent = stringResource(R.string.screens_unit_percent)
    val seconds = stringResource(R.string.screens_unit_seconds)
    val tintLabels = Tint.entries.associateWith { stringResource(it.labelRes()) }
    val idleLabels = IdleMode.entries.associateWith { stringResource(it.labelRes()) }
    ScreenScaffold(
        title = stringResource(R.string.screens_appearance_title),
        onBack = onBack,
        pinned = { IslandPreview(settings) },
    ) {
        SettingGroup(stringResource(R.string.screens_appearance_group_colour)) {
            row {
                ChoiceRow(
                    stringResource(R.string.screens_appearance_tint),
                    options = Tint.entries,
                    selected = a.tint,
                    label = { tintLabels.getValue(it) },
                    onSelect = { set { s -> s.copy(tint = it) } },
                    subtitle = stringResource(
                        when (a.tint) {
                            Tint.Black -> R.string.screens_appearance_tint_black_detail
                            Tint.MaterialYou -> R.string.screens_appearance_tint_you_detail
                            Tint.AlbumArt -> R.string.screens_appearance_tint_art_detail
                            Tint.Custom -> R.string.screens_appearance_tint_custom_detail
                        },
                    ),
                )
            }
            if (a.tint == Tint.Custom) {
                row {
                    ColorPicker(
                        color = a.customColor,
                        onChange = { set { s -> s.copy(customColor = it) } },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    )
                }
            }
        }
        SettingGroup(stringResource(R.string.screens_appearance_group_shape)) {
            row {
                SliderRow(
                    stringResource(R.string.screens_appearance_size),
                    a.scale,
                    0.8f..1.3f,
                    { set { s -> s.copy(scale = (it * 20).roundToInt() / 20f) } },
                    format = { percent.format((it * 100).roundToInt()) },
                )
            }
            row {
                SliderRow(
                    stringResource(R.string.screens_appearance_corner_smoothing),
                    a.cornerSmoothing,
                    0f..1f,
                    { set { s -> s.copy(cornerSmoothing = (it * 20).roundToInt() / 20f) } },
                    subtitle = stringResource(R.string.screens_appearance_corner_smoothing_detail),
                    format = { percent.format((it * 100).roundToInt()) },
                )
            }
        }
        SettingGroup(stringResource(R.string.screens_appearance_group_behaviour)) {
            row {
                ChoiceRow(
                    stringResource(R.string.screens_appearance_idle_pill),
                    options = IdleMode.entries,
                    selected = a.idleMode,
                    label = { idleLabels.getValue(it) },
                    onSelect = { set { s -> s.copy(idleMode = it) } },
                    subtitle = stringResource(R.string.screens_appearance_idle_pill_detail),
                )
            }
            row {
                SliderRow(
                    stringResource(R.string.screens_appearance_expanded_timeout),
                    a.expandedTimeoutSec.toFloat(),
                    2f..15f,
                    { set { s -> s.copy(expandedTimeoutSec = it.roundToInt()) } },
                    steps = 12,
                    format = { seconds.format(it.roundToInt()) },
                )
            }
        }
        SettingGroup(stringResource(R.string.screens_appearance_group_motion)) {
            row {
                SwitchRow(
                    stringResource(R.string.screens_appearance_reduce_motion),
                    checked = a.reduceMotion,
                    onCheckedChange = { set { s -> s.copy(reduceMotion = it) } },
                    subtitle = stringResource(R.string.screens_appearance_reduce_motion_detail),
                )
            }
            row {
                SwitchRow(
                    stringResource(R.string.screens_appearance_haptics),
                    checked = a.haptics,
                    onCheckedChange = { set { s -> s.copy(haptics = it) } },
                    subtitle = stringResource(R.string.screens_appearance_haptics_detail),
                )
            }
        }
    }
}

@StringRes
private fun Tint.labelRes(): Int = when (this) {
    Tint.Black -> R.string.screens_tint_black
    Tint.MaterialYou -> R.string.screens_tint_material_you
    Tint.AlbumArt -> R.string.screens_tint_album_art
    Tint.Custom -> R.string.screens_tint_custom
}

@StringRes
private fun IdleMode.labelRes(): Int = when (this) {
    IdleMode.Always -> R.string.screens_idle_always
    IdleMode.OnlyWithActivity -> R.string.screens_idle_with_activity
    IdleMode.Never -> R.string.screens_idle_never
}

private val Swatches = listOf(
    0xFF000000, 0xFF1C1C1E, 0xFF0B2F3B, 0xFF00695E, 0xFF1E3A8A, 0xFF4C1D95,
    0xFF7F1D1D, 0xFF9A3412, 0xFF3F3F46, 0xFF14532D, 0xFF831843, 0xFF5FE3D0,
)

/** Swatches for the quick pick, hue / saturation / brightness sliders for anything else. */
@Composable
fun ColorPicker(color: Long, onChange: (Long) -> Unit, modifier: Modifier = Modifier) {
    val hsv = FloatArray(3).also { android.graphics.Color.colorToHSV(color.toInt(), it) }
    fun emit(h: Float = hsv[0], s: Float = hsv[1], v: Float = hsv[2]) {
        onChange(android.graphics.Color.HSVToColor(floatArrayOf(h, s, v)).toLong() and 0xFFFFFFFFL)
    }
    val percent = stringResource(R.string.screens_unit_percent)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Swatches.take(6).forEach { Swatch(it, it == color, Modifier.weight(1f)) { onChange(it) } }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Swatches.drop(6).forEach { Swatch(it, it == color, Modifier.weight(1f)) { onChange(it) } }
        }
        HsvSlider(stringResource(R.string.screens_color_hue), hsv[0], 0f..360f, { "${it.roundToInt()}°" }) { emit(h = it) }
        HsvSlider(stringResource(R.string.screens_color_saturation), hsv[1], 0f..1f, { percent.format((it * 100).roundToInt()) }) { emit(s = it) }
        HsvSlider(stringResource(R.string.screens_color_brightness), hsv[2], 0f..1f, { percent.format((it * 100).roundToInt()) }) { emit(v = it) }
    }
}

@Composable
private fun Swatch(value: Long, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(value))
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick),
    )
}

@Composable
private fun HsvSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    format: (Float) -> String,
    onChange: (Float) -> Unit,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(format(value), style = TabularStyle, color = MaterialTheme.colorScheme.primary)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}

@Preview(showBackground = true, heightDp = 1300)
@Composable
private fun AppearancePreview() {
    LagoonPreviewTheme {
        AppearanceContent(
            settings = IslandSettings(appearance = Appearance(tint = Tint.Custom, customColor = 0xFF0B2F3B)),
            onBack = {},
            set = {},
        )
    }
}
