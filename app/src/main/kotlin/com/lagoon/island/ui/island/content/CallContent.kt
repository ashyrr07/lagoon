package com.lagoon.island.ui.island.content

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lagoon.island.R
import com.lagoon.island.engine.ActivityContent
import com.lagoon.island.sources.system.CallSource
import com.lagoon.island.ui.island.TimeFormat

@Composable
private fun CallPhoto(call: ActivityContent.Call, size: Dp, accent: Color) {
    val image = rememberBitmap(call.photo)
    if (image != null) {
        Image(image, null, Modifier.size(size).clip(CircleShape), contentScale = ContentScale.Crop)
    } else {
        Box(Modifier.size(size).clip(CircleShape).background(accent), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Person, null, tint = Color.Black, modifier = Modifier.size(size * 0.62f))
        }
    }
}

@Composable
fun CallCompactLeading(scope: ContentScope, call: ActivityContent.Call, slotHeight: Dp) {
    CallPhoto(call, size = (slotHeight - 18.dp).coerceAtLeast(20.dp), accent = scope.accent)
}

@Composable
fun CallCompactTrailing(scope: ContentScope, call: ActivityContent.Call) {
    when (call.state) {
        ActivityContent.Call.State.Ringing -> Text(
            call.name,
            style = IslandType.compact,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(96.dp),
        )
        ActivityContent.Call.State.Active -> {
            val now by rememberTicking()
            Text(TimeFormat.clock(now - call.startedAt), style = IslandType.compact.copy(color = scope.accent, platformStyle = PlatformTextStyle(includeFontPadding = false)))
        }
        ActivityContent.Call.State.Ended -> Text(stringResource(R.string.island_call_ended_short), style = IslandType.compactDim)
    }
}

@Composable
fun CallExpanded(scope: ContentScope, call: ActivityContent.Call) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CallPhoto(call, 48.dp, scope.accent)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(call.name, style = IslandType.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val subtitle = when (call.state) {
                    ActivityContent.Call.State.Ringing -> call.detail ?: stringResource(R.string.island_call_incoming)
                    ActivityContent.Call.State.Active -> {
                        val now by rememberTicking()
                        TimeFormat.clock(now - call.startedAt) + (call.detail?.let { " · $it" } ?: "")
                    }
                    ActivityContent.Call.State.Ended -> stringResource(R.string.island_call_ended)
                }
                Text(subtitle, style = IslandType.bodyDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        val answer = scope.action(CallSource.ACTION_ANSWER, "answer", "accept")
        val decline = scope.action(CallSource.ACTION_DECLINE, "decline", "reject")
        val hangUp = scope.action(CallSource.ACTION_HANG_UP, "hang_up", "hangup", "end")
        val others = scope.activity.actions.filter { it !== answer && it !== decline && it !== hangUp }
        if (answer != null || decline != null || hangUp != null) {
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterHorizontally),
            ) {
                if (call.state == ActivityContent.Call.State.Ringing) {
                    decline?.let { RoundIconButton(Icons.Rounded.CallEnd, it.label, { scope.act(it, null) }, size = 52.dp, container = Accent.danger) }
                    answer?.let { RoundIconButton(Icons.Rounded.Call, it.label, { scope.act(it, null) }, size = 52.dp, container = Accent.call) }
                } else {
                    (hangUp ?: decline)?.let { RoundIconButton(Icons.Rounded.CallEnd, it.label, { scope.act(it, null) }, size = 52.dp, container = Accent.danger) }
                }
            }
        }
        if (others.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            ActionRow(scope, others)
        }
    }
}
