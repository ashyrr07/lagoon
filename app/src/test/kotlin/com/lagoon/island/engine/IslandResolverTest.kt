package com.lagoon.island.engine

import com.lagoon.island.data.IdleMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IslandResolverTest {
    private val now = 1_000_000L

    private fun activity(
        id: String,
        priority: Priority,
        updatedAt: Long = now - 10,
        ttlMs: Long? = null,
        alertMs: Long? = null,
    ) = IslandActivity(
        id = id,
        source = Source.Demo,
        priority = priority,
        content = ActivityContent.Generic(null, id, null),
        startedAt = updatedAt,
        updatedAt = updatedAt,
        ttlMs = ttlMs,
        alertMs = alertMs,
    )

    private fun resolve(
        vararg activities: IslandActivity,
        intent: UserIntent = UserIntent(),
        visible: Boolean = true,
        idleMode: IdleMode = IdleMode.Always,
    ) = IslandResolver.resolve(activities.toList(), intent, now, visible, idleMode)

    @Test
    fun `nothing live shows idle, or hidden depending on idle mode`() {
        assertEquals(IslandPresentation.Idle, resolve().presentation)
        assertEquals(IslandPresentation.Hidden, resolve(idleMode = IdleMode.Never).presentation)
        assertEquals(IslandPresentation.Hidden, resolve(idleMode = IdleMode.OnlyWithActivity).presentation)
    }

    @Test
    fun `invisible always hides`() {
        val r = resolve(activity("m", Priority.Media), visible = false)
        assertEquals(IslandPresentation.Hidden, r.presentation)
    }

    @Test
    fun `highest priority is primary, next is secondary, third waits`() {
        val media = activity("m", Priority.Media)
        val timer = activity("t", Priority.Timer)
        val note = activity("n", Priority.Notification)
        val r = resolve(media, note, timer).presentation as IslandPresentation.Compact
        assertEquals("t", r.primary.id)
        assertEquals("m", r.secondary?.id)
    }

    @Test
    fun `same priority prefers the most recent`() {
        val old = activity("old", Priority.Notification, updatedAt = now - 500)
        val new = activity("new", Priority.Notification, updatedAt = now - 5)
        val r = resolve(old, new).presentation as IslandPresentation.Compact
        assertEquals("new", r.primary.id)
    }

    @Test
    fun `ttl expiry removes and reports`() {
        val gone = activity("gone", Priority.Notification, updatedAt = now - 10_000, ttlMs = 6_000)
        val live = activity("live", Priority.Notification, updatedAt = now - 1_000, ttlMs = 6_000)
        val r = resolve(gone, live)
        assertEquals(listOf("gone"), r.expired)
        assertEquals("live", (r.presentation as IslandPresentation.Compact).primary.id)
        assertEquals(now - 1_000 + 6_000, r.nextChangeAt)
    }

    @Test
    fun `alert window takes over then ends`() {
        val charge = activity("c", Priority.Alert, updatedAt = now - 1_000, alertMs = 3_000, ttlMs = 3_000)
        val media = activity("m", Priority.Media)
        val r = resolve(charge, media)
        assertEquals("c", (r.presentation as IslandPresentation.Alert).activity.id)
        assertEquals(now + 2_000, r.nextChangeAt)

        val later = IslandResolver.resolve(listOf(charge, media), UserIntent(), now + 2_500, true, IdleMode.Always)
        assertEquals("m", (later.presentation as IslandPresentation.Compact).primary.id)
    }

    @Test
    fun `acknowledged alert does not re-alert`() {
        val charge = activity("c", Priority.Alert, alertMs = 3_000)
        val r = resolve(charge, intent = UserIntent(acknowledgedAlerts = mapOf("c" to charge.updatedAt)))
        assertTrue(r.presentation is IslandPresentation.Compact)
    }

    @Test
    fun `dismissed stays hidden until updated`() {
        val media = activity("m", Priority.Media, updatedAt = 50)
        val dismissed = UserIntent(dismissed = mapOf("m" to 50L))
        assertEquals(IslandPresentation.Idle, resolve(media, intent = dismissed).presentation)
        val updated = media.copy(updatedAt = 60)
        assertTrue(resolve(updated, intent = dismissed).presentation is IslandPresentation.Compact)
    }

    @Test
    fun `expanded shows the chosen activity until its deadline`() {
        val media = activity("m", Priority.Media)
        val timer = activity("t", Priority.Timer)
        val r = resolve(media, timer, intent = UserIntent(expandedId = "m", expandedUntil = now + 5_000))
        assertEquals("m", (r.presentation as IslandPresentation.Expanded).activity.id)
        assertEquals(now + 5_000, r.nextChangeAt)

        val expiredIntent = UserIntent(expandedId = "m", expandedUntil = now - 1)
        assertTrue(resolve(media, timer, intent = expiredIntent).presentation is IslandPresentation.Compact)
    }

    @Test
    fun `expanded id that no longer exists falls back to compact`() {
        val timer = activity("t", Priority.Timer)
        val r = resolve(timer, intent = UserIntent(expandedId = "ghost"))
        assertTrue(r.presentation is IslandPresentation.Compact)
    }

    @Test
    fun `control deck is a presentation of its own`() {
        val r = resolve(intent = UserIntent(expandedId = UserIntent.CONTROL_DECK))
        assertEquals(IslandPresentation.ControlDeck, r.presentation)
        assertNull(r.nextChangeAt)
    }
}
