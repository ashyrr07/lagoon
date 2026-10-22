package com.lagoon.island.engine

/** What the user is currently asking the island to do, as tracked by the engine. */
data class UserIntent(
    /** Activity the user expanded, or [CONTROL_DECK]. */
    val expandedId: String? = null,
    /** Expanded view auto-collapses at this time (null = stays until tapped outside). */
    val expandedUntil: Long? = null,
    /** id -> updatedAt at the time of dismissal; a newer update shows again. */
    val dismissed: Map<String, Long> = emptyMap(),
    /** Alerts the user already saw (id -> updatedAt), so a collapse does not re-alert. */
    val acknowledgedAlerts: Map<String, Long> = emptyMap(),
) {
    companion object {
        const val CONTROL_DECK = "__control_deck__"
    }
}
