package com.lagoon.island.ui.island.content

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lagoon.island.engine.ActivityContent

private fun ActivityContent.Bluetooth.glyph() = when (kind) {
    ActivityContent.Bluetooth.Kind.Earbuds, ActivityContent.Bluetooth.Kind.Headphones -> Icons.Rounded.Headphones
    ActivityContent.Bluetooth.Kind.Watch -> Icons.Rounded.Watch
    ActivityContent.Bluetooth.Kind.Speaker -> Icons.Rounded.Speaker
    ActivityContent.Bluetooth.Kind.Other -> Icons.Rounded.Bluetooth
}

private fun ActivityContent.Bluetooth.batteryText(): String? = when {
    left != null && right != null -> "L ${left}% · R ${right}%"
    battery != null -> "${battery}%"
    else -> null
}

@Composable
fun BluetoothCompactLeading(scope: ContentScope, b: ActivityContent.Bluetooth) {
    GlyphDisc(b.glyph(), if (b.connected) scope.accent else IslandType.white60)
}

@Composable
fun BluetoothCompactTrailing(scope: ContentScope, b: ActivityContent.Bluetooth) {
    val text = if (!b.connected) "Disconnected" else b.batteryText() ?: "Connected"
    Text(text, style = IslandType.compact.copy(color = if (b.connected) scope.accent else IslandType.white60), maxLines = 1)
}

/** A small ring + percentage for one battery. */
@Composable
private fun BatteryRing(label: String, percent: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(40.dp)) {
                val stroke = 3.dp.toPx()
                val inset = stroke / 2f
                val arc = Size(size.width - stroke, size.height - stroke)
                drawArc(IslandType.white30.copy(alpha = 0.18f), 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(stroke, cap = StrokeCap.Round))
                drawArc(if (percent <= 15) Accent.danger else color, -90f, 360f * percent / 100f, false, Offset(inset, inset), arc, style = Stroke(stroke, cap = StrokeCap.Round))
            }
            Text("$percent", style = IslandType.label.copy(color = IslandType.white))
        }
        Text(label, style = IslandType.label)
    }
}

@Composable
fun BluetoothExpanded(scope: ContentScope, b: ActivityContent.Bluetooth) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlyphDisc(b.glyph(), if (b.connected) scope.accent else IslandType.white60, size = 40.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(b.deviceName, style = IslandType.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    if (b.connected) b.batteryText()?.let { "Connected · $it" } ?: "Connected" else "Disconnected",
                    style = IslandType.bodyDim,
                )
            }
        }
        val rings = buildList {
            b.left?.let { add("Left" to it) }
            b.right?.let { add("Right" to it) }
            b.case?.let { add("Case" to it) }
            if (isEmpty()) b.battery?.let { add("Battery" to it) }
        }
        if (b.connected && rings.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally)) {
                rings.forEach { (label, pct) -> BatteryRing(label, pct, scope.accent) }
            }
        }
    }
}
