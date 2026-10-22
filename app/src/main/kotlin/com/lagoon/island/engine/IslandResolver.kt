package com.lagoon.island.engine

import com.lagoon.island.data.IdleMode

/** Output of one resolution: what to show and when the answer might change on its own. */
data class Resolution(
    val presentation: IslandPresentation,
    /** Earliest wall-clock time at which a TTL, alert window or auto-collapse ends; null = never. */
    val nextChangeAt: Long?,
    /** Activities that have expired and should be removed from the registry. */
    val expired: List<String>,
)

/** Pure decision function; see tests. */
object IslandResolver {

    fun resolve(
        activities: Collection<IslandActivity>,
        intent: UserIntent,
        now: Long,
        visible: Boolean,
        idleMode: IdleMode,
    ): Resolution {
        val expired = activities.filter { a -> a.expiresAt()?.let { it <= now } == true }.map { it.id }
        val live = activities
            .filter { it.id !in expired }
            .filter { intent.dismissed[it.id] != it.updatedAt }
            .sortedWith(compareBy<IslandActivity> { it.priority.rank }.thenByDescending { it.updatedAt })

        val deadlines = mutableListOf<Long>()
        activities.forEach { a -> a.expiresAt()?.let { if (it > now) deadlines += it } }

        if (!visible) return Resolution(IslandPresentation.Hidden, deadlines.minOrNull(), expired)

        // 1. Alerts: a fresh system event takes over for its alert window.
        val alerting = live.firstOrNull { a ->
            val until = a.alertUntil() ?: return@firstOrNull false
            until > now && intent.acknowledgedAlerts[a.id] != a.updatedAt
        }
        if (alerting != null) {
            deadlines += alerting.alertUntil()!!
            return Resolution(IslandPresentation.Alert(alerting), deadlines.minOrNull(), expired)
        }

        // 2. Something the user opened.
        val expandedId = intent.expandedId
        val expandedUntil = intent.expandedUntil
        val expandedStillValid = expandedId != null && (expandedUntil == null || expandedUntil > now)
        if (expandedStillValid) {
            expandedUntil?.let { deadlines += it }
            if (expandedId == UserIntent.CONTROL_DECK) {
                return Resolution(IslandPresentation.ControlDeck, deadlines.minOrNull(), expired)
            }
            live.firstOrNull { it.id == expandedId }?.let {
                return Resolution(IslandPresentation.Expanded(it), deadlines.minOrNull(), expired)
            }
        }

        // 3. Compact: primary plus one detached secondary; the rest wait their turn.
        val primary = live.firstOrNull()
        val presentation = if (primary == null) {
            if (idleMode == IdleMode.Never || idleMode == IdleMode.OnlyWithActivity) {
                IslandPresentation.Hidden
            } else {
                IslandPresentation.Idle
            }
        } else {
            IslandPresentation.Compact(primary, live.getOrNull(1))
        }
        return Resolution(presentation, deadlines.minOrNull(), expired)
    }
}
