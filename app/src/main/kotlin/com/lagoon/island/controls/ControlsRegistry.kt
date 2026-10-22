package com.lagoon.island.controls

import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.FlashlightOff
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.ScreenLockRotation
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Screenshot
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WbSunny
import androidx.core.content.ContextCompat
import com.lagoon.island.util.IntentLauncher
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.roundToInt

/**
 * Every quick control the deck can show, keyed by the ids stored in
 * [com.lagoon.island.data.ControlSettings]. States are refreshed from the system while at least
 * one caller holds [observe]; the deck and the Controls screen do that for their lifetime.
 *
 * Nothing here needs a special permission to *read*; writing brightness/rotation needs
 * WRITE_SETTINGS and DND needs notification-policy access, and in both cases the control opens
 * the system grant screen instead of failing silently.
 */
@Singleton
class ControlsRegistry @Inject constructor(
    @ApplicationContext private val context: Context,
    private val launcher: IntentLauncher,
    private val globalActions: GlobalActions,
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val cameraManager = context.getSystemService(CameraManager::class.java)
    private val notificationManager = context.getSystemService(NotificationManager::class.java)
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val wifiManager = context.applicationContext.getSystemService(WifiManager::class.java)
    private val bluetoothAdapter: BluetoothAdapter? =
        context.getSystemService(BluetoothManager::class.java)?.adapter

    private val torchCameraId: String? = findTorchCamera()
    private val torchMaxStrength: Int = torchMaxStrengthLevel()

    private val torchState = MutableStateFlow(ControlState(enabled = torchCameraId != null))
    private val torchStrengthState = MutableStateFlow(
        ControlState(value = torchMaxStrength.toFloat(), max = torchMaxStrength.toFloat(), enabled = torchMaxStrength > 1),
    )
    private val dndState = MutableStateFlow(ControlState())
    private val rotationState = MutableStateFlow(ControlState())
    private val brightnessState = MutableStateFlow(ControlState(max = 255f))
    private val mediaVolumeState = MutableStateFlow(ControlState(max = 15f))
    private val ringVolumeState = MutableStateFlow(ControlState(max = 7f))
    private val wifiState = MutableStateFlow(ControlState())
    private val bluetoothState = MutableStateFlow(ControlState())
    private val launchState = MutableStateFlow(ControlState())

    private var observers = 0
    private var torchCallback: CameraManager.TorchCallback? = null
    private var contentObserver: ContentObserver? = null
    private var receiver: BroadcastReceiver? = null

    // ---- the controls -----------------------------------------------------------------------

    val torch = Control(
        id = ID_TORCH,
        label = "Torch",
        icon = Icons.Rounded.FlashlightOff,
        activeIcon = Icons.Rounded.FlashlightOn,
        kind = ControlKind.Toggle,
        isAvailable = { torchCameraId != null },
        state = torchState.asStateFlow(),
        perform = { setTorch(!torchState.value.active) },
    )

    val torchStrength = Control(
        id = ID_TORCH_STRENGTH,
        label = "Torch level",
        icon = Icons.Rounded.FlashlightOn,
        kind = ControlKind.Slider,
        isAvailable = { torchCameraId != null && torchMaxStrength > 1 },
        state = torchStrengthState.asStateFlow(),
        perform = { value -> value?.let { setTorchStrength(it.roundToInt()) } },
    )

    val dnd = Control(
        id = ID_DND,
        label = "Do not disturb",
        icon = Icons.Rounded.DoNotDisturbOn,
        kind = ControlKind.Toggle,
        isAvailable = { notificationManager != null },
        state = dndState.asStateFlow(),
        perform = { toggleDnd() },
    )

    val rotation = Control(
        id = ID_ROTATION,
        label = "Rotation lock",
        icon = Icons.Rounded.ScreenRotation,
        activeIcon = Icons.Rounded.ScreenLockRotation,
        kind = ControlKind.Toggle,
        isAvailable = { true },
        state = rotationState.asStateFlow(),
        perform = { toggleRotationLock() },
    )

    val brightness = Control(
        id = ID_BRIGHTNESS,
        label = "Brightness",
        icon = Icons.Rounded.Brightness6,
        activeIcon = Icons.Rounded.WbSunny,
        kind = ControlKind.Slider,
        isAvailable = { true },
        state = brightnessState.asStateFlow(),
        perform = { value -> value?.let { setBrightness(it.roundToInt()) } },
    )

    val mediaVolume = Control(
        id = ID_MEDIA_VOLUME,
        label = "Media volume",
        icon = Icons.AutoMirrored.Rounded.VolumeUp,
        kind = ControlKind.Slider,
        isAvailable = { audioManager != null },
        state = mediaVolumeState.asStateFlow(),
        perform = { value -> value?.let { setVolume(AudioManager.STREAM_MUSIC, it.roundToInt()) } },
    )

    val ringVolume = Control(
        id = ID_RING_VOLUME,
        label = "Ring volume",
        icon = Icons.Rounded.NotificationsActive,
        kind = ControlKind.Slider,
        isAvailable = { audioManager != null },
        state = ringVolumeState.asStateFlow(),
        perform = { value -> value?.let { setVolume(AudioManager.STREAM_RING, it.roundToInt()) } },
    )

    val wifi = Control(
        id = ID_WIFI,
        label = "Internet",
        icon = Icons.Rounded.Wifi,
        kind = ControlKind.Panel,
        isAvailable = { true },
        state = wifiState.asStateFlow(),
        perform = { launcher.launch(Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)) },
    )

    val bluetooth = Control(
        id = ID_BLUETOOTH,
        label = "Bluetooth",
        icon = Icons.Rounded.Bluetooth,
        kind = ControlKind.Panel,
        isAvailable = { context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH) },
        state = bluetoothState.asStateFlow(),
        perform = { launcher.launch(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) },
    )

    val camera = Control(
        id = ID_CAMERA,
        label = "Camera",
        icon = Icons.Rounded.CameraAlt,
        kind = ControlKind.Launch,
        isAvailable = { context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) },
        state = launchState.asStateFlow(),
        perform = { launcher.launch(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)) },
    )

    val screenshot = Control(
        id = ID_SCREENSHOT,
        label = "Screenshot",
        icon = Icons.Rounded.Screenshot,
        kind = ControlKind.Global,
        isAvailable = { globalActions.available.value },
        state = launchState.asStateFlow(),
        perform = { globalActions.screenshot() },
    )

    val lock = Control(
        id = ID_LOCK,
        label = "Lock",
        icon = Icons.Rounded.Lock,
        kind = ControlKind.Global,
        isAvailable = { globalActions.available.value },
        state = launchState.asStateFlow(),
        perform = { globalActions.lock() },
    )

    val recents = Control(
        id = ID_RECENTS,
        label = "Recents",
        icon = Icons.Rounded.ViewCarousel,
        kind = ControlKind.Global,
        isAvailable = { globalActions.available.value },
        state = launchState.asStateFlow(),
        perform = { globalActions.recents() },
    )

    /** Tiles, in the order the Controls screen offers them. */
    val tiles: List<Control> = listOf(torch, dnd, rotation, camera, wifi, bluetooth, screenshot, lock, recents)

    /** Sliders, in the order the Controls screen offers them. */
    val sliders: List<Control> = listOf(brightness, mediaVolume, ringVolume, torchStrength)

    val all: List<Control> = tiles + sliders
    private val byId: Map<String, Control> = all.associateBy { it.id }

    operator fun get(id: String): Control? = byId[id]

    /** Whether Enhanced mode is currently providing the global actions. */
    val globalsAvailable: StateFlow<Boolean> = globalActions.available

    // ---- observing ---------------------------------------------------------------------------

    /**
     * Start refreshing states from the system. Returns a handle; call [Handle.close] when done.
     * Ref-counted, so the deck and the settings screen can overlap.
     */
    fun observe(): Handle {
        mainHandler.post { acquire() }
        return Handle { mainHandler.post { release() } }
    }

    fun interface Handle : AutoCloseable {
        override fun close()
    }

    /** Re-read every state now (cheap; call after returning from a system grant screen). */
    fun refresh() {
        refreshDnd()
        refreshRotation()
        refreshBrightness()
        refreshVolumes()
        refreshWifi()
        refreshBluetooth()
        refreshTorchStrength()
    }

    private fun acquire() {
        observers++
        if (observers > 1) return
        refresh()
        registerTorchCallback()
        registerContentObserver()
        registerReceiver()
    }

    private fun release() {
        observers = (observers - 1).coerceAtLeast(0)
        if (observers > 0) return
        torchCallback?.let { runCatching { cameraManager?.unregisterTorchCallback(it) } }
        torchCallback = null
        contentObserver?.let { runCatching { context.contentResolver.unregisterContentObserver(it) } }
        contentObserver = null
        receiver?.let { runCatching { context.unregisterReceiver(it) } }
        receiver = null
    }

    private fun registerTorchCallback() {
        val manager = cameraManager ?: return
        val callback = object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                if (cameraId != torchCameraId) return
                torchState.update { it.copy(active = enabled, enabled = true) }
            }

            override fun onTorchModeUnavailable(cameraId: String) {
                if (cameraId != torchCameraId) return
                torchState.update { it.copy(active = false, enabled = false) }
            }

            override fun onTorchStrengthLevelChanged(cameraId: String, newStrengthLevel: Int) {
                if (cameraId != torchCameraId) return
                torchStrengthState.update { it.copy(value = newStrengthLevel.toFloat()) }
            }
        }
        runCatching { manager.registerTorchCallback(ContextCompat.getMainExecutor(context), callback) }
            .onSuccess { torchCallback = callback }
            .onFailure { Log.w(TAG, "Torch callback failed", it) }
    }

    private fun registerContentObserver() {
        val observer = object : ContentObserver(mainHandler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                when (uri) {
                    Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION) -> refreshRotation()
                    Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS),
                    Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE) -> refreshBrightness()
                    else -> {
                        // Volume indices live under Settings.System too, but under hidden keys.
                        refreshVolumes()
                        refreshRotation()
                        refreshBrightness()
                    }
                }
            }
        }
        runCatching { context.contentResolver.registerContentObserver(Settings.System.CONTENT_URI, true, observer) }
            .onSuccess { contentObserver = observer }
            .onFailure { Log.w(TAG, "Settings observer failed", it) }
    }

    private fun registerReceiver() {
        val r = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                when (intent.action) {
                    NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED,
                    NotificationManager.ACTION_NOTIFICATION_POLICY_ACCESS_GRANTED_CHANGED -> refreshDnd()
                    WifiManager.WIFI_STATE_CHANGED_ACTION -> refreshWifi()
                    BluetoothAdapter.ACTION_STATE_CHANGED -> refreshBluetooth()
                    ACTION_VOLUME_CHANGED, AudioManager.RINGER_MODE_CHANGED_ACTION -> refreshVolumes()
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
            addAction(NotificationManager.ACTION_NOTIFICATION_POLICY_ACCESS_GRANTED_CHANGED)
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(ACTION_VOLUME_CHANGED)
            addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
        }
        runCatching { ContextCompat.registerReceiver(context, r, filter, ContextCompat.RECEIVER_EXPORTED) }
            .onSuccess { receiver = r }
            .onFailure { Log.w(TAG, "Receiver failed", it) }
    }

    // ---- torch -------------------------------------------------------------------------------

    private fun findTorchCamera(): String? {
        val manager = cameraManager ?: return null
        return runCatching {
            val ids = manager.cameraIdList
            ids.firstOrNull { id ->
                val c = manager.getCameraCharacteristics(id)
                c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true &&
                    c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
            } ?: ids.firstOrNull { id ->
                manager.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        }.getOrNull()
    }

    private fun torchMaxStrengthLevel(): Int {
        if (Build.VERSION.SDK_INT < 33) return 1
        val id = torchCameraId ?: return 1
        return runCatching {
            cameraManager?.getCameraCharacteristics(id)?.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL)
        }.getOrNull() ?: 1
    }

    private fun refreshTorchStrength() {
        if (Build.VERSION.SDK_INT < 33 || torchMaxStrength <= 1) return
        val id = torchCameraId ?: return
        runCatching { cameraManager?.getTorchStrengthLevel(id) }.getOrNull()?.let { level ->
            torchStrengthState.update { it.copy(value = level.toFloat()) }
        }
    }

    private fun setTorch(on: Boolean) {
        val id = torchCameraId ?: return
        runCatching { cameraManager?.setTorchMode(id, on) }
            .onSuccess { torchState.update { it.copy(active = on) } }
            .onFailure { Log.w(TAG, "setTorchMode failed", it) }
    }

    private fun setTorchStrength(level: Int) {
        if (Build.VERSION.SDK_INT < 33) return
        val id = torchCameraId ?: return
        val clamped = level.coerceIn(1, torchMaxStrength)
        runCatching { cameraManager?.turnOnTorchWithStrengthLevel(id, clamped) }
            .onSuccess {
                torchStrengthState.update { it.copy(value = clamped.toFloat()) }
                torchState.update { it.copy(active = true) }
            }
            .onFailure { Log.w(TAG, "turnOnTorchWithStrengthLevel failed", it) }
    }

    // ---- do not disturb ----------------------------------------------------------------------

    private fun refreshDnd() {
        val nm = notificationManager ?: return
        val granted = nm.isNotificationPolicyAccessGranted
        val filter = runCatching { nm.currentInterruptionFilter }.getOrDefault(NotificationManager.INTERRUPTION_FILTER_UNKNOWN)
        val on = filter != NotificationManager.INTERRUPTION_FILTER_ALL && filter != NotificationManager.INTERRUPTION_FILTER_UNKNOWN
        dndState.value = ControlState(
            active = on,
            enabled = true,
            needsPermission = !granted,
            detail = when (filter) {
                NotificationManager.INTERRUPTION_FILTER_PRIORITY -> "Priority"
                NotificationManager.INTERRUPTION_FILTER_ALARMS -> "Alarms only"
                NotificationManager.INTERRUPTION_FILTER_NONE -> "Total silence"
                else -> null
            },
        )
    }

    private fun toggleDnd() {
        val nm = notificationManager ?: return
        if (!nm.isNotificationPolicyAccessGranted) {
            launcher.launch(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            return
        }
        val target = if (dndState.value.active) {
            NotificationManager.INTERRUPTION_FILTER_ALL
        } else {
            NotificationManager.INTERRUPTION_FILTER_PRIORITY
        }
        runCatching { nm.setInterruptionFilter(target) }
            .onSuccess { refreshDnd() }
            .onFailure { Log.w(TAG, "setInterruptionFilter failed", it) }
    }

    // ---- rotation lock -----------------------------------------------------------------------

    private fun refreshRotation() {
        val auto = Settings.System.getInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 1) == 1
        rotationState.value = ControlState(active = !auto, enabled = true, needsPermission = !canWriteSettings())
    }

    private fun toggleRotationLock() {
        if (!canWriteSettings()) {
            launchWriteSettings()
            return
        }
        val lockNow = !rotationState.value.active
        runCatching {
            Settings.System.putInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, if (lockNow) 0 else 1)
        }.onSuccess { refreshRotation() }.onFailure { Log.w(TAG, "Rotation write failed", it) }
    }

    // ---- brightness --------------------------------------------------------------------------

    private fun refreshBrightness() {
        val value = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128)
        val auto = Settings.System.getInt(
            context.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS_MODE,
            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
        ) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
        brightnessState.value = ControlState(
            active = auto,
            value = value.coerceIn(0, 255).toFloat(),
            max = 255f,
            enabled = true,
            needsPermission = !canWriteSettings(),
            detail = if (auto) "Auto" else null,
        )
    }

    private fun setBrightness(level: Int) {
        if (!canWriteSettings()) {
            launchWriteSettings()
            return
        }
        val clamped = level.coerceIn(0, 255)
        runCatching {
            val resolver = context.contentResolver
            if (brightnessState.value.active) {
                Settings.System.putInt(
                    resolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
                )
            }
            Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, clamped)
        }.onSuccess {
            brightnessState.update { it.copy(value = clamped.toFloat(), active = false, detail = null) }
        }.onFailure { Log.w(TAG, "Brightness write failed", it) }
    }

    private fun canWriteSettings(): Boolean = Settings.System.canWrite(context)

    private fun launchWriteSettings() {
        launcher.launch(
            Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}")),
        )
    }

    // ---- volume ------------------------------------------------------------------------------

    private fun refreshVolumes() {
        val am = audioManager ?: return
        runCatching {
            val mediaMax = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val media = am.getStreamVolume(AudioManager.STREAM_MUSIC)
            mediaVolumeState.value = ControlState(active = media > 0, value = media.toFloat(), max = mediaMax.toFloat())
            val ringMax = am.getStreamMaxVolume(AudioManager.STREAM_RING)
            val ring = am.getStreamVolume(AudioManager.STREAM_RING)
            val silent = am.ringerMode != AudioManager.RINGER_MODE_NORMAL
            ringVolumeState.value = ControlState(
                active = ring > 0 && !silent,
                value = if (silent) 0f else ring.toFloat(),
                max = ringMax.toFloat(),
                detail = when (am.ringerMode) {
                    AudioManager.RINGER_MODE_VIBRATE -> "Vibrate"
                    AudioManager.RINGER_MODE_SILENT -> "Silent"
                    else -> null
                },
            )
        }.onFailure { Log.w(TAG, "Volume read failed", it) }
    }

    private fun setVolume(stream: Int, index: Int) {
        val am = audioManager ?: return
        val max = am.getStreamMaxVolume(stream)
        val clamped = index.coerceIn(0, max)
        runCatching { am.setStreamVolume(stream, clamped, 0) }
            .onSuccess { refreshVolumes() }
            .onFailure { Log.w(TAG, "setStreamVolume failed (DND may block ring changes)", it) }
    }

    // ---- panels ------------------------------------------------------------------------------

    private fun refreshWifi() {
        @Suppress("DEPRECATION")
        val on = runCatching { wifiManager?.isWifiEnabled == true }.getOrDefault(false)
        wifiState.value = ControlState(active = on)
    }

    private fun refreshBluetooth() {
        val on = runCatching { bluetoothAdapter?.isEnabled == true }.getOrDefault(false)
        bluetoothState.value = ControlState(active = on)
    }

    companion object {
        private const val TAG = "ControlsRegistry"

        /** Hidden but stable since Android 1.x; fires on every stream volume change. */
        private const val ACTION_VOLUME_CHANGED = "android.media.VOLUME_CHANGED_ACTION"

        const val ID_TORCH = "torch"
        const val ID_TORCH_STRENGTH = "torch_strength"
        const val ID_DND = "dnd"
        const val ID_ROTATION = "rotation"
        const val ID_BRIGHTNESS = "brightness"
        const val ID_MEDIA_VOLUME = "media_volume"
        const val ID_RING_VOLUME = "ring_volume"
        const val ID_WIFI = "wifi"
        const val ID_BLUETOOTH = "bluetooth"
        const val ID_CAMERA = "camera"
        const val ID_SCREENSHOT = "screenshot"
        const val ID_LOCK = "lock"
        const val ID_RECENTS = "recents"

        val TILE_IDS: List<String> = listOf(
            ID_TORCH, ID_DND, ID_ROTATION, ID_CAMERA, ID_WIFI, ID_BLUETOOTH, ID_SCREENSHOT, ID_LOCK, ID_RECENTS,
        )
        val SLIDER_IDS: List<String> = listOf(ID_BRIGHTNESS, ID_MEDIA_VOLUME, ID_RING_VOLUME, ID_TORCH_STRENGTH)
    }
}
