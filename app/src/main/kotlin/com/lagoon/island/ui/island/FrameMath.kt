package com.lagoon.island.ui.island

import androidx.compose.ui.geometry.Rect
import com.lagoon.island.cutout.IdleFrame
import com.lagoon.island.cutout.PxRect
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

/** Which shape the island takes; the frame math and the content slots key off this. */
enum class FrameKind { Idle, Compact, Alert, Expanded, Deck }

/**
 * Where everything goes for one presentation, in screen pixels (floats so they animate).
 *
 * [pill] is the main surface, [bubble] the detached secondary (Split), [leading]/[trailing] are
 * the compact content slots either side of the camera, [content] is the expanded content area.
 */
data class FrameSpec(
    val kind: FrameKind,
    val pill: Rect,
    val cornerRadius: Float,
    val bubble: Rect? = null,
    val leading: Rect = Rect.Zero,
    val trailing: Rect = Rect.Zero,
    val content: Rect = Rect.Zero,
) {
    /** Everything that must be inside the window. */
    val bounds: Rect get() = bubble?.let { union(pill, it) } ?: pill

    companion object {
        fun union(a: Rect, b: Rect) = Rect(
            minOf(a.left, b.left), minOf(a.top, b.top), maxOf(a.right, b.right), maxOf(a.bottom, b.bottom),
        )
    }
}

/** Measured content sizes (px) the frame math needs; zero until the content has been laid out. */
data class ContentMeasure(
    val leadingWidth: Int = 0,
    val trailingWidth: Int = 0,
    val expandedHeight: Int = 0,
)

/**
 * Pure geometry for every presentation. Everything is in screen pixels around the camera rect;
 * content never covers the camera (when none is reported, the middle of the idle pill stands in).
 */
