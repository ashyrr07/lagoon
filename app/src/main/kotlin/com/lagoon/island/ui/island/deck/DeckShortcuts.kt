package com.lagoon.island.ui.island.deck

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.lagoon.island.controls.AppShortcuts

/** A row of app icons; tap launches and collapses the island. */
@Composable
fun ShortcutsRow(
    packages: List<String>,
    shortcuts: AppShortcuts,
    onLaunch: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val view = LocalView.current
    val sizePx = with(density) { DeckStyle.ShortcutSize.roundToPx() }
    val icons: Map<String, ImageBitmap?> = remember(packages) {
        packages.associateWith { pkg ->
            runCatching { shortcuts.icon(pkg)?.toBitmap(sizePx, sizePx)?.asImageBitmap() }.getOrNull()
        }
    }
    Row(
        modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        packages.forEach { pkg ->
            val bitmap = icons[pkg]
            Box(
                Modifier
                    .size(DeckStyle.ShortcutSize)
                    .clip(DeckStyle.PillShape)
                    .background(DeckStyle.Surface)
                    .clickable {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        onLaunch(pkg)
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (bitmap != null) {
                    Image(bitmap, contentDescription = shortcuts.label(pkg) ?: pkg, modifier = Modifier.size(DeckStyle.ShortcutSize))
                } else {
                    Icon(Icons.Rounded.Apps, contentDescription = pkg, tint = DeckStyle.TextMuted)
                }
            }
        }
    }
}
