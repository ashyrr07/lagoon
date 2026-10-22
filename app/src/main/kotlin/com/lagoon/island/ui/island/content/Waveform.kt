package com.lagoon.island.ui.island.content

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.random.Random
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Synthetic 5-bar waveform: while playing each bar springs toward random targets; paused bars
 * settle flat. No audio permission is involved. Heights are read in the draw lambda only.
 */
@Composable
fun Waveform(
    playing: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    bars: Int = 5,
    reduceMotion: Boolean = false,
) {
    val heights = remember(bars) { List(bars) { Animatable(0.22f) } }
    LaunchedEffect(playing, reduceMotion, heights) {
        if (playing && !reduceMotion) {
            heights.forEachIndexed { i, bar ->
                launch {
                    while (isActive) {
                        bar.animateTo(
                            0.25f + Random.nextFloat() * 0.75f,
                            spring(dampingRatio = 0.55f, stiffness = 520f + i * 70f),
                        )
                    }
                }
            }
        } else {
            heights.forEach { bar -> launch { bar.animateTo(if (playing) 0.55f else 0.22f, tween(220)) } }
        }
    }
    Canvas(modifier.size(width = 22.dp, height = 16.dp)) {
        val gap = size.width / (bars * 2 - 1)
        val barW = gap
        heights.forEachIndexed { i, bar ->
            val h = (size.height * bar.value).coerceAtLeast(barW)
            drawRoundRect(
                color = color,
                topLeft = Offset(i * gap * 2, (size.height - h) / 2f),
                size = Size(barW, h),
                cornerRadius = CornerRadius(barW / 2f, barW / 2f),
            )
        }
    }
}
