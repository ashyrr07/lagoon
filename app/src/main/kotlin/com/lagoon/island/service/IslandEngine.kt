package com.lagoon.island.service

import com.lagoon.island.data.GestureAction
import com.lagoon.island.data.IslandSettings
import com.lagoon.island.data.SettingsRepository
import com.lagoon.island.engine.ActivityRegistry
import com.lagoon.island.engine.IslandActivity
import com.lagoon.island.engine.IslandPresentation
import com.lagoon.island.engine.IslandResolver
import com.lagoon.island.engine.Resolution
import com.lagoon.island.engine.SwipeDirection
import com.lagoon.island.engine.UserIntent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Turns registry + user intent + visibility into a [IslandPresentation] stream, re-resolving when
 * a TTL, alert window or auto-collapse deadline passes. Gesture handling lives here so the UI
 * stays a renderer.
 */
@Singleton
class IslandEngine @Inject constructor(
    private val registry: ActivityRegistry,
    private val settingsRepository: SettingsRepository,
) {
    private val intent = MutableStateFlow(UserIntent())
    private val visible = MutableStateFlow(true)
    private val tick = MutableStateFlow(0L)

    private val _presentation = MutableStateFlow<IslandPresentation>(IslandPresentation.Hidden)
    val presentation: StateFlow<IslandPresentation> = _presentation.asStateFlow()

    private var settings = IslandSettings()
    private var deadlineJob: Job? = null
    private var scope: CoroutineScope? = null

    /** Hooks for actions the engine cannot perform itself. Set by the controller. */
    var openApp: (IslandActivity) -> Unit = {}
    var mediaCommand: (IslandActivity, GestureAction) -> Unit = { _, _ -> }
    var toggleTorch: () -> Unit = {}

    fun start(scope: CoroutineScope) {
        this.scope = scope
        scope.launch {
            combine(registry.activities, intent, visible, settingsRepository.settings, tick) { acts, i, v, s, _ ->
                settings = s
                IslandResolver.resolve(acts.values, i, now(), v, s.appearance.idleMode)
            }.collect { apply(it) }
        }
    }

    fun stop() {
        deadlineJob?.cancel()
        scope = null
        _presentation.value = IslandPresentation.Hidden
    }

    fun setVisible(value: Boolean) {
        visible.value = value
    }

    fun now(): Long = System.currentTimeMillis()

    private fun apply(resolution: Resolution) {
        resolution.expired.forEach { registry.remove(it) }
        _presentation.value = resolution.presentation
        deadlineJob?.cancel()
        val at = resolution.nextChangeAt ?: return
        deadlineJob = scope?.launch {
            delay((at - now()).coerceAtLeast(1))
            tick.update { it + 1 }
        }
    }

    // ---- user actions -------------------------------------------------------------------------

    fun tap(activity: IslandActivity?) = when (val p = _presentation.value) {
        is IslandPresentation.Idle -> perform(settings.gestures.idleTap, null)
        is IslandPresentation.Compact -> perform(settings.gestures.compactTap, activity ?: p.primary)
        is IslandPresentation.Alert -> expand(p.activity.id)
        is IslandPresentation.Expanded, IslandPresentation.ControlDeck -> touch()
        IslandPresentation.Hidden -> Unit
    }

    fun longPress(activity: IslandActivity?) = when (val p = _presentation.value) {
        is IslandPresentation.Idle -> perform(settings.gestures.idleLongPress, null)
        is IslandPresentation.Compact -> perform(settings.gestures.compactLongPress, activity ?: p.primary)
        is IslandPresentation.Alert -> expand(p.activity.id)
        else -> Unit
    }

    fun swipe(direction: SwipeDirection, activity: IslandActivity?) {
        val g = settings.gestures
        when (val p = _presentation.value) {
            is IslandPresentation.Compact -> {
                val target = activity ?: p.primary
                when (direction) {
                    SwipeDirection.Left -> perform(g.compactSwipeLeft, target)
                    SwipeDirection.Right -> perform(g.compactSwipeRight, target)
                    SwipeDirection.Up -> perform(g.compactSwipeUp, target)
                    SwipeDirection.Down -> perform(g.compactSwipeDown, target)
                }
            }
            is IslandPresentation.Expanded, IslandPresentation.ControlDeck ->
                if (direction == SwipeDirection.Up) perform(g.expandedSwipeUp, p.activityOrNull) else touch()
            is IslandPresentation.Alert -> if (direction == SwipeDirection.Up) acknowledgeAlert(p.activity) else Unit
            else -> Unit
        }
    }

    fun outsideTouch() {
        when (val p = _presentation.value) {
            is IslandPresentation.Expanded, IslandPresentation.ControlDeck -> collapse()
            is IslandPresentation.Alert -> acknowledgeAlert(p.activity)
            else -> Unit
        }
    }

    fun expand(id: String) {
        val timeout = settings.appearance.expandedTimeoutSec.toLong() * 1000
        intent.update { it.copy(expandedId = id, expandedUntil = if (timeout > 0) now() + timeout else null) }
    }

    fun openControlDeck() = expand(UserIntent.CONTROL_DECK)

    /** Interaction inside an expanded view: push the auto-collapse deadline out again. */
    fun touch() {
        val timeout = settings.appearance.expandedTimeoutSec.toLong() * 1000
        if (timeout > 0) intent.update { if (it.expandedId != null) it.copy(expandedUntil = now() + timeout) else it }
    }

    fun collapse() {
        (_presentation.value as? IslandPresentation.Alert)?.let { acknowledgeAlert(it.activity) }
        intent.update { it.copy(expandedId = null, expandedUntil = null) }
    }

    fun dismiss(activity: IslandActivity) {
        if (!activity.dismissible) return collapse()
        intent.update {
            it.copy(dismissed = it.dismissed + (activity.id to activity.updatedAt), expandedId = null, expandedUntil = null)
        }
    }

    fun acknowledgeAlert(activity: IslandActivity) {
        intent.update { it.copy(acknowledgedAlerts = it.acknowledgedAlerts + (activity.id to activity.updatedAt)) }
    }

    /** Rotate primary/secondary: the current primary is pushed behind by a tiny updatedAt bump of the secondary. */
    private fun nextActivity() {
        val p = _presentation.value as? IslandPresentation.Compact ?: return
        val secondary = p.secondary ?: return
        registry.update(secondary.id) { it.copy(priority = p.primary.priority, updatedAt = now()) }
    }

    private fun perform(action: GestureAction, activity: IslandActivity?) {
        when (action) {
            GestureAction.None -> Unit
            GestureAction.Expand -> activity?.let { expand(it.id) } ?: openControlDeck()
            GestureAction.Collapse -> collapse()
            GestureAction.OpenApp -> activity?.let { a -> a.onTap?.invoke() ?: openApp(a) }
            GestureAction.Dismiss -> activity?.let { dismiss(it) }
            GestureAction.ControlDeck -> openControlDeck()
            GestureAction.NextActivity -> nextActivity()
            GestureAction.PlayPause, GestureAction.NextTrack, GestureAction.PreviousTrack ->
                activity?.let { mediaCommand(it, action) }
            GestureAction.Torch -> toggleTorch()
        }
    }
}
