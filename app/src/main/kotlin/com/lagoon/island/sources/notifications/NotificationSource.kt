package com.lagoon.island.sources.notifications

import com.lagoon.island.BuildConfig
import android.app.Notification
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.os.BundleCompat
import com.lagoon.island.data.AppsRepository
import com.lagoon.island.data.FeatureSettings
import com.lagoon.island.data.SettingsRepository
import com.lagoon.island.engine.ActivityContent
import com.lagoon.island.engine.ActivityRegistry
import com.lagoon.island.engine.IconSource
import com.lagoon.island.engine.IslandAction
import com.lagoon.island.engine.IslandActivity
import com.lagoon.island.engine.Priority
import com.lagoon.island.engine.Source
import com.lagoon.island.sources.IslandSource
import com.lagoon.island.sources.NotificationBus
import com.lagoon.island.util.IntentLauncher
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Turns posted notifications into transient `notif:<key>` activities.
 *
 * Filtering lives in [NotificationRules]; this class only reads the `StatusBarNotification`
 * into plain facts, builds the content and wires the PendingIntents. Direct reply: an action with
 * a free-form [RemoteInput] gets `acceptsText = true`; performing it fills the RemoteInput result
 * into a fill-in Intent and sends the action's PendingIntent with it.
 */
@Singleton
class NotificationSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val registry: ActivityRegistry,
    private val bus: NotificationBus,
    private val settingsRepository: SettingsRepository,
    private val launcher: IntentLauncher,
    private val apps: AppsRepository,
) : IslandSource {

    private var features = FeatureSettings()

    /** Keys we cancelled ourselves; the Removed event that follows must not kill the pill. */
    private val selfCancelled = HashSet<String>()

    /** Last shown (title, text, action count, message count) per key, to ignore silent re-posts. */
    private val shown = object : LinkedHashMap<String, Signature>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Signature>?) = size > 200
    }

    private data class Signature(val title: String, val text: String, val actions: Int, val messages: Int)

    override fun start(scope: CoroutineScope) {
        scope.launch {
            settingsRepository.settings.map { it.features }.distinctUntilChanged().collect { f ->
                features = f
                if (!f.notifications) registry.removeWhere { it.source == Source.Notification }
            }
        }
        scope.launch {
            bus.events.collect { event ->
                when (event) {
                    is NotificationBus.Event.Posted -> runCatching { onPosted(event.sbn) }
                    is NotificationBus.Event.Removed -> onRemoved(event.sbn, event.reason)
                }
            }
        }
    }

    override fun stop() {
        registry.removeWhere { it.source == Source.Notification }
        selfCancelled.clear()
        shown.clear()
    }

    private fun onPosted(sbn: StatusBarNotification) {
        val features = features
        if (!features.notifications) return
        val n = sbn.notification
        val facts = NotificationRules.Facts(
            packageName = sbn.packageName,
            isGroupSummary = n.flags and Notification.FLAG_GROUP_SUMMARY != 0,
            isOngoing = sbn.isOngoing || n.flags and Notification.FLAG_FOREGROUND_SERVICE != 0,
            isMediaStyle = isMediaStyle(n),
            isCallStyle = isCallStyle(n),
        )
        if (!NotificationRules.shouldShow(facts, features.notificationApps, context.packageName, BuildConfig.DEBUG)) return

        val appName = apps.labelOf(sbn.packageName)
        val messaging = runCatching { NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(n) }.getOrNull()
        val lastMessage = messaging?.messages?.lastOrNull()
        val extras = NotificationRules.Extras(
            title = n.extras.string(Notification.EXTRA_TITLE),
            text = n.extras.string(Notification.EXTRA_TEXT),
            bigText = n.extras.string(Notification.EXTRA_BIG_TEXT),
            subText = n.extras.string(Notification.EXTRA_SUB_TEXT),
            lastMessageSender = lastMessage?.person?.name?.toString(),
            lastMessageText = lastMessage?.text?.toString(),
        )
        val text = NotificationRules.resolveText(extras, appName)
        val actions = actions(n)

        val key = sbn.key
        val signature = Signature(text.title, text.text, actions.size, messaging?.messages?.size ?: 0)
        if (shown[key] == signature) return // silent update (progress, re-sort): nothing new to show
        shown[key] = signature

        val isConversation = messaging != null || n.category == Notification.CATEGORY_MESSAGE
        val largeIcon: Icon? = BundleCompat.getParcelable(n.extras, EXTRA_CONVERSATION_ICON, Icon::class.java)
            ?: lastMessage?.person?.icon?.let { runCatching { it.toIcon(context) }.getOrNull() }
            ?: n.getLargeIcon()
        val content = ActivityContent.Notification(
            key = key,
            packageName = sbn.packageName,
            appName = appName,
            appIcon = n.smallIcon?.let { IconSource.Platform(it) },
            title = text.title,
            text = text.text,
            largeIcon = largeIcon?.let { IconSource.Platform(it) },
            isConversation = isConversation,
        )
        val id = NotificationRules.activityId(key)
        val now = System.currentTimeMillis()
        registry.post(
            IslandActivity(
                id = id,
                source = Source.Notification,
                priority = Priority.Notification,
                content = content,
                startedAt = now,
                ttlMs = features.notificationSeconds.coerceAtLeast(1) * 1000L,
                tint = n.color.takeIf { it != 0 }?.or(0xFF000000.toInt()),
                actions = actions,
                onTap = { open(sbn) },
            ),
        )
        if (features.dismissSystemNotification) cancel(key)
    }

    private fun onRemoved(sbn: StatusBarNotification, reason: Int) {
        val key = sbn.key
        if (reason == NotificationListenerService.REASON_LISTENER_CANCEL && selfCancelled.remove(key)) return
        selfCancelled.remove(key)
        shown.remove(key)
        registry.remove(NotificationRules.activityId(key))
    }

    private fun open(sbn: StatusBarNotification) {
        val n = sbn.notification
        n.contentIntent?.let { launcher.send(it) }
        if (n.flags and Notification.FLAG_AUTO_CANCEL != 0) cancel(sbn.key)
        registry.remove(NotificationRules.activityId(sbn.key))
    }

    private fun cancel(key: String) {
        selfCancelled += key
        bus.cancel(key)
    }

    private fun actions(n: Notification): List<IslandAction> =
        n.actions.orEmpty().mapIndexed { index, action ->
            val remoteInputs = action.remoteInputs
            val freeForm = remoteInputs.orEmpty().filter { it.allowFreeFormInput }
            IslandAction(
                id = "action:$index",
                label = action.title?.toString()?.trim().orEmpty().ifEmpty { "Action" },
                icon = action.getIcon()?.let { IconSource.Platform(it) },
                acceptsText = freeForm.isNotEmpty(),
            ) { text ->
                val intent = action.actionIntent ?: return@IslandAction
                if (freeForm.isEmpty()) {
                    launcher.send(intent)
                } else {
                    val reply = text?.trim()?.takeIf { it.isNotEmpty() } ?: return@IslandAction
                    val fillIn = Intent()
                    val results = Bundle().apply { freeForm.forEach { putCharSequence(it.resultKey, reply) } }
                    RemoteInput.addResultsToIntent(remoteInputs, fillIn, results)
                    RemoteInput.setResultsSource(fillIn, RemoteInput.SOURCE_FREE_FORM_INPUT)
                    launcher.send(intent, fillIn)
                }
            }
        }

    private fun isMediaStyle(n: Notification): Boolean {
        val template = n.extras.getString(Notification.EXTRA_TEMPLATE).orEmpty()
        return template.endsWith("MediaStyle") ||
            template.endsWith("DecoratedMediaCustomViewStyle") ||
            n.extras.containsKey(Notification.EXTRA_MEDIA_SESSION) ||
            n.category == Notification.CATEGORY_TRANSPORT
    }

    private fun isCallStyle(n: Notification): Boolean =
        n.extras.getString(Notification.EXTRA_TEMPLATE).orEmpty().endsWith("CallStyle") ||
            n.category == Notification.CATEGORY_CALL

    private fun Bundle.string(key: String): String? = getCharSequence(key)?.toString()

    private companion object {
        /** `Notification.EXTRA_CONVERSATION_ICON` (API 30) is not in the public SDK; same key. */
        const val EXTRA_CONVERSATION_ICON = "android.conversationIcon"
    }
}
