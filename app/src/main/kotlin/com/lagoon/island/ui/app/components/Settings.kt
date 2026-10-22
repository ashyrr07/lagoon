package com.lagoon.island.ui.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.lagoon.island.ui.theme.GroupRadius
import com.lagoon.island.ui.theme.TABULAR_FIGURES
import kotlin.math.roundToInt

/*
 * The settings kit. Screens are built from grouped lists: one rounded container per group, rows
 * divided by hairlines. The group container is the only "card" in the app; status and emphasis are
 * carried by colour and the leading glyph, never by extra boxes.
 */

private val RowPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp)

class GroupBuilder {
    internal val rows = mutableListOf<@Composable () -> Unit>()

    fun row(content: @Composable () -> Unit) {
        rows += content
    }
}

/** A titled group of rows. The title sits outside the container, left-aligned with the content. */
@Composable
fun SettingGroup(
    title: String?,
    modifier: Modifier = Modifier,
    tone: Color = MaterialTheme.colorScheme.surfaceContainer,
    build: GroupBuilder.() -> Unit,
) {
    val rows = GroupBuilder().apply(build).rows
    Column(modifier.fillMaxWidth()) {
        if (title != null) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 20.dp, bottom = 8.dp),
            )
        }
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(GroupRadius))
                .background(tone),
        ) {
            rows.forEachIndexed { index, row ->
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    )
                }
                row()
            }
        }
    }
}

/** Round glyph that leads a navigation row. Tone names the group, not the row. */
@Composable
fun Glyph(icon: ImageVector, tint: Color, modifier: Modifier = Modifier) {
    val size = 40.dp
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(50))
            .background(tint.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.55f))
    }
}

@Composable
private fun RowTitle(title: String, subtitle: String?, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val alpha = if (enabled) 1f else 0.5f
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
        )
        if (subtitle != null) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
            )
        }
    }
}

/** Opens another screen. */
@Composable
fun NavRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    tint: Color = MaterialTheme.colorScheme.primary,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick, role = Role.Button)
            .padding(RowPadding)
            .heightIn(min = 40.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (icon != null) Glyph(icon, tint)
        RowTitle(title, subtitle, Modifier.weight(1f))
        if (trailing != null) trailing()
        Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null,
    enabled: Boolean = true,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, role = Role.Switch) { onCheckedChange(!checked) }
            .semantics { toggleableState = if (checked) ToggleableState.On else ToggleableState.Off }
            .padding(RowPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        RowTitle(title, subtitle, Modifier.weight(1f), enabled)
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

/** Numeric read-out that does not jitter: tabular figures. */
val TabularStyle: TextStyle
    @Composable get() = MaterialTheme.typography.titleMedium.copy(
        fontFeatureSettings = TABULAR_FIGURES,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )

@Composable
fun SliderRow(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
    subtitle: String? = null,
    steps: Int = 0,
    format: (Float) -> String = { "${it.roundToInt()}" },
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RowTitle(title, subtitle, Modifier.weight(1f))
            Spacer(Modifier.width(12.dp))
            Text(format(value), style = TabularStyle, color = MaterialTheme.colorScheme.primary)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range, steps = steps)
    }
}

/** A labelled set of mutually exclusive options under a title. */
@Composable
fun <T> ChoiceRow(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    subtitle: String? = null,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(RowPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RowTitle(title, subtitle)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                    label = { Text(label(option), maxLines = 1) },
                )
            }
        }
    }
}

/** A row that only shows a value, for the About screen and diagnostics. */
@Composable
fun ValueRow(title: String, value: String, subtitle: String? = null, onClick: (() -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(RowPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        RowTitle(title, subtitle, Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Free-form row content with the standard padding. */
@Composable
fun ContentRow(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(RowPadding), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
}

/** The three states a setup check can be in. Colour and the ring shape carry it, not just text. */
enum class Health { Ok, Attention, Off }

@Composable
fun healthColor(health: Health): Color = when (health) {
    Health.Ok -> MaterialTheme.colorScheme.primary
    Health.Attention -> MaterialTheme.colorScheme.tertiary
    Health.Off -> MaterialTheme.colorScheme.outline
}

/** A small island: filled when healthy, ringed when it needs attention, hollow when off. */
@Composable
fun HealthPill(health: Health, modifier: Modifier = Modifier) {
    val color = healthColor(health)
    Box(
        modifier
            .width(22.dp)
            .height(12.dp)
            .clip(RoundedCornerShape(50))
            .background(if (health == Health.Ok) color else color.copy(alpha = 0.25f)),
        contentAlignment = Alignment.Center,
    ) {
        if (health != Health.Ok) {
            Box(
                Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(color),
            )
        }
    }
}

/** One line of the setup checklist, with an optional fix action. */
@Composable
fun StatusRow(
    title: String,
    detail: String,
    health: Health,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onAction != null) Modifier.clickable(onClick = onAction) else Modifier)
            .padding(RowPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        HealthPill(health)
        RowTitle(title, detail, Modifier.weight(1f))
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction, contentPadding = PaddingValues(horizontal = 12.dp)) { Text(actionLabel) }
        }
    }
}

/** The one primary action of a screen: full width, pill, 56 dp. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

/** Body copy that is allowed to be long: a note under a group or a disclosure. */
@Composable
fun Note(text: String, modifier: Modifier = Modifier) {
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = modifier.padding(horizontal = 20.dp),
        )
    }
}
