package com.lagoon.island.ui.app.apps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AllowlistTest {
    private val all = setOf("telegram", "whatsapp", "gmail")

    @Test
    fun `empty list means every app is shown`() {
        assertTrue(Allowlist.isShown(emptySet(), "telegram"))
        assertFalse(Allowlist.isShown(setOf("gmail"), "telegram"))
    }

    @Test
    fun `turning one app off from all keeps the others on`() {
        assertEquals(setOf("whatsapp", "gmail"), Allowlist.toggle(emptySet(), all, "telegram", show = false))
    }

    @Test
    fun `turning the last missing app back on returns to all apps`() {
        assertEquals(emptySet<String>(), Allowlist.toggle(setOf("whatsapp", "gmail"), all, "telegram", show = true))
    }

    @Test
    fun `adding and removing within a custom list`() {
        assertEquals(setOf("gmail", "whatsapp"), Allowlist.toggle(setOf("gmail"), all, "whatsapp", show = true))
        assertEquals(setOf("gmail"), Allowlist.toggle(setOf("gmail", "whatsapp"), all, "whatsapp", show = false))
    }
}
