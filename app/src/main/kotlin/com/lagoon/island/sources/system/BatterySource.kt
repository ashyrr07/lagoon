package com.lagoon.island.sources.system

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
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
 * Charger plugged / unplugged, low battery and full battery. Wattage comes from the battery's
 * instantaneous current and voltage, sampled at plug-in and again two seconds later when the
 * charger has negotiated its real power.
 */
@Singleton
class BatterySource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val registry: ActivityRegistry,
    settingsRepository: SettingsRepository,
) : IslandSource {
    private val features = FeatureWatcher(settingsRepository)
    private val receivers = Receivers(context)
    private var scope: CoroutineScope? = null
    private var refineJob: Job? = null

    private var plugged = false
    private var level = -1
    private var status = BatteryManager.BATTERY_STATUS_UNKNOWN
    private var voltageRaw = 0
    private var lowAlerted = false
    private var fullAlerted = false

    override fun start(scope: CoroutineScope) {
        this.scope = scope
        features.watch(scope)
        val sticky = receivers.register(IntentFilter(Intent.ACTION_BATTERY_CHANGED)) { onBatteryChanged(it) }
        if (sticky != null) {
            readSnapshot(sticky)
            // Do not alert for whatever state the phone is already in when the island starts.
            lowAlerted = !plugged && level in 0..LOW_LEVEL
            fullAlerted = plugged && isFull()
        }
        receivers.register(Intent.ACTION_POWER_CONNECTED, Intent.ACTION_POWER_DISCONNECTED) { onPower(it) }
    }

    override fun stop() {
        receivers.unregisterAll()
        refineJob?.cancel()
        refineJob = null
        scope = null
        registry.remove(ID_CHARGING)
    }

    private fun readSnapshot(intent: Intent) {
        level = levelOf(intent) ?: level
        status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, status)
        voltageRaw = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, voltageRaw)
        plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
    }

    private fun onBatteryChanged(intent: Intent) {
        val wasPlugged = plugged
        readSnapshot(intent)
        if (plugged != wasPlugged) {
            // POWER_CONNECTED / DISCONNECTED carry the alert; just keep the cycle flags coherent.
            if (plugged) lowAlerted = false else fullAlerted = false
        }
        registry[ID_CHARGING]?.let { current ->
            val content = current.content as? ActivityContent.Charging ?: return@let
            if (content.level != level) registry.update(ID_CHARGING) { it.copy(content = content.copy(level = level)) }
        }
        val f = features.current
        if (!plugged && level in 0..LOW_LEVEL && !lowAlerted && f.lowBattery) {
            lowAlerted = true
            postAlert(ID_LOW, ActivityContent.Charging(level, null, ActivityContent.Charging.State.Low), f.alertMs())
        }
        if (plugged && isFull() && !fullAlerted && f.charging) {
            fullAlerted = true
            postAlert(ID_FULL, ActivityContent.Charging(level, null, ActivityContent.Charging.State.Full), f.alertMs())
        }
    }

    private fun onPower(intent: Intent) {
        val f = features.current
        when (intent.action) {
            Intent.ACTION_POWER_CONNECTED -> {
                plugged = true
                lowAlerted = false
                registry.remove(ID_LOW)
                if (!f.charging) return
                val first = sampleWatts()
                postAlert(ID_CHARGING, ActivityContent.Charging(level, first, ActivityContent.Charging.State.Connected), f.alertMs())
                refineJob?.cancel()
                refineJob = scope?.launch {
                    delay(REFINE_DELAY_MS)
                    val refined = ChargingMath.refine(first, sampleWatts())
                    registry.update(ID_CHARGING) { activity ->
                        val content = activity.content as? ActivityContent.Charging ?: return@update activity
                        activity.copy(content = content.copy(level = level, watts = refined))
                    }
                }
            }
            Intent.ACTION_POWER_DISCONNECTED -> {
                plugged = false
                fullAlerted = false
                refineJob?.cancel()
                registry.remove(ID_FULL)
                if (!f.charging) {
                    registry.remove(ID_CHARGING)
                    return
                }
                val now = System.currentTimeMillis()
                registry.post(
                    IslandActivity(
                        id = ID_CHARGING,
                        source = Source.Charging,
                        priority = Priority.Alert,
                        content = ActivityContent.Charging(level, null, ActivityContent.Charging.State.Disconnected),
                        startedAt = now,
                        ttlMs = DISCONNECT_TTL_MS,
                    ),
                )
            }
        }
    }

    private fun postAlert(id: String, content: ActivityContent.Charging, alertMs: Long) {
        val now = System.currentTimeMillis()
        registry.post(
            IslandActivity(
                id = id,
                source = Source.Charging,
                priority = Priority.Alert,
                content = content,
                startedAt = now,
                alertMs = alertMs,
                ttlMs = alertMs + 1_000,
            ),
        )
    }

    private fun sampleWatts(): Float? {
        val manager = context.getSystemService(BatteryManager::class.java) ?: return null
        val current = runCatching { manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) }.getOrDefault(Int.MIN_VALUE)
        return ChargingMath.watts(current, voltageRaw)
    }

    private fun isFull(): Boolean = status == BatteryManager.BATTERY_STATUS_FULL || level >= 100

    private fun levelOf(intent: Intent): Int? {
        val raw = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        if (raw < 0 || scale <= 0) return null
        return (raw * 100 / scale).coerceIn(0, 100)
    }

    companion object {
        const val ID_CHARGING = "charging"
        const val ID_LOW = "battery-low"
        const val ID_FULL = "battery-full"
        const val LOW_LEVEL = 15
        private const val REFINE_DELAY_MS = 2_000L
        private const val DISCONNECT_TTL_MS = 2_500L
    }
}
