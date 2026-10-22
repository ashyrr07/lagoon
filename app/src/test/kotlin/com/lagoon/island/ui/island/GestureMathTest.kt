package com.lagoon.island.ui.island

import com.lagoon.island.engine.SwipeDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GestureMathTest {
    private val slop = 16f

    @Test
    fun `inside the slop is not a swipe`() {
        assertNull(GestureMath.classifySwipe(0f, 0f, slop))
        assertNull(GestureMath.classifySwipe(10f, -12f, slop))
        assertNull(GestureMath.classifySwipe(-15.9f, 0f, slop))
    }

    @Test
    fun `dominant axis decides the direction`() {
        assertEquals(SwipeDirection.Right, GestureMath.classifySwipe(40f, 10f, slop))
        assertEquals(SwipeDirection.Left, GestureMath.classifySwipe(-40f, 10f, slop))
        assertEquals(SwipeDirection.Down, GestureMath.classifySwipe(10f, 40f, slop))
        assertEquals(SwipeDirection.Up, GestureMath.classifySwipe(10f, -40f, slop))
    }

    @Test
    fun `one axis beyond the slop is enough`() {
        assertEquals(SwipeDirection.Up, GestureMath.classifySwipe(0f, -16f, slop))
        assertEquals(SwipeDirection.Right, GestureMath.classifySwipe(16f, 0f, slop))
    }

    @Test
    fun `ties go horizontal`() {
        assertEquals(SwipeDirection.Left, GestureMath.classifySwipe(-30f, -30f, slop))
    }
}
