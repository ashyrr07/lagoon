package com.lagoon.island.sources.system

/**
 * Picks the one short thing worth showing in the compact island for a status notification
 * (an always-on notification without real progress, such as a trading bot's "15 open · uPnL
 * −$21.37" or a VPN's "Connected"). Pure, so it is unit-tested.
 */
object StatusGlance {
    const val MAX_CHARS = 12

    enum class Tone { Positive, Negative, Neutral }

    private val SEPARATORS = Regex("\\s*[·•|]\\s*")
    private const val SIGNS = "+\\-−–"
    private const val CURRENCIES = "$€£¥₽₺₹₸₼₴"

    /** A money amount or a percentage, with an optional sign: "−$21.37", "+4.2%", "€12.00", "12 $". */
    private val VALUE = Regex(
        "[$SIGNS]?[$CURRENCIES]\\s?\\d[\\d,]*(?:\\.\\d+)?" +
            "|[$SIGNS]?\\d[\\d,]*(?:\\.\\d+)?\\s?(?:%|[$CURRENCIES])",
    )

    fun pick(title: String?): String? {
        val t = title?.trim()?.replace(Regex("\\s+"), " ").orEmpty()
        if (t.isEmpty()) return null
        VALUE.findAll(t).lastOrNull()?.let { return it.value.trim() }
        if (t.length <= MAX_CHARS) return t
        val parts = t.split(SEPARATORS).map { it.trim() }.filter { it.isNotEmpty() }
        parts.filter { it.length <= MAX_CHARS }.minByOrNull { it.length }?.let { return it }
        return t.take(MAX_CHARS).trimEnd() + "…"
    }

    fun tone(glance: String?): Tone = when (glance?.trimStart()?.firstOrNull()) {
        '-', '−', '–' -> Tone.Negative
        '+' -> Tone.Positive
        else -> Tone.Neutral
    }
}
