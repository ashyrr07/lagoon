package com.lagoon.island.ui.island.content

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lagoon.island.engine.ActivityContent

@Composable
fun GenericCompactLeading(scope: ContentScope, g: ActivityContent.Generic) {
    IslandIcon(g.icon, fallback = Icons.Rounded.Info, size = 22.dp, tint = if (g.icon == null) scope.accent else null)
}

@Composable
fun GenericCompactTrailing(scope: ContentScope, g: ActivityContent.Generic) {
    Text(g.title, style = IslandType.compact, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 110.dp))
}

@Composable
fun GenericExpanded(scope: ContentScope, g: ActivityContent.Generic) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IslandIcon(g.icon, fallback = Icons.Rounded.Info, size = 36.dp, tint = if (g.icon == null) scope.accent else null)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(g.title, style = IslandType.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
                g.text?.let { Text(it, style = IslandType.bodyDim, maxLines = 3, overflow = TextOverflow.Ellipsis) }
            }
        }
        if (scope.activity.actions.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            ActionRow(scope, scope.activity.actions)
        }
    }
}
