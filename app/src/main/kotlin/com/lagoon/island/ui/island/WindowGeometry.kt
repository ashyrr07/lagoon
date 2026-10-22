package com.lagoon.island.ui.island

import com.lagoon.island.cutout.PxRect

object WindowGeometry {
    /** Room around the pill for the expanded shadow; drawing is clipped at the window edge. */
    const val SHADOW_PAD_DP = 12f

    fun windowRect(pill: PxRect, padPx: Int, screenWidth: Int): PxRect {
        val left = (pill.left - padPx).coerceAtLeast(0)
        val right = (pill.right + padPx).coerceAtMost(screenWidth)
        val top = 0
        val bottom = pill.bottom + padPx
        return PxRect(left, top, right, bottom)
    }

    /** Convert a screen rect to window-local coordinates. */
    fun toLocal(rect: PxRect, window: PxRect) = rect.offset(-window.left, -window.top)
}
