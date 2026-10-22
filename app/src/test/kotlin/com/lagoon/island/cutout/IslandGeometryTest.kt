package com.lagoon.island.cutout

import com.lagoon.island.data.Calibration
import com.lagoon.island.data.Placement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IslandGeometryTest {
    // Pixel-like phone at density 2: 24 dp hole centred in a 36 dp status bar, centred horizontally.
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
    fun `idle pill hugs the hole vertically and is a wide pill centred on it`() {
        val frame = IslandGeometry.idleFrame(pixel, Calibration(touchBand = false))
        assertEquals(PxRect(540 - 96, 0, 540 + 96, 72), frame.pill)
        assertEquals(36f, frame.cornerRadius, 0f)
        assertEquals(hole, frame.camera)
        assertEquals(72, frame.statusBarBottom)
    }

    @Test
    fun `touch band extends below the status bar and is on by default`() {
        val frame = IslandGeometry.idleFrame(pixel, Calibration(touchBand = true))
        assertEquals(72 + 20, frame.pill.bottom)
        assertEquals(frame, IslandGeometry.idleFrame(pixel, Calibration()))
    }

    @Test
    fun `a corner hole grows the pill toward the centre`() {
        val corner = pixel.copy(cutout = PxRect(24, 12, 72, 60))
        val frame = IslandGeometry.idleFrame(corner, Calibration())
        assertEquals(24, frame.pill.left)
        assertEquals(24 + 192, frame.pill.right)
    }

    @Test
    fun `custom idle width and offsets apply`() {
        val frame = IslandGeometry.idleFrame(pixel, Calibration(idleWidth = 60f, offsetX = 5f, offsetY = 2f))
        assertEquals(120, frame.pill.width)
        assertEquals(540 + 10, frame.pill.centerX)
        assertEquals(hole.offset(10, 4), frame.camera)
    }

    @Test
    fun `pill never leaves the screen`() {
        val frame = IslandGeometry.idleFrame(pixel, Calibration(offsetX = -900f, offsetY = -50f))
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

    @Test
    fun `size scales the idle pill and roundness sets the corner radius`() {
        val normal = IslandGeometry.idleFrame(pixel, Calibration(touchBand = false))
        val big = IslandGeometry.idleFrame(pixel, Calibration(touchBand = false), scale = 1.25f)
        assertTrue(big.pill.width > normal.pill.width)
        assertTrue(big.pill.height > normal.pill.height)
        assertEquals(big.pill.centerX, normal.pill.centerX)

        val square = IslandGeometry.idleFrame(pixel, Calibration(touchBand = false), roundness = 0.5f)
        assertEquals(square.pill.height / 4f, square.cornerRadius, 0.01f)
        val tooSquare = IslandGeometry.idleFrame(pixel, Calibration(touchBand = false), roundness = 0f)
        assertEquals(tooSquare.pill.height / 2f * IslandGeometry.MIN_ROUNDNESS, tooSquare.cornerRadius, 0.01f)
    }
}
