package com.lagoon.island.sources.system

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChargingMathTest {
    @Test
    fun `pixel style microamps and millivolts`() {
        // Pixel: +2 100 000 µA while charging, 4 350 mV.
        assertEquals(9.1f, ChargingMath.watts(2_100_000, 4_350)!!, 0.01f)
    }

    @Test
    fun `negative current is charge in on some kernels`() {
        assertEquals(9.1f, ChargingMath.watts(-2_100_000, 4_350)!!, 0.01f)
    }

    @Test
    fun `samsung style milliamps`() {
        // Galaxy: 1 850 mA at 4 120 mV ~ 7.6 W.
        assertEquals(7.6f, ChargingMath.watts(-1_850, 4_120)!!, 0.01f)
    }

    @Test
    fun `voltage in volts or microvolts`() {
        assertEquals(4f, ChargingMath.volts(4)!!, 0.001f)
        assertEquals(4.35f, ChargingMath.volts(4_350_000)!!, 0.001f)
        assertEquals(4.35f, ChargingMath.volts(4_350)!!, 0.001f)
    }

    @Test
    fun `fast charger lands above the fast threshold`() {
        // 6.5 A at 9 V style pack voltage reported in mV: 58.5 W.
        val w = ChargingMath.watts(6_500_000, 9_000)
        assertEquals(58.5f, w!!, 0.01f)
        assertTrue(ChargingMath.isFastCharging(w))
        assertFalse(ChargingMath.isFastCharging(7.6f))
        assertFalse(ChargingMath.isFastCharging(null))
    }

    @Test
    fun `unavailable or zero readings give no wattage`() {
        assertNull(ChargingMath.watts(Int.MIN_VALUE, 4_350))
        assertNull(ChargingMath.watts(0, 4_350))
        assertNull(ChargingMath.watts(1_000_000, 0))
        assertNull(ChargingMath.amperes(Int.MIN_VALUE))
    }

    @Test
    fun `implausible products are rejected`() {
        assertNull(ChargingMath.watts(9_999, 1_000_000_000)) // 10 A at 1000 V
        assertNull(ChargingMath.watts(5, 4_000)) // 5 mA trickle: below 0.1 W
    }

    @Test
    fun `refine prefers the settled second sample unless it collapsed`() {
        assertEquals(27f, ChargingMath.refine(5f, 27f))
        assertEquals(5f, ChargingMath.refine(5f, null))
        assertEquals(27f, ChargingMath.refine(null, 27f))
        assertEquals(27f, ChargingMath.refine(27f, 0.5f))
        assertNull(ChargingMath.refine(null, null))
    }
}
