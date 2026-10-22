package com.lagoon.island.cutout

/** Integer pixel rectangle that does not depend on android.graphics (so it is unit-testable). */
data class PxRect(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
    val centerX: Int get() = (left + right) / 2
    val centerY: Int get() = (top + bottom) / 2

    fun offset(dx: Int, dy: Int) = PxRect(left + dx, top + dy, right + dx, bottom + dy)
    fun inset(dx: Int, dy: Int) = PxRect(left + dx, top + dy, right - dx, bottom - dy)
}

enum class CutoutShape {
    /** Single round punch hole. */
    Hole,
    /** Wide pill (dual camera or Face-ID style capsule). */
    Pill,
    /** Classic notch touching the top edge over a large part of the width. */
    Notch,
    /** Nothing reported: under-display camera, tablet, or an OEM that hides it. */
    None,
}

/**
 * What the system told us about the display, in the current rotation's pixel coordinates.
 * [cutout] is the bounding box of the cutout on the top edge, or null if none is reported there.
 */
data class CutoutInfo(
    val cutout: PxRect?,
    val shape: CutoutShape,
    val statusBarHeight: Int,
    val screenWidth: Int,
    val screenHeight: Int,
    val rotation: Int,
    val density: Float,
) {
    val isPortrait: Boolean get() = rotation == 0 || rotation == 2

    companion object {
        fun classify(cutout: PxRect?, screenWidth: Int): CutoutShape = when {
            cutout == null || cutout.width <= 0 || cutout.height <= 0 -> CutoutShape.None
            cutout.width > screenWidth * 0.35f -> CutoutShape.Notch
            cutout.width > cutout.height * 1.6f -> CutoutShape.Pill
            else -> CutoutShape.Hole
        }
    }
}
