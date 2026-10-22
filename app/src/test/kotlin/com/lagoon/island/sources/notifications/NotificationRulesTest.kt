package com.lagoon.island.sources.notifications

import com.lagoon.island.sources.notifications.NotificationRules.Extras
import com.lagoon.island.sources.notifications.NotificationRules.Facts
import com.lagoon.island.sources.notifications.NotificationRules.Text
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationRulesTest {
    private val own = "com.lagoon.island"
    private val chat = Facts(packageName = "org.thoughtcrime.securesms")

    @Test
    fun `empty allowlist shows any non-system app`() {
        assertTrue(NotificationRules.shouldShow(chat, emptySet(), own))
        assertFalse(NotificationRules.shouldShow(Facts("com.android.systemui"), emptySet(), own))
        assertFalse(NotificationRules.shouldShow(Facts("android"), emptySet(), own))
        assertFalse(NotificationRules.shouldShow(Facts("com.google.android.gms"), emptySet(), own))
        // The adb shell passes only when a debug build asks for it.
        assertFalse(NotificationRules.shouldShow(Facts("com.android.shell"), emptySet(), own))
        assertTrue(NotificationRules.shouldShow(Facts("com.android.shell"), emptySet(), own, allowShellForTesting = true))
        assertFalse(NotificationRules.shouldShow(Facts("com.samsung.android.messaging"), emptySet(), own))
    }

    @Test
    fun `allowlist restricts to listed packages, system ones included`() {
        val allow = setOf("com.android.systemui", "com.whatsapp")
        assertTrue(NotificationRules.shouldShow(Facts("com.whatsapp"), allow, own))
        assertTrue(NotificationRules.shouldShow(Facts("com.android.systemui"), allow, own))
        assertFalse(NotificationRules.shouldShow(chat, allow, own))
    }

    @Test
    fun `our own package never shows`() {
        assertFalse(NotificationRules.shouldShow(Facts(own), emptySet(), own))
        assertFalse(NotificationRules.shouldShow(Facts(own), setOf(own), own))
    }

    @Test
    fun `summaries, ongoing, media and call notifications are left to other sources`() {
        assertFalse(NotificationRules.shouldShow(chat.copy(isGroupSummary = true), emptySet(), own))
        assertFalse(NotificationRules.shouldShow(chat.copy(isOngoing = true), emptySet(), own))
        assertFalse(NotificationRules.shouldShow(chat.copy(isMediaStyle = true), emptySet(), own))
        assertFalse(NotificationRules.shouldShow(chat.copy(isCallStyle = true), emptySet(), own))
    }

    @Test
    fun `plain title and text`() {
        assertEquals(
            Text("Order shipped", "Arrives tomorrow"),
            NotificationRules.resolveText(Extras(title = "Order shipped", text = "Arrives tomorrow"), "Shop"),
        )
    }

    @Test
    fun `big text beats collapsed text`() {
        assertEquals(
            Text("Mail", "The whole message body"),
            NotificationRules.resolveText(Extras(title = "Mail", text = "The whole...", bigText = "The whole message body"), "Gmail"),
        )
    }

    @Test
    fun `newest message wins for a one to one conversation`() {
        val extras = Extras(title = "Ada", text = "old", lastMessageSender = "Ada", lastMessageText = "See you at 9")
        assertEquals(Text("Ada", "See you at 9"), NotificationRules.resolveText(extras, "Signal"))
    }

    @Test
    fun `group conversation keeps the group title and prefixes the sender`() {
        val extras = Extras(title = "Weekend plans", lastMessageSender = "Ada", lastMessageText = "Beach?")
        assertEquals(Text("Weekend plans", "Ada: Beach?"), NotificationRules.resolveText(extras, "Signal"))
    }

    @Test
    fun `title only falls back to app name as title`() {
        assertEquals(Text("Bank", "Code sent"), NotificationRules.resolveText(Extras(title = "Code sent"), "Bank"))
    }

    @Test
    fun `redacted OTP notification still shows something`() {
        assertEquals(Text("Bank", NotificationRules.FALLBACK_TEXT), NotificationRules.resolveText(Extras(), "Bank"))
        assertEquals(
            Text("Bank", NotificationRules.FALLBACK_TEXT),
            NotificationRules.resolveText(Extras(title = "  ", text = "\n"), "Bank"),
        )
    }

    @Test
    fun `whitespace is collapsed`() {
        assertEquals(
            Text("A  b".replace("  ", " "), "line one line two"),
            NotificationRules.resolveText(Extras(title = " A  b ", text = "line one\nline two"), "App"),
        )
    }

    @Test
    fun `activity id is namespaced by key`() {
        assertEquals("notif:0|com.x|1|null|10", NotificationRules.activityId("0|com.x|1|null|10"))
    }
}
