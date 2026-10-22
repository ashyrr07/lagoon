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
        // ring = max(8dp, (72 - 48) / 2) = 16 px; inset 24 px.
        assertEquals(516f - 16f - 60f - 24f, spec.pill.left, 0f)
        assertEquals(564f + 16f + 80f + 24f, spec.pill.right, 0f)
        // Slots sit either side of the camera ring and never cover it.
        assertEquals(516f - 16f, spec.leading.right, 0f)
        assertEquals(564f + 16f, spec.trailing.left, 0f)
        assertTrue(spec.leading.right <= hole.left)
        assertTrue(spec.trailing.left >= hole.right)
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
}
