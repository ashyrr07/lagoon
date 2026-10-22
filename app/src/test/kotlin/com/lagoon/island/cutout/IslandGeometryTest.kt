package com.lagoon.island.cutout

import com.lagoon.island.data.Calibration
import com.lagoon.island.data.Placement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IslandGeometryTest {
    // Pixel-like phone at density 2: 24 dp hole centred in a 36 dp status bar.
    private val hole = PxRect(left = 516, top = 12, right = 564, bottom = 60)
    private val pixel = CutoutInfo(
        cutout = hole,
        shape = CutoutShape.Hole,
        statusBarHeight = 72,
        screenWidth = 1080,
        screenHeight = 2400,
        rotation = 0,
        density = 2f,
    )

    @Test
    fun `standard pill wraps the hole and hangs below the status bar`() {
        val frame = IslandGeometry.idleFrame(pixel, Calibration())
        assertEquals(516 - 28, frame.pill.left)
        assertEquals(564 + 28, frame.pill.right)
        assertEquals(0, frame.pill.top)
        assertEquals(72 + 20, frame.pill.bottom)
        assertEquals(frame.pill.height / 2f, frame.cornerRadius, 0f)
        assertEquals(hole, frame.camera)
    }

    @Test
    fun `hug camera keeps a tight 6dp ring`() {
        val frame = IslandGeometry.idleFrame(pixel, Calibration(hugCamera = true))
        assertEquals(PxRect(516 - 12, 0, 564 + 12, 60 + 12), frame.pill)
    }

    @Test
    fun `offsets move pill and camera together`() {
        val frame = IslandGeometry.idleFrame(pixel, Calibration(offsetX = 5f, offsetY = 2f))
        assertEquals(516 - 28 + 10, frame.pill.left)
        assertEquals(hole.offset(10, 4), frame.camera)
    }

    @Test
    fun `pill never leaves the screen`() {
        val frame = IslandGeometry.idleFrame(pixel, Calibration(offsetX = -400f, offsetY = -50f))
        assertEquals(0, frame.pill.left)
        assertEquals(0, frame.pill.top)
    }

    @Test
    fun `no cutout uses a virtual camera at the chosen placement`() {
        val none = pixel.copy(cutout = null, shape = CutoutShape.None)
        val center = IslandGeometry.idleFrame(none, Calibration())
        assertNull(center.camera)
        assertEquals(540, center.pill.centerX)
        val left = IslandGeometry.idleFrame(none, Calibration(placement = Placement.Left))
        assertTrue(left.pill.centerX < 200)
        val right = IslandGeometry.idleFrame(none, Calibration(placement = Placement.Right))
        assertTrue(right.pill.centerX > 880)
    }

    @Test
    fun `cutout classification`() {
        assertEquals(CutoutShape.Hole, CutoutInfo.classify(PxRect(0, 0, 48, 48), 1080))
        assertEquals(CutoutShape.Pill, CutoutInfo.classify(PxRect(0, 0, 120, 48), 1080))
        assertEquals(CutoutShape.Notch, CutoutInfo.classify(PxRect(0, 0, 500, 80), 1080))
        assertEquals(CutoutShape.None, CutoutInfo.classify(null, 1080))
    }
}
