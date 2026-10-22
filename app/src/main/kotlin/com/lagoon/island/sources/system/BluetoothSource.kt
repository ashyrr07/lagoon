package com.lagoon.island.sources.system

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.ParcelUuid
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import com.lagoon.island.R
import com.lagoon.island.data.SettingsRepository
import com.lagoon.island.engine.ActivityContent
import com.lagoon.island.engine.ActivityRegistry
import com.lagoon.island.engine.IslandActivity
import com.lagoon.island.engine.Priority
import com.lagoon.island.engine.Source
import com.lagoon.island.sources.IslandSource
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Connect / disconnect alerts with battery where the platform or the device gives it away:
 * the (unpublished but long-standing) BATTERY_LEVEL_CHANGED broadcast, Apple's +IPHONEACCEV HFP
 * command, and, for a few seconds after an audio device connects, a filtered BLE scan for AirPods'
 * proximity-pairing frames and Fast Pair battery frames (Pixel Buds). Needs BLUETOOTH_CONNECT;
 * the scan additionally needs BLUETOOTH_SCAN and is skipped without it.
 */
@Singleton
class BluetoothSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val registry: ActivityRegistry,
    settingsRepository: SettingsRepository,
) : IslandSource {
    private class Device(
        val address: String,
        var name: String,
        var kind: ActivityContent.Bluetooth.Kind,
        var connectedAt: Long,
        var battery: Int? = null,
        var pods: PodLevels? = null,
    ) {
        fun content(connected: Boolean) = ActivityContent.Bluetooth(
            deviceName = name,
            connected = connected,
            battery = battery ?: pods?.let { p -> listOfNotNull(p.left, p.right).minOrNull() },
            left = pods?.left,
            right = pods?.right,
            case = pods?.case,
            kind = kind,
        )
    }

    private val features = FeatureWatcher(settingsRepository)
    private val receivers = Receivers(context)
    private val devices = HashMap<String, Device>()
    private var scope: CoroutineScope? = null
    private var scanner: BluetoothLeScanner? = null
    private var scanCallback: ScanCallback? = null
    private var scanJob: Job? = null
    private var scanTarget: String? = null

    override fun start(scope: CoroutineScope) {
        this.scope = scope
        features.watch(scope)
        receivers.register(BluetoothDevice.ACTION_ACL_CONNECTED, BluetoothDevice.ACTION_ACL_DISCONNECTED) { onAcl(it) }
        receivers.register(ACTION_BATTERY_LEVEL_CHANGED) { onBatteryLevel(it) }
        receivers.register(
            IntentFilter(BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT).apply {
                addCategory("${BluetoothHeadset.VENDOR_SPECIFIC_HEADSET_EVENT_COMPANY_ID_CATEGORY}.${AirPodsPayload.APPLE_COMPANY_ID}")
            },
        ) { onVendorEvent(it) }
    }

    override fun stop() {
        receivers.unregisterAll()
        stopScan()
        scope = null
        registry.removeWhere { it.source == Source.Bluetooth }
        devices.clear()
    }

    private fun onAcl(intent: Intent) {
        val device = IntentCompat.getParcelableExtra(intent, BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java) ?: return
        val address = device.address ?: return
        val now = System.currentTimeMillis()
        when (intent.action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                val name = deviceName(device)
                val kind = BluetoothKinds.kind(deviceClass(device), name)
                val entry = Device(address, name, kind, now).also { devices[address] = it }
                if (!features.current.bluetooth) return
                postAlert(entry)
                if (BluetoothKinds.isAudio(kind)) startScan(address)
            }
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                val entry = devices.remove(address) ?: return
                if (scanTarget == address) stopScan()
                if (!features.current.bluetooth) return
                registry.post(
                    IslandActivity(
                        id = idFor(address),
                        source = Source.Bluetooth,
                        priority = Priority.Alert,
                        content = entry.content(connected = false),
                        startedAt = now,
                        ttlMs = DISCONNECT_TTL_MS,
                    ),
                )
            }
        }
    }

    private fun onBatteryLevel(intent: Intent) {
        val device = IntentCompat.getParcelableExtra(intent, BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java) ?: return
        val level = intent.getIntExtra(EXTRA_BATTERY_LEVEL, -1)
        if (level !in 0..100) return
        val entry = devices[device.address] ?: Device(device.address, deviceName(device), BluetoothKinds.kind(null, deviceName(device)), 0L)
            .also { devices[device.address] = it }
        entry.battery = level
        refresh(entry)
    }

    private fun onVendorEvent(intent: Intent) {
        val command = intent.getStringExtra(BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_CMD) ?: return
        if (!command.equals(IphoneAccev.COMMAND, ignoreCase = true)) return
        // The stack puts an Object[] (arrays are Serializable), so it comes back as one.
        val args = runCatching {
            IntentCompat.getSerializableExtra(intent, BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_ARGS, Array<Any?>::class.java)
        }.getOrNull()?.toList()
        val percent = IphoneAccev.parseBatteryPercent(args) ?: return
        val device = IntentCompat.getParcelableExtra(intent, BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java) ?: return
        val entry = devices[device.address] ?: return
        entry.battery = percent
        refresh(entry)
    }

    /** Pushes new battery details into the live alert, or shows a short compact card if it already went. */
    private fun refresh(entry: Device) {
        if (!features.current.bluetooth) return
        val now = System.currentTimeMillis()
        val id = idFor(entry.address)
        val existing = registry[id]
        if (existing != null && (existing.content as? ActivityContent.Bluetooth)?.connected == true) {
            registry.update(id) { it.copy(content = entry.content(connected = true)).refreshed(now, REFRESH_TTL_MS) }
        } else if (now - entry.connectedAt <= LATE_DETAIL_WINDOW_MS) {
            registry.post(
                IslandActivity(
                    id = id,
                    source = Source.Bluetooth,
                    priority = Priority.Alert,
                    content = entry.content(connected = true),
                    startedAt = now,
                    ttlMs = REFRESH_TTL_MS,
                ),
            )
        }
    }

    private fun postAlert(entry: Device) {
        val now = System.currentTimeMillis()
        val alertMs = features.current.alertMs()
        registry.post(
            IslandActivity(
                id = idFor(entry.address),
                source = Source.Bluetooth,
                priority = Priority.Alert,
                content = entry.content(connected = true),
                startedAt = now,
                alertMs = alertMs,
                ttlMs = alertMs + CONNECT_LINGER_MS,
            ),
        )
    }

    private fun startScan(address: String) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) return
        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter ?: return
        val leScanner = runCatching { adapter.bluetoothLeScanner }.getOrNull() ?: return
        stopScan()
        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) = onScanResult(result)
            override fun onBatchScanResults(results: MutableList<ScanResult>) = results.forEach { onScanResult(it) }
            override fun onScanFailed(errorCode: Int) {
                Log.w(TAG, "BLE scan failed: $errorCode")
                stopScan()
            }
        }
        val filters = listOf(
            ScanFilter.Builder()
                .setManufacturerData(AirPodsPayload.APPLE_COMPANY_ID, byteArrayOf(AirPodsPayload.PROXIMITY_PAIRING.toByte()), byteArrayOf(0xFF.toByte()))
                .build(),
            ScanFilter.Builder().setServiceData(ParcelUuid.fromString(FastPairPayload.SERVICE_UUID), ByteArray(0), ByteArray(0)).build(),
        )
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        val started = runCatching { leScanner.startScan(filters, settings, callback) }.isSuccess
        if (!started) return
        scanner = leScanner
        scanCallback = callback
        scanTarget = address
        scanJob = scope?.launch {
            delay(SCAN_MAX_MS)
            stopScan()
        }
    }

    private fun onScanResult(result: ScanResult) {
        val target = scanTarget?.let { devices[it] } ?: return
        val record = result.scanRecord ?: return
        val pods = AirPodsPayload.parse(record.getManufacturerSpecificData(AirPodsPayload.APPLE_COMPANY_ID))
            ?: FastPairPayload.parseBattery(record.getServiceData(ParcelUuid.fromString(FastPairPayload.SERVICE_UUID)))
            ?: return
        if (!pods.hasAny) return
        target.pods = pods
        refresh(target)
        if (pods.left != null && pods.right != null) stopScan()
    }

    private fun stopScan() {
        scanJob?.cancel()
        scanJob = null
        val callback = scanCallback
        val leScanner = scanner
        if (callback != null && leScanner != null &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        ) {
            runCatching { leScanner.stopScan(callback) }
        }
        scanner = null
        scanCallback = null
        scanTarget = null
    }

    private fun deviceName(device: BluetoothDevice): String {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return fallbackName()
        return runCatching { device.name }.getOrNull()?.takeIf { it.isNotBlank() } ?: fallbackName()
    }

    private fun fallbackName(): String = context.getString(R.string.island_bt_device_fallback)

    private fun deviceClass(device: BluetoothDevice): Int? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return null
        return runCatching { device.bluetoothClass?.deviceClass }.getOrNull()
    }

    companion object {
        private const val TAG = "BluetoothSource"
        const val ACTION_BATTERY_LEVEL_CHANGED = "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED"
        const val EXTRA_BATTERY_LEVEL = "android.bluetooth.device.extra.BATTERY_LEVEL"
        private const val SCAN_MAX_MS = 8_000L
        private const val CONNECT_LINGER_MS = 2_000L
        private const val REFRESH_TTL_MS = 4_000L
        private const val LATE_DETAIL_WINDOW_MS = 15_000L
        private const val DISCONNECT_TTL_MS = 3_000L

        fun idFor(address: String) = "bt:$address"
    }
}
