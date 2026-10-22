package com.lagoon.island.ui.island

import androidx.compose.ui.geometry.Rect
import com.lagoon.island.cutout.IdleFrame
import com.lagoon.island.cutout.PxRect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameMathTest {
    // Pixel-like phone at density 2: 24 dp hole centred in a 36 dp status bar, 1080 px wide.
    private val hole = PxRect(516, 12, 564, 60)
    private val idle = IdleFrame(pill = PxRect(444, 0, 636, 72), camera = hole, cornerRadius = 36f, statusBarBottom = 72)
    private val math = FrameMath(idle, screenWidth = 1080, density = 2f)

    @Test
    fun `idle spec is the idle pill`() {
        val spec = math.idle()
        assertEquals(FrameKind.Idle, spec.kind)
        assertEquals(Rect(444f, 0f, 636f, 72f), spec.pill)
        assertEquals(36f, spec.cornerRadius, 0f)
        assertNull(spec.bubble)
    }

    @Test
    fun `compact pill reaches 14dp below the status bar and wraps both slots`() {
        val spec = math.compact(ContentMeasure(leadingWidth = 60, trailingWidth = 80), hasSecondary = false)
        assertEquals(0f, spec.pill.top, 0f)
        assertEquals(72f + 28f, spec.pill.bottom, 0f)
        assertEquals(50f, spec.cornerRadius, 0f)
        // ring = max(8dp, (72 - 48) / 2) = 16 px; inset 24 px. Both sides take the wider slot (80),
        // so the pill stays centred on the camera.
        assertEquals(516f - 16f - 80f - 24f, spec.pill.left, 0f)
        assertEquals(564f + 16f + 80f + 24f, spec.pill.right, 0f)
        assertEquals(540f, spec.pill.center.x, 0f)
        // Slots sit at the outer edges and never cover the camera ring.
        assertEquals(spec.pill.left + 24f, spec.leading.left, 0f)
        assertEquals(spec.pill.right - 24f, spec.trailing.right, 0f)
        assertTrue(spec.leading.right <= hole.left - 16f)
        assertTrue(spec.trailing.left >= hole.right + 16f)
    }

    @Test
    fun `compact pill falls back to hugging the camera when centring would leave the screen`() {
        // Camera 120 px from the left edge: a centred pill with a 300 px slot cannot fit.
        val nearEdge = IdleFrame(pill = PxRect(24, 0, 216, 72), camera = PxRect(96, 12, 144, 60), cornerRadius = 36f, statusBarBottom = 72)
        val spec = FrameMath(nearEdge, 1080, 2f).compact(ContentMeasure(leadingWidth = 20, trailingWidth = 300), hasSecondary = false)
        assertTrue(spec.pill.left >= 24f)
        assertTrue(spec.leading.right <= 96f)
        assertTrue(spec.trailing.left >= 144f)
    }

    @Test
    fun `compact pill is never narrower than the idle pill`() {
        val spec = math.compact(ContentMeasure(), hasSecondary = false)
        assertEquals(192f, spec.pill.width, 0f)
        assertEquals(540f, spec.pill.center.x, 0f)
    }

    @Test
    fun `compact pill keeps the 12dp screen margins`() {
        val spec = math.compact(ContentMeasure(leadingWidth = 500, trailingWidth = 500), hasSecondary = false)
        assertEquals(24f, spec.pill.left, 0f)
        assertEquals(1080f - 24f, spec.pill.right, 0f)
    }

    @Test
    fun `leading content moves to the trailing side when a corner camera leaves no room`() {
        val corner = IdleFrame(pill = PxRect(24, 0, 216, 72), camera = PxRect(24, 12, 72, 60), cornerRadius = 36f, statusBarBottom = 72)
        val spec = FrameMath(corner, 1080, 2f).compact(ContentMeasure(leadingWidth = 60, trailingWidth = 80), hasSecondary = false)
        assertTrue("leading slot must be right of the camera", spec.leading.left >= 72f)
        assertTrue("trailing comes first", spec.trailing.right <= spec.leading.left)
        assertEquals(24f, spec.pill.left, 0f)
    }

    @Test
    fun `split bubble is an idle-height circle 8dp to the trailing side`() {
        val spec = math.compact(ContentMeasure(leadingWidth = 40, trailingWidth = 40), hasSecondary = true)
        val bubble = spec.bubble
        assertNotNull(bubble)
        bubble!!
        assertEquals(72f, bubble.width, 0f)
        assertEquals(72f, bubble.height, 0f)
        assertEquals(spec.pill.right + 16f, bubble.left, 0f)
        assertEquals(spec.pill.center.y, bubble.center.y, 0f)
        assertEquals(bubble.right, spec.bounds.right, 0f)
    }

    @Test
    fun `split bubble flips to the leading side at the right edge`() {
        val rightEdge = IdleFrame(pill = PxRect(864, 0, 1056, 72), camera = PxRect(1008, 12, 1056, 60), cornerRadius = 36f, statusBarBottom = 72)
        val spec = FrameMath(rightEdge, 1080, 2f).compact(ContentMeasure(leadingWidth = 40, trailingWidth = 0), hasSecondary = true)
        val bubble = spec.bubble!!
        assertTrue(bubble.right <= spec.pill.left)
        assertEquals(spec.pill.left - 16f, bubble.right, 0f)
    }

    @Test
    fun `expanded card spans the screen minus margins and starts content below camera and status bar`() {
        val spec = math.expanded(contentHeight = 300)
        assertEquals(FrameKind.Expanded, spec.kind)
        assertEquals(24f, spec.pill.left, 0f)
        assertEquals(1056f, spec.pill.right, 0f)
        assertEquals(0f, spec.pill.top, 0f)
        assertEquals(88f, spec.cornerRadius, 0f)
        // content top = max(statusBarBottom 72, camera bottom 60) + 8 dp = 88
        assertEquals(88f, spec.content.top, 0f)
        assertEquals(88f + 300f, spec.content.bottom, 0f)
        assertEquals(spec.content.bottom + 32f, spec.pill.bottom, 0f)
        assertEquals(24f + 32f, spec.content.left, 0f)
    }

    @Test
    fun `virtual camera sits in the middle of the idle pill when none is reported`() {
        val none = FrameMath(idle.copy(camera = null), 1080, 2f)
        assertEquals(540f, none.camera.center.x, 0f)
        assertEquals(36f, none.camera.center.y, 0f)
        assertEquals(48f, none.camera.width, 0f)
    }

    @Test
    fun `scale multiplies dp-based sizes`() {
        val big = FrameMath(idle, 1080, 2f, scale = 1.5f)
        assertEquals(72f + 42f, big.compactHeight, 0f)
    }

    @Test
    fun `px conversion rounds outward`() {
        with(FrameMath.Companion) {
            assertEquals(PxRect(1, 2, 4, 6), Rect(1.2f, 2.9f, 3.1f, 5.01f).toPxRect())
        }
    }

    @Test
    fun `rect union and containment`() {
        val a = Rect(0f, 0f, 10f, 10f)
        val b = Rect(5f, 5f, 20f, 12f)
        assertEquals(Rect(0f, 0f, 20f, 12f), FrameSpec.union(a, b))
        with(FrameMath.Companion) {
            assertTrue(FrameSpec.union(a, b).containsRect(b))
            assertTrue(!a.containsRect(b))
        }
    }

    @Test
    fun `long compact content on a wide unfolded screen stays centred on the camera`() {
        // Unfolded tablet-like screen, 2200 px wide, camera (or virtual camera) in the middle.
        val wideHole = PxRect(1076, 12, 1124, 60)
        val wideIdle = IdleFrame(pill = PxRect(1004, 0, 1196, 72), camera = wideHole, cornerRadius = 36f, statusBarBottom = 72)
        val wide = FrameMath(wideIdle, screenWidth = 2200, density = 2f)
        assertEquals(260, wide.maxSlotWidth) // 130 dp cap
        val spec = wide.compact(ContentMeasure(leadingWidth = 48, trailingWidth = wide.maxSlotWidth), hasSecondary = false)
        assertEquals(1100f, spec.pill.center.x, 0.5f)
        assertTrue(spec.pill.width < 2200f / 2)
    }

    @Test
    fun `compact corners follow the idle roundness`() {
        val rounded = FrameMath(idle.copy(cornerRadius = 18f), screenWidth = 1080, density = 2f)
        val spec = rounded.compact(ContentMeasure(leadingWidth = 40, trailingWidth = 40), hasSecondary = false)
        assertEquals(spec.pill.height / 4f, spec.cornerRadius, 0.01f)
    }

    @Test
    fun `tall top-edge cutout gives a round hole at its bottom`() {
        // Samsung-style report: 70 x 104 px from the top edge.
        val tall = IdleFrame(pill = PxRect(498, 0, 750, 120), camera = PxRect(589, 0, 659, 104), cornerRadius = 60f, statusBarBottom = 110)
        val hole = FrameMath(tall, 1248, 2.625f).cameraHole
        assertEquals(Rect(589f, 34f, 659f, 104f), hole)
    }
}
