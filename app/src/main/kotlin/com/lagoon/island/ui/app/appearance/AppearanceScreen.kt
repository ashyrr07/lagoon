package com.lagoon.island.ui.app.appearance

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    ScreenScaffold(
        title = "Appearance",
        onBack = onBack,
        pinned = { IslandPreview(settings) },
    ) {
        SettingGroup("Colour") {
            row {
                ChoiceRow(
                    "Tint",
                    options = Tint.entries,
                    selected = a.tint,
                    label = { it.label() },
                    onSelect = { set { s -> s.copy(tint = it) } },
                    subtitle = when (a.tint) {
                        Tint.Black -> "Pure black, like the hardware around the camera"
                        Tint.MaterialYou -> "Follows your wallpaper colours"
                        Tint.AlbumArt -> "Takes the colour of what is playing"
                        Tint.Custom -> "Pick any colour"
                    },
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
        SettingGroup("Shape") {
            row {
                SliderRow(
                    "Size",
                    a.scale,
                    0.8f..1.3f,
                    { set { s -> s.copy(scale = (it * 20).roundToInt() / 20f) } },
                    format = { "${(it * 100).roundToInt()} %" },
                )
            }
            row {
                SliderRow(
                    "Corner smoothing",
                    a.cornerSmoothing,
                    0f..1f,
                    { set { s -> s.copy(cornerSmoothing = (it * 20).roundToInt() / 20f) } },
                    subtitle = "0 is a plain round corner, 1 is the iPhone-style continuous curve",
                    format = { "${(it * 100).roundToInt()} %" },
                )
            }
        }
        SettingGroup("Behaviour") {
            row {
                ChoiceRow(
                    "Idle pill",
                    options = IdleMode.entries,
                    selected = a.idleMode,
                    label = { it.label() },
                    onSelect = { set { s -> s.copy(idleMode = it) } },
                    subtitle = "When nothing is happening",
                )
            }
            row {
                SliderRow(
                    "Expanded stays open",
                    a.expandedTimeoutSec.toFloat(),
                    2f..15f,
                    { set { s -> s.copy(expandedTimeoutSec = it.roundToInt()) } },
                    steps = 12,
                    format = { "${it.roundToInt()} s" },
                )
            }
        }
        SettingGroup("Motion") {
            row {
                SwitchRow(
                    "Reduce motion",
                    checked = a.reduceMotion,
                    onCheckedChange = { set { s -> s.copy(reduceMotion = it) } },
                    subtitle = "Crossfades instead of springs. Also follows the system animation scale.",
                )
            }
            row {
                SwitchRow(
                    "Haptics",
                    checked = a.haptics,
                    onCheckedChange = { set { s -> s.copy(haptics = it) } },
                    subtitle = "A light tick when the island opens or a tile toggles",
                )
            }
        }
    }
}

private fun Tint.label() = when (this) {
    Tint.Black -> "Black"
    Tint.MaterialYou -> "You"
    Tint.AlbumArt -> "Art"
    Tint.Custom -> "Custom"
}

private fun IdleMode.label() = when (this) {
    IdleMode.Always -> "Always"
    IdleMode.OnlyWithActivity -> "With activity"
    IdleMode.Never -> "Never"
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
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Swatches.take(6).forEach { Swatch(it, it == color, Modifier.weight(1f)) { onChange(it) } }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Swatches.drop(6).forEach { Swatch(it, it == color, Modifier.weight(1f)) { onChange(it) } }
        }
        HsvSlider("Hue", hsv[0], 0f..360f, { "${it.roundToInt()}°" }) { emit(h = it) }
        HsvSlider("Saturation", hsv[1], 0f..1f, { "${(it * 100).roundToInt()} %" }) { emit(s = it) }
        HsvSlider("Brightness", hsv[2], 0f..1f, { "${(it * 100).roundToInt()} %" }) { emit(v = it) }
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
