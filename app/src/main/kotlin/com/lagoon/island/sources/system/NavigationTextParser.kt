package com.lagoon.island.sources.system

data class NavigationInfo(val instruction: String, val distance: String, val eta: String?)

/**
 * Pulls "next turn", "distance to it" and "ETA" out of the title / text / sub-text of Google Maps'
 * and Waze's ongoing navigation notifications. Layouts differ by version and locale, so the parser
 * works on shape, not position: a field that is only a distance is the distance; a field with
 * separators (·) and a clock time or "N min" is the ETA line; whatever remains is the instruction,
 * minus an "In 200 m" / "in 1.2 km" that carries the distance itself.
 */
object NavigationTextParser {
    private const val UNITS = """(?:km|m|mi|ft|yd|meters?|metres?|miles?|feet|kilomet(?:er|re)s?|км|м)"""
    private const val PREPOSITIONS = """(?:in|en|dans|tra|em|à|a|nach|[Чч]ерез)"""

    /** End of a word that also holds after Cyrillic (`\b` is ASCII-only on the JVM). */
    private const val END = """(?![\p{L}\d])"""
    private val DISTANCE = Regex("""\b(\d+(?:[.,]\d+)?)\s?$UNITS$END""", RegexOption.IGNORE_CASE)
    private val LEADING_DISTANCE = Regex("""^\s*$PREPOSITIONS\s+(\d+(?:[.,]\d+)?\s?$UNITS)$END\s*[,:\-–·]?\s*""", RegexOption.IGNORE_CASE)
    private val TRAILING_DISTANCE = Regex("""\s*[,:\-–·]?\s*(?<![\p{L}])$PREPOSITIONS\s+(\d+(?:[.,]\d+)?\s?$UNITS)$END\s*$""", RegexOption.IGNORE_CASE)
    private val CLOCK_TIME = Regex("""\b\d{1,2}:\d{2}\s?(?:[ap]\.?m\.?)?\b""", RegexOption.IGNORE_CASE)
    private val DURATION = Regex("""\b\d+\s?(?:min|mins|minutes?|h|hr|hrs|hours?|std|мин|ч)$END""", RegexOption.IGNORE_CASE)
    private val ARRIVAL_WORDS = listOf("eta", "arriv", "ankunft", "llegada", "прибыт")
    private val SEPARATORS = Regex("""\s*[·•|]\s*""")

    fun parse(title: String?, text: String?, subText: String?): NavigationInfo? {
        val fields = listOf(title, text, subText).map { it?.trim().orEmpty() }.filter { it.isNotEmpty() }
        if (fields.isEmpty()) return null

        val remaining = fields.toMutableList()
        val etaLine = remaining.firstOrNull { isEtaLine(it) }?.also { remaining.remove(it) }
        val distanceField = remaining.firstOrNull { isOnlyDistance(it) }?.also { remaining.remove(it) }

        var instruction = remaining.firstOrNull() ?: ""
        var distance = distanceField ?: ""

        LEADING_DISTANCE.find(instruction)?.let { m ->
            if (distance.isEmpty()) distance = m.groupValues[1].trim()
            instruction = instruction.removeRange(m.range).trim()
        }
        if (distance.isEmpty()) {
            TRAILING_DISTANCE.find(instruction)?.let { m ->
                distance = m.groupValues[1].trim()
                instruction = instruction.removeRange(m.range).trim()
            }
        }
        if (distance.isEmpty() && SEPARATORS.containsMatchIn(instruction)) {
            val parts = SEPARATORS.split(instruction).map { it.trim() }.filter { it.isNotEmpty() }
            parts.firstOrNull { isOnlyDistance(it) }?.let { distPart ->
                distance = distPart
                instruction = parts.filter { it != distPart }.joinToString(" · ")
            }
        }
        if (instruction.isEmpty()) instruction = distance.ifEmpty { fields.first() }
        instruction = instruction.replaceFirstChar { it.uppercaseChar() }

        return NavigationInfo(instruction = instruction, distance = distance, eta = etaLine?.let(::normaliseEta))
    }

    fun isOnlyDistance(s: String): Boolean = DISTANCE.matches(s.trim())

    fun isEtaLine(s: String): Boolean {
        val hasSeparator = SEPARATORS.containsMatchIn(s)
        val hasTime = CLOCK_TIME.containsMatchIn(s) || DURATION.containsMatchIn(s)
        return hasTime && (hasSeparator || DISTANCE.containsMatchIn(s) || ARRIVAL_WORDS.any { s.contains(it, ignoreCase = true) })
    }

    /** "12 min · 4.5 km · 14:05" -> "14:05 · 12 min · 4.5 km": arrival first, as the island shows it. */
    private fun normaliseEta(line: String): String {
        val parts = SEPARATORS.split(line).map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.size <= 1) return line.trim()
        val arrival = parts.firstOrNull { CLOCK_TIME.matches(it) }
        return (listOfNotNull(arrival) + parts.filter { it != arrival }).joinToString(" · ")
    }
}
