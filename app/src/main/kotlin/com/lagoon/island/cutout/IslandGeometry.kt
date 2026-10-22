package com.lagoon.island.cutout

import com.lagoon.island.data.Calibration
import com.lagoon.island.data.Placement
import kotlin.math.roundToInt

/** The idle pill in screen pixels plus the exclusion zone the content must keep clear. */
data class IdleFrame(
    val pill: PxRect,
    /** The camera area, in the same coordinates; null when nothing is reported (content may use the middle). */
    val camera: PxRect?,
    val cornerRadius: Float,
)

/**
 * Pure geometry: detected cutout + user calibration -> idle pill rectangle.
 *
 * Standard mode rule: the pill always reaches at least [TOUCH_BAND_DP] below the status bar so that
 * the lower band can receive touches (SystemUI owns the strip above). "Hug camera" disables that.
 */
object IslandGeometry {
    const val HUG_PAD_DP = 6f
    const val PAD_X_DP = 14f
    const val PAD_Y_DP = 6f
    const val TOUCH_BAND_DP = 10f
    const val VIRTUAL_CAMERA_DP = 24f
    const val EDGE_MARGIN_DP = 16f

    fun idleFrame(info: CutoutInfo, calibration: Calibration): IdleFrame {
        val d = info.density
        fun dp(v: Float) = (v * d).roundToInt()

        val camera = info.cutout ?: virtualCamera(info, calibration, ::dp)
        val padX = dp(calibration.padX ?: if (calibration.hugCamera) HUG_PAD_DP else PAD_X_DP)
        val padY = dp(calibration.padY ?: if (calibration.hugCamera) HUG_PAD_DP else PAD_Y_DP)

        val top = (camera.top - padY).coerceAtLeast(0)
        val naturalBottom = camera.bottom + padY
        val bottom = if (calibration.hugCamera) {
            naturalBottom
        } else {
            maxOf(naturalBottom, info.statusBarHeight + dp(TOUCH_BAND_DP))
        }
        var pill = PxRect(camera.left - padX, top, camera.right + padX, bottom)
            .offset(dp(calibration.offsetX), dp(calibration.offsetY))

        // Keep it on screen horizontally and never above the top edge.
        val overflowLeft = -pill.left
        if (overflowLeft > 0) pill = pill.offset(overflowLeft, 0)
        val overflowRight = pill.right - info.screenWidth
        if (overflowRight > 0) pill = pill.offset(-overflowRight, 0)
        if (pill.top < 0) pill = pill.offset(0, -pill.top)

        val cameraOut = info.cutout?.offset(dp(calibration.offsetX), dp(calibration.offsetY))
        return IdleFrame(pill = pill, camera = cameraOut, cornerRadius = pill.height / 2f)
    }

    private fun virtualCamera(info: CutoutInfo, calibration: Calibration, dp: (Float) -> Int): PxRect {
        val size = dp(VIRTUAL_CAMERA_DP)
        val centerY = info.statusBarHeight / 2
        val centerX = when (calibration.placement) {
            Placement.Center -> info.screenWidth / 2
            Placement.Left -> dp(EDGE_MARGIN_DP) + size
            Placement.Right -> info.screenWidth - dp(EDGE_MARGIN_DP) - size
        }
        return PxRect(centerX - size / 2, centerY - size / 2, centerX + size / 2, centerY + size / 2)
    }
}