class FrameMath(
    private val idle: IdleFrame,
    private val screenWidth: Int,
    private val density: Float,
    private val scale: Float = 1f,
) {
    private fun dp(v: Float): Float = v * density * scale

    private val idleRect = idle.pill.toRect()

    /** The camera, or a virtual one in the middle of the idle pill. */
    val camera: Rect = idle.camera?.toRect() ?: run {
        val size = minOf(idleRect.height, dp(24f))
        Rect(
            idleRect.center.x - size / 2, idleRect.center.y - size / 2,
            idleRect.center.x + size / 2, idleRect.center.y + size / 2,
        )
    }

    private val margin = dp(EDGE_MARGIN_DP)

    /** Compact pill height: at least the idle pill, and reaching 14 dp below the status bar. */
    val compactHeight: Float = max(idleRect.bottom, idle.statusBarBottom + dp(COMPACT_BAND_DP)) - idleRect.top

    /**
     * The widest one compact slot may be: never more than [MAX_SLOT_DP] (compact is a glance, like
     * iOS; long text ends in "…" and the full text is in the expanded view), and never so wide that
     * a pill centred on the camera would cross the screen margin. On a wide unfolded screen this
     * keeps a long message centred instead of stretching the pill toward one edge.
     */
    val maxSlotWidth: Int = minOf(
        (screenWidth - camera.width) / 2f - dp(RING_DP) - dp(CONTENT_INSET_DP) - margin,
        dp(MAX_SLOT_DP),
    ).coerceAtLeast(0f).roundToInt()

    /** Corner radius as a fraction of half the height, taken from the idle pill (Appearance "Roundness"). */
    private val roundness: Float = if (idleRect.height > 0f) (idle.cornerRadius / (idleRect.height / 2f)).coerceIn(0f, 1f) else 1f

    fun idle(): FrameSpec = FrameSpec(FrameKind.Idle, idleRect, idle.cornerRadius)

    /**
     * Compact pill: at least as tall as the idle pill and tall enough to leave a tappable band
     * below the status bar. Centred on the camera with both sides as wide as the wider slot
     * (the narrower slot sits at its outer edge), falling back to hugging the camera when a
     * centred pill would cross the 12 dp screen margins.
     */
    fun compact(measure: ContentMeasure, hasSecondary: Boolean, kind: FrameKind = FrameKind.Compact): FrameSpec {
        val top = idleRect.top
        val height = compactHeight
        val bottom = top + height
        val ring = max(dp(RING_DP), (idleRect.height - camera.height) / 2f)
        val inset = dp(CONTENT_INSET_DP)
        val gap = dp(SLOT_GAP_DP)

        val leadingW = measure.leadingWidth.toFloat()
        val trailingW = measure.trailingWidth.toFloat()

        // Leading content goes left of the camera unless the screen edge leaves no room for it.
        val roomLeft = camera.left - ring - inset - margin
        val leadingOnLeft = leadingW <= 0f || leadingW <= roomLeft

        var left: Float
        var right: Float
        val leadingRect: Rect
        val trailingRect: Rect
        // iOS-style balance: both sides as wide as the wider slot, so the pill is centred on the camera.
        val leadingSide = if (leadingW > 0f) leadingW + inset else 0f
        val trailingSide = if (trailingW > 0f) trailingW + inset else 0f
        val side = max(leadingSide, trailingSide)
        val centredLeft = camera.left - ring - side
        val centredRight = camera.right + ring + side
        val centredFits = leadingOnLeft && centredLeft >= margin && centredRight <= screenWidth - margin
        if (centredFits) {
            left = centredLeft
            right = centredRight
            leadingRect = Rect(left + inset, top, left + inset + leadingW, bottom)
            trailingRect = Rect(right - inset - trailingW, top, right - inset, bottom)
        } else if (leadingOnLeft) {
            left = camera.left - ring - (if (leadingW > 0f) leadingW + inset else 0f)
            right = camera.right + ring + (if (trailingW > 0f) trailingW + inset else 0f)
            leadingRect = Rect(camera.left - ring - leadingW, top, camera.left - ring, bottom)
            trailingRect = Rect(camera.right + ring, top, camera.right + ring + trailingW, bottom)
        } else {
            left = camera.left - ring
            val extra = (if (leadingW > 0f) leadingW + gap else 0f) + trailingW
            right = camera.right + ring + (if (extra > 0f) extra + inset else 0f)
            trailingRect = Rect(camera.right + ring, top, camera.right + ring + trailingW, bottom)
            leadingRect = Rect(trailingRect.right + gap, top, trailingRect.right + gap + leadingW, bottom)
        }

        // Never narrower than the idle pill: grow symmetrically around the camera.
        val minWidth = idleRect.width
        val short = minWidth - (right - left)
        if (short > 0f) {
            left -= short / 2
            right += short / 2
        }
        // Keep the edge margins; push the far edge out instead.
        if (left < margin) {
            right += margin - left
            left = margin
        }
        if (right > screenWidth - margin) {
            left -= right - (screenWidth - margin)
            right = screenWidth - margin
            if (left < margin) left = margin
        }

        val pill = Rect(left, top, right, bottom)
        val bubble = if (hasSecondary) bubbleFor(pill, idleRect.height) else null
        return FrameSpec(
            kind = kind,
            pill = pill,
            cornerRadius = height / 2f * roundness,
            bubble = bubble,
            leading = leadingRect,
            trailing = trailingRect,
        )
    }

    /** A circle of idle height, 8 dp to the trailing side of the pill, or leading if that is off-screen. */
    private fun bubbleFor(pill: Rect, diameter: Float): Rect {
        val gap = dp(BUBBLE_GAP_DP)
        val centerY = pill.top + pill.height / 2f
        val trailingLeft = pill.right + gap
        val left = if (trailingLeft + diameter <= screenWidth - margin) trailingLeft else pill.left - gap - diameter
        return Rect(left, centerY - diameter / 2f, left + diameter, centerY + diameter / 2f)
    }

    /**
     * Expanded card: screen width minus margins, top at the pill top, status-bar-tall plain area
     * on top so the system icons stay legible, content below the camera and the status bar.
     */
    fun expanded(contentHeight: Int, kind: FrameKind = FrameKind.Expanded): FrameSpec {
        val top = idleRect.top
        val contentTop = max(idle.statusBarBottom.toFloat(), camera.bottom) + dp(CONTENT_TOP_GAP_DP)
        val inset = dp(CARD_INSET_DP)
        val contentBottom = contentTop + contentHeight
        val bottom = contentBottom + dp(CARD_BOTTOM_PAD_DP)
        val pill = Rect(margin, top, screenWidth - margin, bottom)
        return FrameSpec(
            kind = kind,
            pill = pill,
            cornerRadius = dp(EXPANDED_RADIUS_DP),
            content = Rect(margin + inset, contentTop, screenWidth - margin - inset, contentBottom),
        )
    }

    /** Minimum height the expanded content area needs so the card is never shorter than a compact pill. */
    val minExpandedContentHeight: Int get() = dp(MIN_EXPANDED_CONTENT_DP).roundToInt()

    companion object {
        const val EDGE_MARGIN_DP = 12f
        const val COMPACT_BAND_DP = 14f
        const val RING_DP = 8f
        const val CONTENT_INSET_DP = 12f
        const val SLOT_GAP_DP = 8f
        const val MAX_SLOT_DP = 130f
        const val BUBBLE_GAP_DP = 8f
        const val CONTENT_TOP_GAP_DP = 8f
        const val CARD_INSET_DP = 16f
        const val CARD_BOTTOM_PAD_DP = 16f
        const val EXPANDED_RADIUS_DP = 44f
        const val MIN_EXPANDED_CONTENT_DP = 56f

        fun PxRect.toRect() = Rect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())

        /** Round outward so the window never clips a fractional edge. */
        fun Rect.toPxRect() = PxRect(
            floor(left).toInt(), floor(top).toInt(), ceil(right).toInt(), ceil(bottom).toInt(),
        )

        fun Rect.containsRect(other: Rect): Boolean =
            other.left >= left && other.top >= top && other.right <= right && other.bottom <= bottom
    }
}
