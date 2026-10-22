package com.lagoon.island.service

import android.content.ComponentCallbacks
import android.content.Context
import android.content.res.Configuration
import android.provider.Settings
import android.util.Log
import com.lagoon.island.cutout.CutoutDetector
import com.lagoon.island.cutout.CutoutInfo
import com.lagoon.island.cutout.IdleFrame
import com.lagoon.island.cutout.IslandGeometry
import com.lagoon.island.data.IslandSettings
import com.lagoon.island.data.SettingsRepository
import com.lagoon.island.overlay.AppOverlayHost
import com.lagoon.island.overlay.IslandWindow
import com.lagoon.island.overlay.OverlayHost
import com.lagoon.island.ui.island.IslandRoot
import com.lagoon.island.ui.island.IslandUiState
import com.lagoon.island.ui.island.WindowGeometry
import com.lagoon.island.util.IntentLauncher
import android.content.Intent
import com.lagoon.island.ui.app.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** Owns the island window for the life of the notification listener binding. */
@Singleton
class IslandController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val launcher: IntentLauncher,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var job: Job? = null
    private var window: IslandWindow? = null
    private var host: OverlayHost? = null
    private var detector: CutoutDetector? = null

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val uiState = IslandUiState().apply {
        // M1 spike: a tap on the idle pill opens Lagoon. Proves background activity start from the overlay.
        onTap = { launcher.launch(Intent(context, MainActivity::class.java)) }
    }
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

    fun onListenerConnected(listener: IslandNotificationListener) {
        _state.value = _state.value.copy(listenerBound = true)
        start()
    }

    fun onListenerDisconnected() {
        _state.value = _state.value.copy(listenerBound = false)
        stop()
    }

    private fun start() {
        if (job != null) return
        job = scope.launch {
            settingsRepository.settings.collectLatest { settings ->
                lastSettings = settings
                apply(settings)
            }
        }
        context.registerComponentCallbacks(configCallbacks)
    }

    private fun stop() {
        job?.cancel()
        job = null
        runCatching { context.unregisterComponentCallbacks(configCallbacks) }
        window?.destroy()
        window = null
        host = null
        detector = null
        _state.value = _state.value.copy(showing = false)
    }

    /** Called by the settings app after permissions change or the user toggles the island. */
    fun refresh() {
        if (job == null) return
        lastSettings?.let { scope.launch { apply(it) } }
    }

    private fun apply(settings: IslandSettings) {
        if (!settings.islandEnabled || !Settings.canDrawOverlays(context)) {
            window?.hide()
            _state.value = _state.value.copy(showing = false)
            return
        }
        val host = host ?: AppOverlayHost(context).also { host = it }
        val detector = detector ?: CutoutDetector(host.windowContext).also { detector = it }
        val window = window ?: IslandWindow(host, onOutsideTouch = { uiState.onOutsideTouch() }).also {
            it.setContent { IslandRoot(uiState) }
            window = it
        }

        val cutout = detector.detect()
        if (!cutout.isPortrait && settings.visibility.hideInLandscape) {
            window.hide()
            _state.value = _state.value.copy(showing = false, cutout = cutout)
            return
        }
        val frame = IslandGeometry.idleFrame(cutout, settings.calibration)
        val padPx = (WindowGeometry.SHADOW_PAD_DP * cutout.density).toInt()
        val windowRect = WindowGeometry.windowRect(frame.pill, padPx, cutout.screenWidth)
        uiState.update(frame, windowRect, cutout, settings)
        window.show(windowRect)
        _state.value = _state.value.copy(showing = window.isShowing, cutout = cutout, frame = frame)
        Log.d(TAG, "Island frame=${frame.pill} window=$windowRect cutout=${cutout.cutout} shape=${cutout.shape}")
    }

    companion object {
        private const val TAG = "IslandController"
    }
}
