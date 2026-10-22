package com.lagoon.island.ui.island

import com.lagoon.island.engine.SwipeDirection
import kotlin.math.abs

/** Pure gesture classification, shared by the pill gesture detector and its tests. */
object GestureMath {
    /**
     * A drag of ([dx], [dy]) from the touch-down point is a swipe once it leaves the touch slop;
     * the dominant axis decides the direction. Null means "not a swipe" (tap or long press).
     */
    fun classifySwipe(dx: Float, dy: Float, slop: Float): SwipeDirection? {
        val ax = abs(dx)
        val ay = abs(dy)
        if (ax < slop && ay < slop) return null
        return if (ax >= ay) {
            if (dx > 0) SwipeDirection.Right else SwipeDirection.Left
        } else {
            if (dy > 0) SwipeDirection.Down else SwipeDirection.Up
        }
    }
}
