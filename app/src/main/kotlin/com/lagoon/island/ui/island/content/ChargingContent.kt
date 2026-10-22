package com.lagoon.island.ui.island.content

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.PowerOff
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.unit.dp
import com.lagoon.island.engine.ActivityContent
import com.lagoon.island.ui.island.TimeFormat

private fun ActivityContent.Charging.glyph() = when (state) {
    ActivityContent.Charging.State.Connected -> Icons.Rounded.Bolt
    ActivityContent.Charging.State.Disconnected -> Icons.Rounded.PowerOff
    ActivityContent.Charging.State.Low -> Icons.Rounded.BatteryAlert
    ActivityContent.Charging.State.Full -> Icons.Rounded.BatteryFull
}

private fun ActivityContent.Charging.color(accent: Color) = when (state) {
    ActivityContent.Charging.State.Low -> Accent.danger
    ActivityContent.Charging.State.Disconnected -> IslandType.white60
    else -> accent
}

private fun ActivityContent.Charging.headline(): String = when (state) {
    ActivityContent.Charging.State.Connected -> "Charging"
    ActivityContent.Charging.State.Disconnected -> "Unplugged"
    ActivityContent.Charging.State.Low -> "Low battery"
    ActivityContent.Charging.State.Full -> "Fully charged"
}

@Composable
fun ChargingCompactLeading(scope: ContentScope, c: ActivityContent.Charging) {
    GlyphDisc(c.glyph(), c.color(scope.accent))
}

@Composable
fun ChargingCompactTrailing(scope: ContentScope, c: ActivityContent.Charging) {
    val text = TimeFormat.watts(c.watts)?.takeIf { c.state == ActivityContent.Charging.State.Connected } ?: "${c.level}%"
    Text(
        text,
        style = IslandType.compact.copy(color = c.color(scope.accent), platformStyle = PlatformTextStyle(includeFontPadding = false)),
        maxLines = 1,
    )
}

/**
 * Ring around the camera: fills to the battery level on arrival, with a slow travelling glint
 * while charging. Drawn in the slot the frame reserves around the camera.
 */
@Composable
fun ChargingRing(scope: ContentScope, c: ActivityContent.Charging, modifier: Modifier = Modifier) {
    val fill = remember { Animatable(0f) }
    LaunchedEffect(c.level, scope.reduceMotion) {
        if (scope.reduceMotion) fill.snapTo(c.level / 100f)
        else fill.animateTo(c.level / 100f, spring(dampingRatio = 0.8f, stiffness = 120f))
    }
    val charging = c.state == ActivityContent.Charging.State.Connected && !scope.reduceMotion
    // Travelling glint: a frame-clock loop that only runs while charging (read in the draw lambda).
    val glint = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(charging) {
        if (!charging) return@LaunchedEffect
        while (true) {
            withFrameMillis { glint.floatValue = (it % 2400L) / 2400f * 360f }
        }
    }
    val color = c.color(scope.accent)
    Canvas(modifier.fillMaxSize()) {
        val stroke = 2.5.dp.toPx()
        val inset = stroke / 2f
        val arcSize = Size(size.width - stroke, size.height - stroke)
        drawArc(IslandType.white30.copy(alpha = 0.18f), 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        drawArc(color, -90f, 360f * fill.value, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        if (charging) {
            drawArc(
                Brush.sweepGradient(
                    0f to Color.Transparent, 0.75f to Color.Transparent, 1f to Color.White.copy(alpha = 0.7f),
                    center = Offset(size.width / 2f, size.height / 2f),
                ),
                glint.floatValue, 70f, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
    }
}

@Composable
fun ChargingExpanded(scope: ContentScope, c: ActivityContent.Charging) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        GlyphDisc(c.glyph(), c.color(scope.accent), size = 40.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(c.headline(), style = IslandType.title)
            val detail = buildString {
                append("${c.level}%")
                TimeFormat.watts(c.watts)?.takeIf { c.state == ActivityContent.Charging.State.Connected }?.let { append(" · $it") }
            }
            Text(detail, style = IslandType.bodyDim)
        }
        Text("${c.level}%", style = IslandType.big.copy(color = c.color(scope.accent)))
    }
}
