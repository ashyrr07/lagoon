package com.lagoon.island.ui.island.deck

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lagoon.island.controls.Control
import com.lagoon.island.controls.ControlKind
import com.lagoon.island.controls.ControlState

/** One squircle tile: icon over a short label, accent-filled while on. */
@Composable
fun DeckTile(
    control: Control,
    state: ControlState,
    onPerform: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val on = state.active && control.kind == ControlKind.Toggle
    val background by animateColorAsState(if (on) DeckStyle.Accent else DeckStyle.Surface, label = "tile")
    val foreground by animateColorAsState(if (on) DeckStyle.OnAccent else DeckStyle.Text, label = "tileFg")
    val icon = if (state.active) control.activeIcon ?: control.icon else control.icon

    Box(
        modifier
            .height(DeckStyle.TileHeight)
            .clip(DeckStyle.TileShape)
            .background(background)
            .then(
                if (state.needsPermission) {
                    Modifier.border(1.dp, DeckStyle.Warn.copy(alpha = 0.6f), DeckStyle.TileShape)
                } else {
                    Modifier
                },
            )
            .alpha(if (state.enabled) 1f else 0.4f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = foreground),
                enabled = state.enabled,
                onClick = {
                    if (control.kind == ControlKind.Toggle) {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    } else {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    }
                    onPerform()
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, contentDescription = null, tint = foreground, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(3.dp))
            Text(
                text = when {
                    state.needsPermission -> "${control.label} · grant"
                    state.detail != null && state.active -> state.detail
                    else -> control.label
                },
                style = DeckStyle.Label.copy(color = foreground),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}
