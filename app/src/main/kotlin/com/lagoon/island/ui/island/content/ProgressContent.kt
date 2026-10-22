package com.lagoon.island.ui.island.content

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Downloading
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lagoon.island.engine.ActivityContent

/** Thin bar; indeterminate progress sweeps a short segment back and forth. */
@Composable
fun ThinProgressBar(progress: Float?, color: Color, modifier: Modifier = Modifier, reduceMotion: Boolean = false) {
    // Indeterminate sweep: a frame-clock loop that only runs while there is no progress value.
    val sweep = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(progress == null, reduceMotion) {
        if (progress != null) return@LaunchedEffect
        val period = if (reduceMotion) 2400L else 1200L
        while (true) {
            withFrameMillis { t ->
                val phase = (t % (period * 2)).toFloat() / period
                sweep.floatValue = if (phase <= 1f) phase else 2f - phase
            }
        }
    }
    Canvas(modifier.height(4.dp)) {
        val r = CornerRadius(size.height / 2f)
        drawRoundRect(IslandType.white30, Offset.Zero, size, r)
        if (progress != null) {
            drawRoundRect(color, Offset.Zero, Size(size.width * progress.coerceIn(0f, 1f), size.height), r)
        } else {
            val w = size.width * 0.3f
            drawRoundRect(color, Offset((size.width - w) * sweep.floatValue, 0f), Size(w, size.height), r)
        }
    }
}

@Composable
fun ProgressCompactLeading(scope: ContentScope, p: ActivityContent.Progress) {
    IslandIcon(p.icon, fallback = Icons.Rounded.Downloading, size = 22.dp, tint = if (p.icon == null) scope.accent else null)
}

@Composable
fun ProgressCompactTrailing(scope: ContentScope, p: ActivityContent.Progress) {
    Column(Modifier.width(72.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)) {
        Text(p.title, style = IslandType.compact, maxLines = 1, overflow = TextOverflow.Ellipsis)
        ThinProgressBar(p.progress, scope.accent, Modifier.fillMaxWidth(), scope.reduceMotion)
    }
}

@Composable
fun ProgressExpanded(scope: ContentScope, p: ActivityContent.Progress) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IslandIcon(p.icon, fallback = Icons.Rounded.Downloading, size = 36.dp, tint = if (p.icon == null) scope.accent else null)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(p.title, style = IslandType.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                p.text?.let { Text(it, style = IslandType.bodyDim, maxLines = 2, overflow = TextOverflow.Ellipsis) }
            }
            p.progress?.let {
                Spacer(Modifier.width(12.dp))
                Text(percentText((it * 100).toInt()), style = IslandType.body)
            }
        }
        Spacer(Modifier.height(12.dp))
        ThinProgressBar(p.progress, scope.accent, Modifier.fillMaxWidth(), scope.reduceMotion)
        if (scope.activity.actions.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            ActionRow(scope, scope.activity.actions)
        }
    }
}
