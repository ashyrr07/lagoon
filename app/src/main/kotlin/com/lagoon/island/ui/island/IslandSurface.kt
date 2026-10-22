package com.lagoon.island.ui.island

import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp

/** Colours of the island surface: the fill and an optional glow (album art / Material You). */
data class SurfaceStyle(
    val base: Color = Color.Black,
    val glow: Color? = null,
)

/**
 * Draws the pill (or card) and the detached bubble behind the content, reading every animated
 * value inside the draw lambda so a running animation only redraws.
 *
 * [originOffset] converts the frame's screen coordinates into this node's coordinates.
 */
fun Modifier.islandSurface(
    frame: IslandFrameState,
    style: State<SurfaceStyle>,
    glowAlpha: State<Float>,
    smoothing: Float,
    originOffset: () -> Offset,
): Modifier = drawWithCache {
    val path = Path()
    val shadowPad = 2.dp.toPx()
    val highlight = 1.5.dp.toPx()
    onDrawBehind {
        val origin = originOffset()
        val pill = frame.pill.value.translate(-origin.x, -origin.y)
        val radius = frame.cornerRadius.value
        val s = style.value
        val elevation = frame.elevation.value

        if (elevation > 0.01f) drawSoftShadow(pill, radius, smoothing, elevation, shadowPad, path)

        smoothRoundedPath(pill, radius, smoothing, path)
        drawPath(path, s.base)
        val glow = s.glow
        val ga = glowAlpha.value
        if (glow != null && ga > 0.01f) {
            clipPath(path) {
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(glow.copy(alpha = 0.55f * ga), glow.copy(alpha = 0.18f * ga), Color.Transparent),
                        center = Offset(pill.left + pill.height * 0.6f, pill.top + pill.height * 0.55f),
                        radius = maxOf(pill.width * 0.75f, pill.height),
                    ),
                    topLeft = Offset(pill.left, pill.top),
                    size = pill.size,
                )
            }
        }
        drawTopHighlight(path, pill, highlight)

        val ba = frame.bubbleAlpha.value
        if (ba > 0.01f) {
            val b = frame.bubble.value.translate(-origin.x, -origin.y)
            if (b.width > 0.5f && b.height > 0.5f) {
                val bubblePath = smoothRoundedPath(b, b.height / 2f, 0f, Path())
                drawPath(bubblePath, s.base, alpha = ba)
                drawTopHighlight(bubblePath, b, highlight, ba)
            }
        }
    }
}

/** 1 dp of 8% white along the inside of the top edge. */
private fun DrawScope.drawTopHighlight(path: Path, rect: Rect, heightPx: Float, alpha: Float = 1f) {
    drawPath(
        path,
        Brush.verticalGradient(
            colors = listOf(Color.White.copy(alpha = 0.08f * alpha), Color.Transparent),
            startY = rect.top,
            endY = rect.top + heightPx,
        ),
    )
}

/** Stacked translucent outlines; stays inside the 12 dp window padding (max 10 dp reach). */
private fun DrawScope.drawSoftShadow(
    pill: Rect,
    radius: Float,
    smoothing: Float,
    strength: Float,
    stepPx: Float,
    scratch: Path,
) {
    val layers = floatArrayOf(0.16f, 0.11f, 0.07f, 0.035f)
    layers.forEachIndexed { i, a ->
        val grow = stepPx * (i + 1)
        val r = Rect(pill.left - grow, pill.top - grow * 0.4f, pill.right + grow, pill.bottom + grow * 1.2f)
        smoothRoundedPath(r, radius + grow, smoothing, scratch)
        drawPath(scratch, Color.Black.copy(alpha = a * strength))
    }
}
