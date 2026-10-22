package com.lagoon.island.controls

import android.Manifest
import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import com.lagoon.island.engine.ActivityContent
import com.lagoon.island.engine.ActivityRegistry
import com.lagoon.island.engine.IslandActivity
import com.lagoon.island.util.IntentLauncher
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Static description of a built-in widget, for the Controls screen. */
data class WidgetSpec(val id: String, val label: String, val icon: ImageVector)

data class BatteryInfo(
    val level: Int = -1,
    val charging: Boolean = false,
    val full: Boolean = false,
)

sealed interface CalendarState {
    data object NoPermission : CalendarState
    data object Empty : CalendarState
    data class Next(val title: String, val startAt: Long, val endAt: Long, val allDay: Boolean) : CalendarState
}

/**
 * Data behind the built-in widgets. Receivers and observers are live only while someone holds an
 * [observe] handle; the derived flows from [ActivityRegistry] are shared while subscribed.
 */
@Singleton
class Widgets @Inject constructor(
    @ApplicationContext private val context: Context,
    private val registry: ActivityRegistry,
    private val launcher: IntentLauncher,
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    /** The media activity to show in Now Playing: a playing one if any, else the newest. */
    val nowPlaying: StateFlow<IslandActivity?> = registry.activities
        .map { map -> WidgetSelection.selectNowPlaying(map.values) }
        .stateIn(scope, SharingStarted.WhileSubscribed(1_000), WidgetSelection.selectNowPlaying(registry.activities.value.values))

    /** A connected Bluetooth device reporting battery, if the Bluetooth source posted one. */
    val bluetoothBattery: StateFlow<ActivityContent.Bluetooth?> = registry.activities
        .map { map -> WidgetSelection.selectBluetooth(map.values) }
        .stateIn(scope, SharingStarted.WhileSubscribed(1_000), WidgetSelection.selectBluetooth(registry.activities.value.values))

    private val _nextAlarm = MutableStateFlow<Long?>(null)
    /** Trigger time (wall-clock millis) of the next alarm clock, or null. */
    val nextAlarm: StateFlow<Long?> = _nextAlarm.asStateFlow()

    private val _battery = MutableStateFlow(BatteryInfo())
    val battery: StateFlow<BatteryInfo> = _battery.asStateFlow()

    private val _calendar = MutableStateFlow<CalendarState>(CalendarState.NoPermission)
    val calendar: StateFlow<CalendarState> = _calendar.asStateFlow()

    private var observers = 0
    private var receiver: BroadcastReceiver? = null
    private var calendarObserver: ContentObserver? = null

    fun observe(): ControlsRegistry.Handle {
        mainHandler.post { acquire() }
        return ControlsRegistry.Handle { mainHandler.post { release() } }
    }

    fun refresh() {
        refreshAlarm()
        refreshBattery()
        refreshCalendar()
    }

    fun openAlarms() {
        launcher.launch(Intent(AlarmClock.ACTION_SHOW_ALARMS))
    }

    fun openCalendar(event: CalendarState.Next?) {
        val intent = if (event != null) {
            Intent(Intent.ACTION_VIEW).setData(CalendarContract.CONTENT_URI.buildUpon().appendPath("time").appendPath(event.startAt.toString()).build())
        } else {
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR)
        }
        launcher.launch(intent)
    }

    fun hasCalendarPermission(): Boolean =
        context.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    private fun acquire() {
        observers++
        if (observers > 1) return
        refresh()
        val r = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                when (intent.action) {
                    AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED -> refreshAlarm()
                    Intent.ACTION_BATTERY_CHANGED -> readBattery(intent)
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }
        runCatching { ContextCompat.registerReceiver(context, r, filter, ContextCompat.RECEIVER_EXPORTED) }
            .onSuccess { receiver = r }
            .onFailure { Log.w(TAG, "Receiver failed", it) }
        if (hasCalendarPermission()) {
            val observer = object : ContentObserver(mainHandler) {
                override fun onChange(selfChange: Boolean) = refreshCalendar()
            }
            runCatching { context.contentResolver.registerContentObserver(CalendarContract.CONTENT_URI, true, observer) }
                .onSuccess { calendarObserver = observer }
        }
    }

    private fun release() {
        observers = (observers - 1).coerceAtLeast(0)
        if (observers > 0) return
        receiver?.let { runCatching { context.unregisterReceiver(it) } }
        receiver = null
        calendarObserver?.let { runCatching { context.contentResolver.unregisterContentObserver(it) } }
        calendarObserver = null
    }

    private fun refreshAlarm() {
        _nextAlarm.value = runCatching { alarmManager?.nextAlarmClock?.triggerTime }.getOrNull()
    }

    private fun refreshBattery() {
        val sticky = runCatching {
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        }.getOrNull() ?: return
        readBattery(sticky)
    }

    private fun readBattery(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
        val pct = if (level >= 0 && scale > 0) (level * 100) / scale else -1
        _battery.value = BatteryInfo(
            level = pct,
            charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL,
            full = status == BatteryManager.BATTERY_STATUS_FULL,
        )
    }

    private fun refreshCalendar() {
        if (!hasCalendarPermission()) {
            _calendar.value = CalendarState.NoPermission
            return
        }
        scope.launch {
            val next = withContext(Dispatchers.IO) { queryNextEvent(System.currentTimeMillis()) }
            _calendar.value = next ?: CalendarState.Empty
        }
    }

    private fun queryNextEvent(now: Long): CalendarState.Next? = runCatching {
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
            .appendPath(now.toString())
            .appendPath((now + LOOKAHEAD_MS).toString())
            .build()
        val projection = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
        )
        context.contentResolver.query(
            uri,
            projection,
            "${CalendarContract.Instances.END} > ? AND ${CalendarContract.Instances.VISIBLE} = 1",
            arrayOf(now.toString()),
            "${CalendarContract.Instances.BEGIN} ASC",
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            CalendarState.Next(
                title = cursor.getString(0)?.takeIf { it.isNotBlank() } ?: "(No title)",
                startAt = cursor.getLong(1),
                endAt = cursor.getLong(2),
                allDay = cursor.getInt(3) == 1,
            )
        }
    }.onFailure { Log.w(TAG, "Calendar query failed", it) }.getOrNull()

    companion object {
        private const val TAG = "Widgets"
        private const val LOOKAHEAD_MS = 24 * 60 * 60 * 1000L

        const val ID_NOW_PLAYING = "now_playing"
        const val ID_NEXT_ALARM = "next_alarm"
        const val ID_BATTERY = "battery"
        const val ID_TIMER = "timer"
        const val ID_CALENDAR = "calendar"

        val ALL: List<WidgetSpec> = listOf(
            WidgetSpec(ID_NOW_PLAYING, "Now playing", Icons.Rounded.MusicNote),
            WidgetSpec(ID_NEXT_ALARM, "Next alarm", Icons.Rounded.Alarm),
            WidgetSpec(ID_BATTERY, "Battery", Icons.Rounded.BatteryStd),
            WidgetSpec(ID_TIMER, "Timer", Icons.Rounded.Timer),
            WidgetSpec(ID_CALENDAR, "Next event", Icons.Rounded.CalendarToday),
        )
        val IDS: List<String> = ALL.map { it.id }
    }
}

/** Pure pickers over the activity registry; unit-tested. */
object WidgetSelection {
    /** A playing media activity if any, else the most recently updated one. */
    fun selectNowPlaying(activities: Collection<IslandActivity>): IslandActivity? {
        val media = activities.filter { it.id.startsWith("media:") && it.content is ActivityContent.Media }
        return media.firstOrNull { (it.content as ActivityContent.Media).isPlaying }
            ?: media.maxByOrNull { it.updatedAt }
    }

    /** The first connected Bluetooth device that reports any battery level. */
    fun selectBluetooth(activities: Collection<IslandActivity>): ActivityContent.Bluetooth? =
        activities
            .filter { it.id.startsWith("bt:") }
            .mapNotNull { it.content as? ActivityContent.Bluetooth }
            .firstOrNull { it.connected && (it.battery != null || it.left != null || it.right != null) }
}
