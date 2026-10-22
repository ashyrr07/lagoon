package com.lagoon.island.sources.system

import android.app.NotificationManager
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.service.notification.StatusBarNotification
import android.util.Log
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Small system indicators: ringer mode and Do Not Disturb changes (transient alerts), the torch
 * (ambient, with a "turn off" action), a screen recording in progress (ambient, with SystemUI's own
 * stop button) and, when enabled, "no internet" while the default network has no validated route.
 */
@Singleton
class SystemStateSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val registry: ActivityRegistry,
    private val bus: NotificationBus,
    private val launcher: IntentLauncher,
    private val settingsRepository: SettingsRepository,
) : IslandSource {
    private val features = FeatureWatcher(settingsRepository)
    private val receivers = Receivers(context)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var cameraManager: CameraManager? = null

    private var ringerMode = -1
    private var interruptionFilter = -1
    private val torchOn = HashMap<String, Boolean>()
    private var torchCallback: CameraManager.TorchCallback? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var recordingKey: String? = null

    override fun start(scope: CoroutineScope) {
        features.watch(scope) {
            bus.activeNotifications().forEach(::onPosted)
            cameraManager?.let(::updateTorch)
        }
        val audio = context.getSystemService(AudioManager::class.java)
        ringerMode = audio?.ringerMode ?: -1
        receivers.register(AudioManager.RINGER_MODE_CHANGED_ACTION) { intent ->
            val mode = intent.getIntExtra(AudioManager.EXTRA_RINGER_MODE, -1)
            if (mode == ringerMode) return@register
            ringerMode = mode
            if (features.current.ringerMode) postRinger(mode)
        }
        val notifications = context.getSystemService(NotificationManager::class.java)
        interruptionFilter = notifications?.currentInterruptionFilter ?: -1
        receivers.register(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED) {
            val filter = notifications?.currentInterruptionFilter ?: return@register
            if (filter == interruptionFilter) return@register
            interruptionFilter = filter
            if (features.current.ringerMode) postDnd(filter != NotificationManager.INTERRUPTION_FILTER_ALL)
        }
        registerTorch()
        scope.launch {
            settingsRepository.settings.map { it.features.noInternet }.distinctUntilChanged().collect { enabled ->
                if (enabled) registerNetwork() else unregisterNetwork()
            }
        }
        scope.launch {
            bus.events.collect { event ->
                when (event) {
                    is NotificationBus.Event.Posted -> onPosted(event.sbn)
                    is NotificationBus.Event.Removed -> if (event.sbn.key == recordingKey) clearRecording()
                }
            }
        }
    }

    override fun stop() {
        receivers.unregisterAll()
        torchCallback?.let { cb -> runCatching { context.getSystemService(CameraManager::class.java)?.unregisterTorchCallback(cb) } }
        torchCallback = null
        cameraManager = null
        torchOn.clear()
        unregisterNetwork()
        clearRecording()
        registry.removeWhere { it.source == Source.System }
    }

    // --- ringer / DND -----------------------------------------------------------------------------

    private fun postRinger(mode: Int) {
        val (title, icon) = when (mode) {
            AudioManager.RINGER_MODE_SILENT -> "Silent" to R.drawable.ic_sys_silent
            AudioManager.RINGER_MODE_VIBRATE -> "Vibrate" to R.drawable.ic_sys_vibrate
            AudioManager.RINGER_MODE_NORMAL -> "Ring" to R.drawable.ic_sys_ring
            else -> return
        }
        postTransient(ID_RINGER, ActivityContent.Generic(IconSource.Res(icon), title, null))
    }

    private fun postDnd(on: Boolean) {
        postTransient(
            ID_DND,
            ActivityContent.Generic(IconSource.Res(R.drawable.ic_sys_dnd), "Do not disturb", if (on) "On" else "Off"),
        )
    }

    private fun postTransient(id: String, content: ActivityContent.Generic) {
        val now = System.currentTimeMillis()
        val alertMs = features.current.alertMs()
        registry.post(
            IslandActivity(
                id = id,
                source = Source.System,
                priority = Priority.Alert,
                content = content,
                startedAt = now,
                alertMs = alertMs,
                ttlMs = alertMs + 1_000,
            ),
        )
    }

    // --- torch ------------------------------------------------------------------------------------

    private fun registerTorch() {
        val camera = context.getSystemService(CameraManager::class.java) ?: return
        val callback = object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                torchOn[cameraId] = enabled
                updateTorch(camera)
            }

            override fun onTorchModeUnavailable(cameraId: String) {
                torchOn.remove(cameraId)
                updateTorch(camera)
            }
        }
        runCatching { camera.registerTorchCallback(callback, mainHandler) }
            .onSuccess {
                torchCallback = callback
                cameraManager = camera
            }
            .onFailure { Log.w(TAG, "Torch callback unavailable", it) }
    }

    private fun updateTorch(camera: CameraManager) {
        val litCameras = torchOn.filterValues { it }.keys
        if (litCameras.isEmpty() || !features.current.torch) {
            registry.remove(ID_TORCH)
            return
        }
        if (registry[ID_TORCH] != null) return
        val now = System.currentTimeMillis()
        registry.post(
            IslandActivity(
                id = ID_TORCH,
                source = Source.System,
                priority = Priority.Ambient,
                content = ActivityContent.Generic(IconSource.Res(R.drawable.ic_sys_torch), "Flashlight on", null),
                startedAt = now,
                actions = listOf(
                    IslandAction(id = "torch-off", label = "Turn off", perform = {
                        litCameras.forEach { id -> runCatching { camera.setTorchMode(id, false) } }
                    }),
                ),
                dismissible = false,
            ),
        )
    }

    /** Camera ids with a flash, for callers that want to toggle the torch themselves. */
    fun flashCameraIds(): List<String> = runCatching {
        val camera = context.getSystemService(CameraManager::class.java) ?: return emptyList()
        camera.cameraIdList.filter { id ->
            camera.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
    }.getOrDefault(emptyList())

    // --- screen recording -------------------------------------------------------------------------

    private fun onPosted(sbn: StatusBarNotification) {
        if (!features.current.screenRecording) return
        if (!isScreenRecording(sbn)) return
        val n = sbn.notification
        recordingKey = sbn.key
        val now = System.currentTimeMillis()
        registry.post(
            IslandActivity(
                id = ID_RECORDING,
                source = Source.System,
                priority = Priority.Ambient,
                content = ActivityContent.Generic(
                    icon = IconSource.Res(R.drawable.ic_sys_record),
                    title = n.title ?: "Screen recording",
                    text = n.text,
                ),
                startedAt = if (n.showsChronometer && n.`when` > 0) n.`when` else now,
                updatedAt = now,
                actions = sbn.islandActions(launcher) { TimerActionLabels.shorten(it) },
                onTap = sbn.openAction(launcher),
                dismissible = false,
            ),
        )
    }

    private fun clearRecording() {
        recordingKey = null
        registry.remove(ID_RECORDING)
    }

    // --- connectivity -----------------------------------------------------------------------------

    private fun registerNetwork() {
        if (networkCallback != null) return
        val connectivity = context.getSystemService(ConnectivityManager::class.java) ?: return
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = Unit

            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                val validated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                mainHandler.post { setNoInternet(!validated) }
            }

            override fun onLost(network: Network) {
                mainHandler.post { setNoInternet(true) }
            }
        }
        runCatching { connectivity.registerDefaultNetworkCallback(callback) }
            .onSuccess { networkCallback = callback }
            .onFailure { Log.w(TAG, "Network callback unavailable (ACCESS_NETWORK_STATE?)", it) }
    }

    private fun unregisterNetwork() {
        networkCallback?.let { cb -> runCatching { context.getSystemService(ConnectivityManager::class.java)?.unregisterNetworkCallback(cb) } }
        networkCallback = null
        registry.remove(ID_NO_INTERNET)
    }

    private fun setNoInternet(noInternet: Boolean) {
        if (!noInternet || networkCallback == null) {
            registry.remove(ID_NO_INTERNET)
            return
        }
        if (registry[ID_NO_INTERNET] != null) return
        registry.post(
            IslandActivity(
                id = ID_NO_INTERNET,
                source = Source.System,
                priority = Priority.Ambient,
                content = ActivityContent.Generic(IconSource.Res(R.drawable.ic_sys_no_internet), "No internet", null),
                startedAt = System.currentTimeMillis(),
            ),
        )
    }

    companion object {
        private const val TAG = "SystemStateSource"
        const val ID_RINGER = "ringer"
        const val ID_DND = "dnd"
        const val ID_TORCH = "torch"
        const val ID_RECORDING = "screen-recording"
        const val ID_NO_INTERNET = "no-internet"

        private val RECORDER_PACKAGES = setOf(
            "com.android.systemui",
            "com.samsung.android.app.smartcapture",
            "com.miui.screenrecorder",
            "com.oplus.screenrecorder",
            "com.coloros.screenrecorder",
            "com.vivo.smartshot",
            "com.huawei.screenrecorder",
        )
        private val RECORDING_WORDS = listOf("recording", "record", "aufnahme", "enregistrement", "grabaci", "grava", "registrazione")

        fun isScreenRecording(sbn: StatusBarNotification): Boolean {
            if (sbn.packageName !in RECORDER_PACKAGES) return false
            val n = sbn.notification
            if (!n.isOngoing) return false
            val words = listOfNotNull(n.title, n.text, n.subText, n.channelId).joinToString(" ").lowercase()
            return RECORDING_WORDS.any { it in words } && (n.showsChronometer || n.actions.orEmpty().isNotEmpty())
        }
    }
}
