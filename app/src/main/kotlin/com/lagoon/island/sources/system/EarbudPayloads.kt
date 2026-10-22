package com.lagoon.island.sources.system

import com.lagoon.island.engine.ActivityContent

/** Per-component battery levels of a pair of earbuds, in percent; null = unknown. */
data class PodLevels(
    val left: Int?,
    val right: Int?,
    val case: Int?,
    val leftCharging: Boolean = false,
    val rightCharging: Boolean = false,
    val caseCharging: Boolean = false,
    /** Vendor model id when the payload carries one (AirPods: 0x0E20 = Pro, 0x0F20 = 2nd gen, ...). */
    val model: Int? = null,
) {
    val hasAny: Boolean get() = left != null || right != null || case != null
}

/**
 * Apple "proximity pairing" advertisement: manufacturer data for company 0x004C, type 0x07.
 * Layout (after the company id):
 *  [0] 0x07 type, [1] length, [2] prefix, [3..4] model id, [5] status (bit 0x20 = which pod is primary),
 *  [6] pod levels, one nibble each (0..10 = 0..100 %, 0xF = unknown), [7] high nibble = charging flags
 *  (0x1 / 0x2 pods, 0x4 case), low nibble = case level.
 */
object AirPodsPayload {
    const val APPLE_COMPANY_ID = 0x004C
    const val PROXIMITY_PAIRING = 0x07

    fun parse(data: ByteArray?): PodLevels? {
        if (data == null || data.size < 8) return null
        if (data[0].toInt() and 0xFF != PROXIMITY_PAIRING) return null
        val status = data[5].toInt() and 0xFF
        val flipped = status and 0x20 == 0
        val pods = data[6].toInt() and 0xFF
        val caseByte = data[7].toInt() and 0xFF
        val hi = pods shr 4
        val lo = pods and 0xF
        val charging = caseByte shr 4
        val left = nibbleToPercent(if (flipped) hi else lo)
        val right = nibbleToPercent(if (flipped) lo else hi)
        val case = nibbleToPercent(caseByte and 0xF)
        val leftBit = if (flipped) 0x1 else 0x2
        val rightBit = if (flipped) 0x2 else 0x1
        return PodLevels(
            left = left,
            right = right,
            case = case,
            leftCharging = left != null && charging and leftBit != 0,
            rightCharging = right != null && charging and rightBit != 0,
            caseCharging = case != null && charging and 0x4 != 0,
            model = ((data[3].toInt() and 0xFF) shl 8) or (data[4].toInt() and 0xFF),
        )
    }

    private fun nibbleToPercent(nibble: Int): Int? = if (nibble in 0..10) nibble * 10 else null
}

/**
 * Google Fast Pair non-discoverable advertisement (service data for UUID 0xFE2C): a flags byte, then
 * length/type-headed fields. Type 0b0011 (show UI) or 0b0100 (hide UI) with length 3 carries the
 * battery of left bud, right bud and case; each byte is bit 7 = charging, bits 0..6 = percent
 * (0x7F = unknown).
 */
object FastPairPayload {
    const val SERVICE_UUID = "0000fe2c-0000-1000-8000-00805f9b34fb"
    private const val TYPE_BATTERY_SHOW = 0x3
    private const val TYPE_BATTERY_HIDE = 0x4

    fun parseBattery(serviceData: ByteArray?): PodLevels? {
        if (serviceData == null || serviceData.size < 2) return null
        var i = 1
        while (i < serviceData.size) {
            val header = serviceData[i].toInt() and 0xFF
            val length = header shr 4
            val type = header and 0xF
            val start = i + 1
            val end = start + length
            if (end > serviceData.size) return null
            if ((type == TYPE_BATTERY_SHOW || type == TYPE_BATTERY_HIDE) && length == 3) {
                val l = serviceData[start].toInt() and 0xFF
                val r = serviceData[start + 1].toInt() and 0xFF
                val c = serviceData[start + 2].toInt() and 0xFF
                return PodLevels(
                    left = level(l), right = level(r), case = level(c),
                    leftCharging = level(l) != null && l and 0x80 != 0,
                    rightCharging = level(r) != null && r and 0x80 != 0,
                    caseCharging = level(c) != null && c and 0x80 != 0,
                )
            }
            i = end
        }
        return null
    }

