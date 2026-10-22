package com.lagoon.island.ui.island.content

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lagoon.island.engine.ActivityContent
import com.lagoon.island.ui.island.TimeFormat

@Composable
fun TimerCompactLeading(scope: ContentScope) {
    GlyphDisc(Icons.Rounded.Timer, scope.accent)
}

@Composable
fun TimerCompactTrailing(scope: ContentScope, timer: ActivityContent.Timer) {
    val now by rememberTicking()
    Text(
        TimeFormat.clock(TimeFormat.timerMillis(timer, now)),
        style = IslandType.compact.copy(color = scope.accent, platformStyle = PlatformTextStyle(includeFontPadding = false)),
        maxLines = 1,
    )
}

@Composable
fun TimerExpanded(scope: ContentScope, timer: ActivityContent.Timer) {
    val now by rememberTicking()
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlyphDisc(Icons.Rounded.Timer, scope.accent, size = 40.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(timer.label.ifBlank { if (timer.endAt != null) "Timer" else "Stopwatch" }, style = IslandType.bodyDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    TimeFormat.clock(TimeFormat.timerMillis(timer, now)),
                    style = IslandType.big.copy(color = if (timer.running) IslandType.white else IslandType.white60),
                    maxLines = 1,
                )
            }
        }
        if (scope.activity.actions.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            ActionRow(scope, scope.activity.actions)
        }
    }
}
