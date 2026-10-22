package com.lagoon.island.ui.app.onboarding

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lagoon.island.R
import com.lagoon.island.data.Appearance
import com.lagoon.island.data.IdleMode
import com.lagoon.island.data.IslandSettings
import com.lagoon.island.data.Tint
import com.lagoon.island.ui.app.components.IslandPreview
import kotlin.math.abs

/** One choice in a quick-style row. */
private data class StyleOption<T>(val value: T, @StringRes val label: Int)

private val ColorOptions = listOf(
    StyleOption(Tint.Black, R.string.style_color_black),
    StyleOption(Tint.MaterialYou, R.string.style_color_dynamic),
    StyleOption(Tint.AlbumArt, R.string.style_color_album),
)
private val SizeOptions = listOf(
    StyleOption(0.85f, R.string.style_size_small),
    StyleOption(1f, R.string.style_size_medium),
    StyleOption(1.2f, R.string.style_size_large),
)
private val ShapeOptions = listOf(
    StyleOption(1f, R.string.style_shape_pill),
    StyleOption(0.55f, R.string.style_shape_rounded),
)
private val IdleOptions = listOf(
    StyleOption(IdleMode.Always, R.string.style_idle_always),
    StyleOption(IdleMode.OnlyWithActivity, R.string.style_idle_activity),
)

/**
 * First-run "Make it yours": a few big, obvious choices with a live preview, instead of sending
 * people into the full Appearance screen. Every choice applies at once to the real island too.
 */
@Composable
internal fun ColumnScope.StyleStep(settings: IslandSettings, set: ((Appearance) -> Appearance) -> Unit) {
    val a = settings.appearance
    IslandPreview(settings)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.style_title), style = MaterialTheme.typography.displaySmall)
        Text(
            stringResource(R.string.style_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    ChoiceGroup(R.string.style_color, ColorOptions, selected = { it == a.tint }) { v -> set { it.copy(tint = v) } }
    ChoiceGroup(R.string.style_size, SizeOptions, selected = { abs(it - a.scale) < 0.08f }) { v -> set { it.copy(scale = v) } }
    ChoiceGroup(R.string.style_shape, ShapeOptions, selected = { abs(it - a.roundness) < 0.2f }) { v -> set { it.copy(roundness = v) } }
    ChoiceGroup(R.string.style_idle, IdleOptions, selected = { it == a.idleMode }) { v -> set { it.copy(idleMode = v) } }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChoiceGroup(
    @StringRes title: Int,
    options: List<StyleOption<T>>,
    selected: (T) -> Boolean,
    onSelect: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            stringResource(title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 4.dp),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                val isSelected = selected(option.value)
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelect(option.value) },
                    label = { Text(stringResource(option.label), style = MaterialTheme.typography.labelLarge) },
                    leadingIcon = if (isSelected) {
                        { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.padding(0.dp)) }
                    } else {
                        null
                    },
                    colors = FilterChipDefaults.filterChipColors(),
                )
            }
        }
    }
}