    private fun level(byte: Int): Int? = (byte and 0x7F).takeIf { it in 0..100 }
}

/**
 * Apple's HFP vendor command "+IPHONEACCEV": args are [count, key, value, key, value, ...]; key 1 is
 * the battery level 0..9 meaning 10..100 %, key 2 the dock state.
 */
object IphoneAccev {
    const val COMMAND = "+IPHONEACCEV"

    fun parseBatteryPercent(args: List<Any?>?): Int? {
        if (args.isNullOrEmpty()) return null
        val ints = args.map { (it as? Number)?.toInt() ?: (it as? String)?.trim()?.toIntOrNull() }
        val count = ints[0] ?: return null
        var i = 1
        repeat(count) {
            val key = ints.getOrNull(i) ?: return null
            val value = ints.getOrNull(i + 1) ?: return null
            if (key == 1 && value in 0..9) return (value + 1) * 10
            i += 2
        }
        return null
    }
}

/** Maps a Bluetooth class (major | minor device bits) and name to the island's device kinds. */
object BluetoothKinds {
    private const val MAJOR_MASK = 0x1F00
    private const val MAJOR_AUDIO_VIDEO = 0x0400
    private const val MAJOR_WEARABLE = 0x0700
    private const val AUDIO_VIDEO_WEARABLE_HEADSET = 0x0404
    private const val AUDIO_VIDEO_HANDSFREE = 0x0408
    private const val AUDIO_VIDEO_LOUDSPEAKER = 0x0414
    private const val AUDIO_VIDEO_HEADPHONES = 0x0418
    private const val AUDIO_VIDEO_PORTABLE_AUDIO = 0x041C
    private const val AUDIO_VIDEO_HIFI_AUDIO = 0x0428
    private const val WEARABLE_WRIST_WATCH = 0x0704

    private val EARBUD_WORDS = listOf("buds", "pods", "earbud", "earphone", "dots", "in-ear", "free")
    private val HEADPHONE_WORDS = listOf("headphone", "over-ear", "wh-", "qc", "quietcomfort", "max")
    private val WATCH_WORDS = listOf("watch", "band", "fit")
    private val SPEAKER_WORDS = listOf("speaker", "soundbar", "boom", "flip", "charge", "sound", "echo")

    fun kind(deviceClass: Int?, name: String?): ActivityContent.Bluetooth.Kind {
        val lower = name.orEmpty().lowercase()
        if ("pods" in lower && "max" in lower) return ActivityContent.Bluetooth.Kind.Headphones
        if (EARBUD_WORDS.any { it in lower }) return ActivityContent.Bluetooth.Kind.Earbuds
        if (HEADPHONE_WORDS.any { it in lower }) return ActivityContent.Bluetooth.Kind.Headphones
        if (WATCH_WORDS.any { it in lower }) return ActivityContent.Bluetooth.Kind.Watch
        if (SPEAKER_WORDS.any { it in lower }) return ActivityContent.Bluetooth.Kind.Speaker
        return when (deviceClass) {
            null -> ActivityContent.Bluetooth.Kind.Other
            AUDIO_VIDEO_WEARABLE_HEADSET, AUDIO_VIDEO_HANDSFREE -> ActivityContent.Bluetooth.Kind.Earbuds
            AUDIO_VIDEO_HEADPHONES -> ActivityContent.Bluetooth.Kind.Headphones
            AUDIO_VIDEO_LOUDSPEAKER, AUDIO_VIDEO_PORTABLE_AUDIO, AUDIO_VIDEO_HIFI_AUDIO -> ActivityContent.Bluetooth.Kind.Speaker
            WEARABLE_WRIST_WATCH -> ActivityContent.Bluetooth.Kind.Watch
            else -> when (deviceClass and MAJOR_MASK) {
                MAJOR_WEARABLE -> ActivityContent.Bluetooth.Kind.Watch
                MAJOR_AUDIO_VIDEO -> ActivityContent.Bluetooth.Kind.Headphones
                else -> ActivityContent.Bluetooth.Kind.Other
            }
        }
    }

    fun isAudio(kind: ActivityContent.Bluetooth.Kind): Boolean = when (kind) {
        ActivityContent.Bluetooth.Kind.Earbuds, ActivityContent.Bluetooth.Kind.Headphones,
        ActivityContent.Bluetooth.Kind.Speaker -> true
        else -> false
    }
}
