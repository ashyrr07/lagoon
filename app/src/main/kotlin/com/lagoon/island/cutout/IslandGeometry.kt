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
    /** Bottom edge of the status bar; content below it is tappable in Standard mode. */
    val statusBarBottom: Int,
)

object IslandGeometryDefaults {
    const val PAD_X_DP = 6f
    const val PAD_Y_DP = 6f
    /** A wide pill reads as "island" even around a small round hole (iPhone proportions). */
    const val IDLE_MIN_WIDTH_DP = 96f
    const val TOUCH_BAND_DP = 10f
    const val VIRTUAL_CAMERA_DP = 24f
    const val EDGE_MARGIN_DP = 12f
}

/**
 * Pure geometry: detected cutout + user calibration -> idle pill rectangle.
 *
 * The pill hugs the cutout vertically (ring of [Calibration.padY]) and is at least
 * [IslandGeometryDefaults.IDLE_MIN_WIDTH_DP] wide, grown symmetrically around the camera where the
 * screen edge allows, otherwise toward the centre. With [Calibration.touchBand] the pill also
 * reaches below the status bar so that the lower band receives touches in Standard mode.
 */
object IslandGeometry {
    const val MIN_ROUNDNESS = 0.35f

    /**
     * @param scale Appearance "Size": scales the ring and the idle width (0.8 to 1.3).
     * @param roundness Appearance "Roundness": corner radius as a fraction of half the height,
     *   from a rounded rectangle (0.35) to a full pill (1).
     */
    fun idleFrame(
        info: CutoutInfo,
        calibration: Calibration,
        scale: Float = 1f,
        roundness: Float = 1f,
    ): IdleFrame {
        val d = info.density
        fun dp(v: Float) = (v * d).roundToInt()

        val camera = info.cutout ?: virtualCamera(info, calibration, ::dp)
        val padX = dp((calibration.padX ?: IslandGeometryDefaults.PAD_X_DP) * scale)
        val padY = dp((calibration.padY ?: IslandGeometryDefaults.PAD_Y_DP) * scale)
        val minWidth = dp((calibration.idleWidth ?: IslandGeometryDefaults.IDLE_MIN_WIDTH_DP) * scale)
        val margin = dp(IslandGeometryDefaults.EDGE_MARGIN_DP)

        val top = (camera.top - padY).coerceAtLeast(0)
        var bottom = camera.bottom + padY
        if (calibration.touchBand) {
            bottom = maxOf(bottom, info.statusBarHeight + dp(IslandGeometryDefaults.TOUCH_BAND_DP))
        }

        // Width: ring around the camera, widened to minWidth symmetrically, then pushed inside the edges.
        var left = camera.left - padX
        var right = camera.right + padX
        val extra = minWidth - (right - left)
        if (extra > 0) {
            left -= extra / 2
            right += extra - extra / 2
        }
        if (left < margin) {
            right += margin - left
            left = margin
        }
        if (right > info.screenWidth - margin) {
            left -= right - (info.screenWidth - margin)
            right = info.screenWidth - margin
            if (left < margin) left = margin
        }

        var pill = PxRect(left, top, right, bottom).offset(dp(calibration.offsetX), dp(calibration.offsetY))
        if (pill.left < 0) pill = pill.offset(-pill.left, 0)
        if (pill.right > info.screenWidth) pill = pill.offset(info.screenWidth - pill.right, 0)
        if (pill.top < 0) pill = pill.offset(0, -pill.top)

        val cameraOut = info.cutout?.offset(dp(calibration.offsetX), dp(calibration.offsetY))
        return IdleFrame(
            pill = pill,
            camera = cameraOut,
            cornerRadius = pill.height / 2f * roundness.coerceIn(MIN_ROUNDNESS, 1f),
            statusBarBottom = info.statusBarHeight,
        )
    }

    private fun virtualCamera(info: CutoutInfo, calibration: Calibration, dp: (Float) -> Int): PxRect {
        val size = dp(IslandGeometryDefaults.VIRTUAL_CAMERA_DP)
        val centerY = info.statusBarHeight / 2
        val centerX = when (calibration.placement) {
            Placement.Center -> info.screenWidth / 2
            Placement.Left -> dp(IslandGeometryDefaults.EDGE_MARGIN_DP) + dp(IslandGeometryDefaults.IDLE_MIN_WIDTH_DP) / 2
            Placement.Right -> info.screenWidth - dp(IslandGeometryDefaults.EDGE_MARGIN_DP) - dp(IslandGeometryDefaults.IDLE_MIN_WIDTH_DP) / 2
        }
        return PxRect(centerX - size / 2, centerY - size / 2, centerX + size / 2, centerY + size / 2)
    }
}
