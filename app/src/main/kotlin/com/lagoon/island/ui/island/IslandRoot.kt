package com.lagoon.island.ui.island

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset

/** M1: the idle pill only. The frame engine and content arrive in M2. */
@Composable
fun IslandRoot(state: IslandUiState) {
    val idle = state.idle ?: return
    val local = WindowGeometry.toLocal(idle.pill, state.windowRect)
    val density = LocalDensity.current
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .offset { IntOffset(local.left, local.top) }
                .size(
                    width = with(density) { local.width.toDp() },
                    height = with(density) { local.height.toDp() },
                )
                .clip(RoundedCornerShape(50))
                .background(Color.Black)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { state.onTap() },
                ),
        )
    }
}
