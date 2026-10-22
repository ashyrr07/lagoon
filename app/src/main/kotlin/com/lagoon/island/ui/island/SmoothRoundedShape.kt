package com.lagoon.island.ui.island

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.min

/**
 * Continuous ("squircle") rounded rectangle. Each corner is one cubic Bézier whose handles lie on
 * the two edges, so the curve leaves each edge tangentially; [smoothing] stretches the curve along
 * the edges (0 = a plain circular corner, 1 = the curve starts 2x the radius from the corner and
 * the handles sit at the corner point, which is the iOS-style continuous curve).
 */
class SmoothRoundedShape(private val radiusPx: Float, private val smoothing: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Generic(smoothRoundedPath(Rect(0f, 0f, size.width, size.height), radiusPx, smoothing))
}

/** Circular-arc cubic handle fraction (4/3 * tan(pi/8)). */
private const val ARC_HANDLE = 0.5523f

/** Build the squircle path for [rect] into [into]; the surface and the clip share it. */
fun smoothRoundedPath(rect: Rect, radiusPx: Float, smoothing: Float, into: Path = Path()): Path {
    val path = into
    path.reset()
    val w = rect.width
    val h = rect.height
    if (w <= 0f || h <= 0f) return path
    val r = min(radiusPx, min(w, h) / 2f).coerceAtLeast(0f)
    if (r <= 0f) {
        path.addRect(rect)
        return path
    }
    // A full pill (radius = half the short side) must stay a true semicircle, so smoothing fades
    // out as the radius approaches that limit; a card with a small radius keeps it in full.
    val half = min(w, h) / 2f
    val s = smoothing.coerceIn(0f, 1f) * ((half - r) / r).coerceIn(0f, 1f)
    // Distance along each edge from the corner point at which the curve starts.
    val d = min(r * (1f + s), min(w, h) / 2f)
    // Handle length as a fraction of d: a circle at s = 0, handles on the corner at s = 1.
    val hf = ARC_HANDLE + (1f - ARC_HANDLE) * s
    val hl = d * (1f - hf) // handle distance from the corner along the edge
    val l = rect.left
    val t = rect.top
    val rt = rect.right
    val b = rect.bottom

    path.moveTo(l + d, t)
    path.lineTo(rt - d, t)
    path.cubicTo(rt - hl, t, rt, t + hl, rt, t + d)
    path.lineTo(rt, b - d)
    path.cubicTo(rt, b - hl, rt - hl, b, rt - d, b)
    path.lineTo(l + d, b)
    path.cubicTo(l + hl, b, l, b - hl, l, b - d)
    path.lineTo(l, t + d)
    path.cubicTo(l, t + hl, l + hl, t, l + d, t)
    path.close()
    return path
}
