package com.lagoon.island.service

import android.content.ComponentCallbacks
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.provider.Settings
import android.util.Log
import com.lagoon.island.cutout.CutoutDetector
import com.lagoon.island.cutout.CutoutInfo
import com.lagoon.island.cutout.IdleFrame
import com.lagoon.island.cutout.IslandGeometry
import com.lagoon.island.cutout.PxRect
import com.lagoon.island.data.IslandSettings
import com.lagoon.island.data.SettingsRepository
import com.lagoon.island.engine.IslandPresentation
import com.lagoon.island.overlay.AppOverlayHost
import com.lagoon.island.overlay.IslandWindow
import com.lagoon.island.overlay.OverlayHost
import com.lagoon.island.sources.SourceManager
import com.lagoon.island.sources.media.MediaSource
import com.lagoon.island.ui.app.MainActivity
import com.lagoon.island.ui.island.IslandRoot
import com.lagoon.island.ui.island.IslandUiState
import com.lagoon.island.ui.island.WindowGeometry
import com.lagoon.island.util.IntentLauncher
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Owns the island window for the life of the notification listener binding and wires
 * sources -> engine -> UI state -> window.
 */
@Singleton
class IslandController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val launcher: IntentLauncher,
    private val engine: IslandEngine,
    private val sourceManager: SourceManager,
    private val mediaSource: MediaSource,
    private val enhanced: EnhancedModeManager, // Enhanced mode: accessibility host + keyguard state
) {
    private var scope: CoroutineScope? = null
    private var window: IslandWindow? = null
    private var host: OverlayHost? = null
    private var appHost: AppOverlayHost? = null // Enhanced mode: kept for falling back
    private var detector: CutoutDetector? = null
    private var settingsJob: Job? = null

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    val uiState = IslandUiState()
    private var lastSettings: IslandSettings? = null

    data class State(
        val listenerBound: Boolean = false,
        val showing: Boolean = false,
        val cutout: CutoutInfo? = null,
        val frame: IdleFrame? = null,
    )

    private val configCallbacks = object : ComponentCallbacks {
        override fun onConfigurationChanged(newConfig: Configuration) = refresh()

        @Deprecated("Deprecated in Java")
        override fun onLowMemory() = Unit
    }

    init {
        uiState.onTap = engine::tap
        uiState.onLongPress = engine::longPress
        uiState.onSwipe = engine::swipe
        uiState.onOutsideTouch = engine::outsideTouch
        uiState.onInteraction = engine::touch
        uiState.onCollapse = engine::collapse
        uiState.onAction = { activity, action, text ->
            engine.touch()
            runCatching { action.perform(text) }.onFailure { Log.w(TAG, "Action ${action.id} failed", it) }
            if (!action.acceptsText) engine.collapse()
        }
        uiState.onRequestWindow = ::growWindow
        uiState.onFrameSettled = ::fitWindow
        uiState.onRequestFocus = { focus -> window?.setFocusable(focus) }
        engine.mediaCommand = mediaSource::command
        engine.openApp = { activity ->
            activity.onTap?.invoke() ?: launcher.launch(Intent(context, MainActivity::class.java))
        }
    }

    fun onListenerConnected() {
        _state.value = _state.value.copy(listenerBound = true)
        start()
    }

    fun onListenerDisconnected() {
        _state.value = _state.value.copy(listenerBound = false)
        stop()
    }

    private fun start() {
        if (scope != null) return
        val s = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        scope = s
        engine.start(s)
        sourceManager.startAll()
        s.launch { engine.presentation.collect { onPresentation(it) } }
        settingsJob = s.launch {
            settingsRepository.settings.collectLatest { settings ->
                lastSettings = settings
                apply(settings)
            }
        }
        context.registerComponentCallbacks(configCallbacks)
        // Enhanced mode: re-apply when the accessibility host comes or goes, or the keyguard changes.
        enhanced.startKeyguardWatch(context)
        s.launch { enhanced.host.collect { refresh() } }
        s.launch { enhanced.keyguardLocked.collect { refresh() } }
    }

    private fun stop() {
        sourceManager.stopAll()
        engine.stop()
        runCatching { context.unregisterComponentCallbacks(configCallbacks) }
        enhanced.stopKeyguardWatch(context) // Enhanced mode
        window?.destroy()
        window = null
        host = null
        appHost = null // Enhanced mode
        detector = null
        scope?.cancel()
        scope = null
        settingsJob = null
        _state.value = _state.value.copy(showing = false)
    }

    /** Called by the settings app after permissions change or the user toggles the island. */
    fun refresh() {
        if (scope == null) return
        lastSettings?.let { scope?.launch { apply(it) } }
    }

    private fun apply(settings: IslandSettings) {
        uiState.settings = settings
        uiState.reduceMotion = settings.appearance.reduceMotion || animatorScale() == 0f
        // Enhanced mode: prefer the accessibility host while its service is connected; it needs no
        // overlay permission. Re-host the live window when the host changes (state is kept).
        val host: OverlayHost = enhanced.host.value ?: appHost ?: AppOverlayHost(context).also { appHost = it }
        if (this.host !== host) {
            this.host = host
            detector = null
            window?.rehost(host)
        }
        val needsOverlayPermission = host.kind == OverlayHost.Kind.App && !Settings.canDrawOverlays(context)
        // Enhanced mode: the app overlay draws under the keyguard anyway; the accessibility overlay
        // sits above it, so there the user decides with showOnLockScreen.
        val hiddenByKeyguard = enhanced.keyguardLocked.value &&
            (host.kind == OverlayHost.Kind.App || !settings.enhanced.showOnLockScreen)
        if (!settings.islandEnabled || needsOverlayPermission || hiddenByKeyguard) {
            engine.setVisible(false)
            window?.hide()
            _state.value = _state.value.copy(showing = false)
            return
        }
        val detector = detector ?: CutoutDetector(host.windowContext).also { detector = it }
        val window = window ?: IslandWindow(host, onOutsideTouch = { uiState.onOutsideTouch() }).also {
            it.setContent { IslandRoot(uiState) }
            window = it
        }
        uiState.hostKind = host.kind

        val cutout = detector.detect()
        uiState.cutout = cutout
        if (!cutout.isPortrait && settings.visibility.hideInLandscape) {
            engine.setVisible(false)
            window.hide()
            _state.value = _state.value.copy(showing = false, cutout = cutout)
            return
        }
        val frame = IslandGeometry.idleFrame(cutout, settings.calibration)
        uiState.idle = frame
        engine.setVisible(true)
        _state.value = _state.value.copy(cutout = cutout, frame = frame)
        onPresentation(engine.presentation.value)
        Log.d(TAG, "Island frame=${frame.pill} cutout=${cutout.cutout} shape=${cutout.shape}")
    }

    private fun onPresentation(presentation: IslandPresentation) {
        uiState.presentation = presentation
        val window = window ?: return
        if (presentation is IslandPresentation.Hidden) {
            window.hide()
            _state.value = _state.value.copy(showing = false)
            return
        }
        if (!window.isShowing) {
            val idle = uiState.idle ?: return
            fitWindow(idle.pill)
        }
    }

    /** Grow the window at once so an animation toward a bigger frame is never clipped. */
    private fun growWindow(needed: PxRect) {
        val window = window ?: return
        val cutout = uiState.cutout ?: return
        val pad = (WindowGeometry.SHADOW_PAD_DP * cutout.density).toInt()
        val target = WindowGeometry.windowRect(needed, pad, cutout.screenWidth)
        val current = window.rect
        val union = if (window.isShowing) {
            PxRect(
                minOf(current.left, target.left), 0,
                maxOf(current.right, target.right), maxOf(current.bottom, target.bottom),
            )
        } else {
            target
        }
        show(union)
    }

    /** Shrink the window to exactly the settled frame plus shadow padding. */
    private fun fitWindow(frame: PxRect) {
        val cutout = uiState.cutout ?: return
        val pad = (WindowGeometry.SHADOW_PAD_DP * cutout.density).toInt()
        show(WindowGeometry.windowRect(frame, pad, cutout.screenWidth))
    }

    private fun show(rect: PxRect) {
        val window = window ?: return
        if (window.isShowing && window.rect == rect) return
        uiState.windowRect = rect
        window.show(rect)
        _state.value = _state.value.copy(showing = window.isShowing)
    }

    private fun animatorScale(): Float =
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)

    companion object {
        private const val TAG = "IslandController"
    }
}
