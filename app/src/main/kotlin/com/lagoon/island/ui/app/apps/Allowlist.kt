package com.lagoon.island.ui.app.apps

/**
 * The notification allowlist as people see it: an empty set means "all apps" (what
 * [com.lagoon.island.sources.notifications.NotificationRules.shouldShow] already does).
 */
object Allowlist {
    fun isShown(allowlist: Set<String>, packageName: String): Boolean =
        allowlist.isEmpty() || packageName in allowlist

    /** New allowlist after switching [packageName] on or off, given every app in the list. */
    fun toggle(allowlist: Set<String>, allApps: Set<String>, packageName: String, show: Boolean): Set<String> {
        val current = if (allowlist.isEmpty()) allApps else allowlist
        val next = if (show) current + packageName else current - packageName
        return if (next.containsAll(allApps)) emptySet() else next
    }
}
