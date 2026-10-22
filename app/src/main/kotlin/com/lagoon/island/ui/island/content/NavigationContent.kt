package com.lagoon.island.ui.island.content

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lagoon.island.engine.ActivityContent

@Composable
fun NavigationCompactLeading(scope: ContentScope, n: ActivityContent.Navigation) {
    IslandIcon(n.icon, fallback = Icons.Rounded.Navigation, size = 22.dp, tint = scope.accent)
}

@Composable
fun NavigationCompactTrailing(scope: ContentScope, n: ActivityContent.Navigation) {
    Text(n.distance, style = IslandType.compact.copy(color = scope.accent), maxLines = 1)
}

@Composable
fun NavigationExpanded(scope: ContentScope, n: ActivityContent.Navigation) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IslandIcon(n.icon, fallback = Icons.Rounded.Navigation, size = 40.dp, tint = scope.accent)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(n.instruction, style = IslandType.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(n.distance, style = IslandType.body.copy(color = scope.accent), maxLines = 1)
            n.eta?.let { Text(it, style = IslandType.bodyDim, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
    }
}
