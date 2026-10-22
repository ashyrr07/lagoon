package com.lagoon.island.ui.island.content

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lagoon.island.engine.ActivityContent
import com.lagoon.island.engine.IslandAction

@Composable
private fun NotificationAvatar(n: ActivityContent.Notification, size: Dp) {
    val source = n.largeIcon ?: n.appIcon
    Box(Modifier.size(size).clip(CircleShape), contentAlignment = Alignment.Center) {
        IslandIcon(source, fallback = Icons.Rounded.Notifications, size = size)
    }
}

@Composable
fun NotificationCompactLeading(scope: ContentScope, n: ActivityContent.Notification, slotHeight: Dp) {
    NotificationAvatar(n, (slotHeight - 18.dp).coerceAtLeast(20.dp))
}

@Composable
fun NotificationCompactTrailing(scope: ContentScope, n: ActivityContent.Notification) {
    Text(
        n.title.ifBlank { n.appName },
        style = IslandType.compact,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.widthIn(max = 110.dp),
    )
}

@Composable
fun NotificationExpanded(scope: ContentScope, n: ActivityContent.Notification) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NotificationAvatar(n, 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(n.appName, style = IslandType.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(n.title, style = IslandType.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (n.largeIcon != null && n.appIcon != null) {
                Spacer(Modifier.width(8.dp))
                IslandIcon(n.appIcon, fallback = Icons.Rounded.Notifications, size = 18.dp)
            }
        }
        if (n.text.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(n.text, style = IslandType.body, maxLines = 4, overflow = TextOverflow.Ellipsis)
        }
        val reply = scope.activity.actions.firstOrNull { it.acceptsText }
        val buttons = scope.activity.actions.filter { !it.acceptsText }
        if (buttons.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            ActionRow(scope, buttons)
        }
        if (reply != null) {
            Spacer(Modifier.height(10.dp))
            ReplyField(scope, reply)
        }
    }
}

/** Inline reply: asks the window for focus while editing, sends through the action, then lets go. */
@Composable
private fun ReplyField(scope: ContentScope, action: IslandAction) {
    var text by remember(action.id) { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    fun send() {
        val t = text.trim()
        if (t.isEmpty()) return
        scope.act(action, t)
        text = ""
        focusManager.clearFocus()
        scope.requestFocus(false)
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.10f))
            .padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BasicTextField(
            value = text,
            onValueChange = { text = it },
            textStyle = IslandType.body,
            cursorBrush = SolidColor(scope.accent),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { send() }),
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { if (it.isFocused) scope.requestFocus(true) },
            decorationBox = { inner ->
                Box {
                    if (text.isEmpty()) Text(action.label.ifBlank { "Reply" }, style = IslandType.bodyDim)
                    inner()
                }
            },
        )
        RoundIconButton(
            Icons.AutoMirrored.Rounded.Send,
            "Send",
            onClick = ::send,
            size = 32.dp,
            container = if (text.isBlank()) Color.White.copy(alpha = 0.12f) else scope.accent,
            tint = if (text.isBlank()) IslandType.white60 else Color.Black,
            iconSize = 16.dp,
        )
    }
}
