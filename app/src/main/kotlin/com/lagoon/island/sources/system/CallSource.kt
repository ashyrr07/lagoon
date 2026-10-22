package com.lagoon.island.sources.system

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.app.Person
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.service.notification.StatusBarNotification
import android.telecom.TelecomManager
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.os.BundleCompat
import com.lagoon.island.R
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Phone calls, mirrored from the dialer's notification. `Notification.CallStyle` gives caller,
 * photo and the three intents directly; any other dialer notification is read by title / text and
 * its buttons matched by label and icon. `TelephonyCallback` (READ_PHONE_STATE, optional) only
 * supplies ringing / off-hook / idle. Nothing is read from the call log or contacts.
 */
@Singleton
class CallSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val registry: ActivityRegistry,
    private val bus: NotificationBus,
    private val launcher: IntentLauncher,
    settingsRepository: SettingsRepository,
) : IslandSource {
    private val features = FeatureWatcher(settingsRepository)
    private var scope: CoroutineScope? = null
    private var endJob: Job? = null
    private var telephonyCallback: TelephonyCallback? = null
    private var telephonyState: Int? = null

    private var notificationKey: String? = null
    private var lastName: String? = null
    private var lastPhoto: Bitmap? = null
    private var activeSince: Long? = null

    override fun start(scope: CoroutineScope) {
        this.scope = scope
        // Replays the shade on start and whenever a switch flips, so cards appear and vanish with it.
        features.watch(scope) { bus.activeNotifications().forEach(::onPosted) }
        registerTelephony()
        scope.launch {
            bus.events.collect { event ->
                when (event) {
                    is NotificationBus.Event.Posted -> onPosted(event.sbn)
                    is NotificationBus.Event.Removed -> onRemoved(event.sbn)
                }
            }
        }
    }

    override fun stop() {
        telephonyCallback?.let { cb -> runCatching { context.getSystemService(TelephonyManager::class.java)?.unregisterTelephonyCallback(cb) } }
        telephonyCallback = null
        telephonyState = null
        endJob?.cancel()
        endJob = null
        scope = null
        notificationKey = null
        activeSince = null
        registry.remove(ID)
    }

    private fun registerTelephony() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) return
        val telephony = context.getSystemService(TelephonyManager::class.java) ?: return
        val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
            override fun onCallStateChanged(state: Int) = onTelephonyState(state)
        }
        runCatching { telephony.registerTelephonyCallback(ContextCompat.getMainExecutor(context), callback) }
            .onSuccess { telephonyCallback = callback }
            .onFailure { Log.w(TAG, "Telephony callback unavailable", it) }
    }

    private fun onTelephonyState(state: Int) {
        val previous = telephonyState
        telephonyState = state
        if (!features.current.calls) return
        when (state) {
            TelephonyManager.CALL_STATE_IDLE -> if (previous != null && previous != TelephonyManager.CALL_STATE_IDLE && notificationKey == null) postEnded()
            TelephonyManager.CALL_STATE_RINGING -> if (notificationKey == null) postBare(ActivityContent.Call.State.Ringing, context.getString(R.string.island_call_incoming))
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                if (activeSince == null) activeSince = System.currentTimeMillis()
                if (notificationKey == null) postBare(ActivityContent.Call.State.Active, context.getString(R.string.island_call_in_progress))
                else registry.update(ID) { a -> (a.content as? ActivityContent.Call)?.takeIf { it.state == ActivityContent.Call.State.Ringing }?.let { c -> rebuild(a, c.copy(state = ActivityContent.Call.State.Active, startedAt = activeSince ?: a.startedAt)) } ?: a }
            }
        }
    }

    private fun onPosted(sbn: StatusBarNotification) {
        if (!features.current.calls) return
        if (!isCallNotification(sbn)) return
        val n = sbn.notification
        val extras = n.extras
        val callType = extras?.takeIf { it.containsKey(EXTRA_CALL_TYPE) }?.getInt(EXTRA_CALL_TYPE)
        val person = extras?.let { runCatching { BundleCompat.getParcelable(it, Notification.EXTRA_CALL_PERSON, Person::class.java) }.getOrNull() }

        val name = person?.name?.toString()?.takeIf { it.isNotBlank() } ?: n.title ?: lastName ?: context.getString(R.string.island_call_fallback_name)
        val photo = person?.icon.toBitmapOrNull(context) ?: n.getLargeIcon().toBitmapOrNull(context) ?: lastPhoto
        val detail = n.text?.takeIf { it != name }

        val actions = buildActions(sbn, callType)
        val hasAnswer = actions.any { it.id == ACTION_ANSWER }
        val state = CallStateInference.infer(callType, telephonyState, hasAnswer)
        if (state == ActivityContent.Call.State.Active && activeSince == null) {
            activeSince = if (n.showsChronometer && n.`when` > 0) n.`when` else System.currentTimeMillis()
        }
        if (state == ActivityContent.Call.State.Ringing) activeSince = null

        endJob?.cancel()
        notificationKey = sbn.key
        lastName = name
        lastPhoto = photo
        val now = System.currentTimeMillis()
        val content = ActivityContent.Call(
            name = name,
            detail = detail,
            photo = photo,
            state = state,
            startedAt = activeSince ?: now,
        )
        val existing = registry[ID]
        val sameState = (existing?.content as? ActivityContent.Call)?.state == state
        registry.post(
            IslandActivity(
                id = ID,
                source = Source.Call,
                priority = if (state == ActivityContent.Call.State.Ringing) Priority.Alert else Priority.Call,
                content = content,
                startedAt = existing?.startedAt ?: now,
                updatedAt = if (sameState) existing.updatedAt else now,
                alertMs = if (state == ActivityContent.Call.State.Ringing) RINGING_ALERT_MS else null,
                actions = actions,
                onTap = sbn.openAction(launcher),
                dismissible = state != ActivityContent.Call.State.Ringing,
            ),
        )
    }

    private fun onRemoved(sbn: StatusBarNotification) {
        if (sbn.key != notificationKey) return
        notificationKey = null
        // The dialer re-posts a different notification when ringing turns into an active call;
        // an off-hook line says so outright, and without telephony we wait a beat for the re-post.
        if (telephonyState == TelephonyManager.CALL_STATE_OFFHOOK) return
        endJob?.cancel()
        endJob = scope?.launch {
            delay(REPOST_GRACE_MS)
            if (notificationKey == null) postEnded()
        }
    }

    private fun postEnded() {
        val existing = registry[ID] ?: return
        val content = existing.content as? ActivityContent.Call ?: return
        val now = System.currentTimeMillis()
        activeSince = null
        registry.post(
            existing.copy(
                priority = Priority.Call,
                content = content.copy(state = ActivityContent.Call.State.Ended),
                updatedAt = now,
                alertMs = null,
                ttlMs = ENDED_TTL_MS,
                actions = emptyList(),
                dismissible = true,
            ),
        )
    }

    private fun postBare(state: ActivityContent.Call.State, title: String) {
        val now = System.currentTimeMillis()
        val existing = registry[ID]
        registry.post(
            IslandActivity(
                id = ID,
                source = Source.Call,
                priority = if (state == ActivityContent.Call.State.Ringing) Priority.Alert else Priority.Call,
                content = ActivityContent.Call(lastName ?: title, null, lastPhoto, state, activeSince ?: now),
                startedAt = existing?.startedAt ?: now,
                updatedAt = now,
                alertMs = if (state == ActivityContent.Call.State.Ringing) RINGING_ALERT_MS else null,
                dismissible = state != ActivityContent.Call.State.Ringing,
            ),
        )
    }

    private fun rebuild(activity: IslandActivity, content: ActivityContent.Call): IslandActivity = activity.copy(
        priority = Priority.Call,
        content = content,
        updatedAt = System.currentTimeMillis(),
        alertMs = null,
        dismissible = true,
    )

    /** CallStyle's intents first; otherwise the dialer's own buttons, classified by label and icon. */
    private fun buildActions(sbn: StatusBarNotification, callType: Int?): List<IslandAction> {
        val n = sbn.notification
        val extras = n.extras
        val result = mutableListOf<IslandAction>()
        fun pending(key: String): PendingIntent? = extras?.let { runCatching { BundleCompat.getParcelable(it, key, PendingIntent::class.java) }.getOrNull() }
        if (callType != null) {
            pending(Notification.EXTRA_ANSWER_INTENT)?.let { result += action(ACTION_ANSWER, R.string.island_call_answer, it) }
            pending(Notification.EXTRA_DECLINE_INTENT)?.let { result += action(ACTION_DECLINE, R.string.island_call_decline, it) }
            pending(Notification.EXTRA_HANG_UP_INTENT)?.let { result += action(ACTION_HANG_UP, R.string.island_call_hang_up, it) }
            if (result.isNotEmpty()) return result
        }
        val notificationActions = n.actions.orEmpty()
        val candidates = notificationActions.mapIndexed { index, a ->
            CallActionCandidate(index, a.title?.toString(), iconName(sbn.packageName, a.getIcon()?.takeIf { it.type == android.graphics.drawable.Icon.TYPE_RESOURCE }?.resId))
        }
        val matched = CallActionMatcher.match(candidates)
        matched.answer?.let { i -> notificationActions[i].actionIntent?.let { result += action(ACTION_ANSWER, R.string.island_call_answer, it) } }
        matched.decline?.let { i -> notificationActions[i].actionIntent?.let { result += action(ACTION_DECLINE, R.string.island_call_decline, it) } }
        matched.hangUp?.let { i -> notificationActions[i].actionIntent?.let { result += action(ACTION_HANG_UP, R.string.island_call_hang_up, it) } }
        return result
    }

    private fun action(id: String, @StringRes label: Int, intent: PendingIntent) =
        IslandAction(id, context.getString(label), perform = { launcher.send(intent) })

    private fun iconName(packageName: String, resId: Int?): String? {
        if (resId == null || resId == 0) return null
        return runCatching {
            context.packageManager.getResourcesForApplication(packageName).getResourceEntryName(resId)
        }.getOrNull()
    }

    private fun isCallNotification(sbn: StatusBarNotification): Boolean {
        val n = sbn.notification
        if (n.extras?.containsKey(EXTRA_CALL_TYPE) == true) return true
        if (n.category == Notification.CATEGORY_CALL) return true
        val dialer = runCatching { context.getSystemService(TelecomManager::class.java)?.defaultDialerPackage }.getOrNull()
        return dialer != null && sbn.packageName == dialer && (n.isOngoing || n.fullScreenIntent != null)
    }

    companion object {
        private const val TAG = "CallSource"
        const val ID = "call"
        const val ACTION_ANSWER = "call-answer"
        const val ACTION_DECLINE = "call-decline"
        const val ACTION_HANG_UP = "call-hangup"
        private const val RINGING_ALERT_MS = 120_000L
        private const val ENDED_TTL_MS = 3_000L
        private const val REPOST_GRACE_MS = 800L
    }
}
