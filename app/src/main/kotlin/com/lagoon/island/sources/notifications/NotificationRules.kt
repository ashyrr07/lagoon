package com.lagoon.island.sources.notifications

/**
 * Pure decision and text-extraction logic behind [NotificationSource]. No Android types so it
 * runs in plain JVM unit tests; the source maps `StatusBarNotification` into [Facts] and
 * [Extras] first.
 */
object NotificationRules {
    const val FALLBACK_TEXT = "New notification"

    fun activityId(key: String) = "notif:$key"

    /** What the filter needs to know about one posted notification. */
    data class Facts(
        val packageName: String,
        val isGroupSummary: Boolean = false,
        val isOngoing: Boolean = false,
        val isMediaStyle: Boolean = false,
        val isCallStyle: Boolean = false,
    )

    /**
     * Should this notification reach the island?
     *
     * Group summaries, ongoing/foreground-service, media and call notifications are skipped
     * (other sources own those). With an empty [allowlist] every app except the system's own
     * packages shows; otherwise only allowlisted packages do. Our own package never shows.
     */
    fun shouldShow(facts: Facts, allowlist: Set<String>, ownPackage: String): Boolean {
        if (facts.packageName == ownPackage) return false
        if (facts.isGroupSummary || facts.isOngoing || facts.isMediaStyle || facts.isCallStyle) return false
        return if (allowlist.isEmpty()) !isSystemPackage(facts.packageName) else facts.packageName in allowlist
    }

    /** Packages that post plumbing notifications nobody wants mirrored (SystemUI, Settings, ...). */
    fun isSystemPackage(packageName: String): Boolean =
        packageName == "android" ||
            packageName in SYSTEM_PACKAGES ||
            SYSTEM_PREFIXES.any { packageName.startsWith(it) }

    /** The text-bearing extras of a notification, already read out of the Bundle. */
    data class Extras(
        val title: String? = null,
        val text: String? = null,
        val bigText: String? = null,
        val subText: String? = null,
        /** Sender and body of the newest MessagingStyle message, when present. */
        val lastMessageSender: String? = null,
        val lastMessageText: String? = null,
    )

    data class Text(val title: String, val text: String)

    /**
     * Picks a title and a body. The newest message wins for conversations, then big text, then
     * plain text. Android 15 redacts OTP notifications for listeners (empty text), so a notification
     * with nothing readable still shows [FALLBACK_TEXT] under the app name.
     */
    fun resolveText(extras: Extras, appName: String): Text {
        val message = extras.lastMessageText?.clean()
        val sender = extras.lastMessageSender?.clean()
        val title = extras.title?.clean()
        val body = message
            ?: extras.bigText?.clean()
            ?: extras.text?.clean()
            ?: extras.subText?.clean()
        return when {
            message != null && sender != null && title != null && !title.equals(sender, ignoreCase = true) ->
                // Group chat: "Group name" / "Sender: message".
                Text(title, "$sender: $message")
            message != null && sender != null -> Text(sender, message)
            title != null && body != null -> Text(title, body)
            title != null -> Text(appName, title)
            body != null -> Text(appName, body)
            else -> Text(appName, FALLBACK_TEXT)
        }
    }

    private fun String.clean(): String? = trim().replace(WHITESPACE, " ").takeIf { it.isNotEmpty() }

    private val WHITESPACE = Regex("\\s+")
    private val SYSTEM_PACKAGES = setOf(
        "com.google.android.gms",
        "com.google.android.gsf",
        "com.google.android.apps.wellbeing",
        "com.google.android.permissioncontroller",
        "com.google.android.packageinstaller",
        "com.google.android.setupwizard",
    )
    private val SYSTEM_PREFIXES = listOf("com.android.", "com.samsung.android.", "com.sec.android.", "com.miui.")
}
