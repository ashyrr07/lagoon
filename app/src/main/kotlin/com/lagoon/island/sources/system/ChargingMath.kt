package com.lagoon.island.sources.system

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Charging wattage from the raw values the platform hands out. Units are not standardised across
 * OEMs: BATTERY_PROPERTY_CURRENT_NOW is µA on most devices but mA on some (Samsung, older HTC), and
 * its sign flips depending on whether the kernel counts charge in or out as positive. EXTRA_VOLTAGE
 * is mV on nearly everything, but a few report V or µV.
 */
object ChargingMath {
    /** Above this magnitude a current reading is taken to be in µA; 10 A in mA is unrealistic. */
    private const val MICROAMP_THRESHOLD = 10_000

    /** Reported by [android.os.BatteryManager.getIntProperty] when the property is unavailable. */
    private const val UNAVAILABLE = Int.MIN_VALUE

    private const val MAX_PLAUSIBLE_WATTS = 300f
    private const val MIN_PLAUSIBLE_WATTS = 0.1f

    /** Absolute current in amperes, unit-normalised, or null when unknown. */
    fun amperes(currentRaw: Int): Float? {
        if (currentRaw == UNAVAILABLE || currentRaw == 0) return null
        val magnitude = abs(currentRaw.toLong())
        return if (magnitude >= MICROAMP_THRESHOLD) magnitude / 1_000_000f else magnitude / 1_000f
    }

    /** Voltage in volts, unit-normalised, or null when unknown. */
    fun volts(voltageRaw: Int): Float? {
        if (voltageRaw <= 0) return null
        return when {
            voltageRaw > 100_000 -> voltageRaw / 1_000_000f // µV
            voltageRaw > 100 -> voltageRaw / 1_000f // mV
            else -> voltageRaw.toFloat() // V
        }
    }

    /** Charging power in watts rounded to one decimal, or null when it cannot be known. */
    fun watts(currentRaw: Int, voltageRaw: Int): Float? {
        val a = amperes(currentRaw) ?: return null
        val v = volts(voltageRaw) ?: return null
        val w = a * v
        if (w < MIN_PLAUSIBLE_WATTS || w > MAX_PLAUSIBLE_WATTS) return null
        return (w * 10f).roundToInt() / 10f
    }

    /** Prefer a later, settled sample; keep the earlier one if the later is missing or tiny. */
    fun refine(first: Float?, second: Float?): Float? = when {
        second == null -> first
        first == null -> second
        second < first * 0.2f -> first
        else -> second
    }

    fun isFastCharging(watts: Float?): Boolean = watts != null && watts > 15f
}
