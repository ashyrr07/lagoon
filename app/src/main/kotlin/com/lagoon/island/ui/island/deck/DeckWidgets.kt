package com.lagoon.island.ui.island.deck

import android.text.format.DateFormat
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lagoon.island.R
import com.lagoon.island.controls.BatteryInfo
import com.lagoon.island.controls.CalendarState
import com.lagoon.island.controls.TimerSnapshot
import com.lagoon.island.engine.ActivityContent
import com.lagoon.island.engine.IslandActivity
import com.lagoon.island.ui.island.content.percentText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

/** The card every widget sits in: fixed size so the row scrolls predictably. */
@Composable
fun WidgetCard(
    modifier: Modifier = Modifier,
    width: androidx.compose.ui.unit.Dp = DeckStyle.WidgetWidth,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .width(width)
            .height(DeckStyle.WidgetHeight)
            .clip(DeckStyle.WidgetShape)
            .background(DeckStyle.Surface)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        content()
    }
}

@Composable
private fun IconText(icon: ImageVector, title: String, body: String?, tint: androidx.compose.ui.graphics.Color = DeckStyle.Accent) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, style = DeckStyle.Title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (body != null) Text(body, style = DeckStyle.Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Small round icon button inside a widget; 36 dp visual inside a 44 dp touch target. */
@Composable
private fun WidgetButton(icon: ImageVector, description: String, onClick: () -> Unit, filled: Boolean = false) {
    val view = LocalView.current
    Box(
        Modifier
            .size(DeckStyle.TouchTarget)
            .clip(DeckStyle.PillShape)
            .clickable {
                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(DeckStyle.PillShape)
                .background(if (filled) DeckStyle.Accent else DeckStyle.SurfaceHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = description,
                tint = if (filled) DeckStyle.OnAccent else DeckStyle.Text,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

// ---- Now playing -------------------------------------------------------------------------------

@Composable
fun NowPlayingWidget(activity: IslandActivity?, onOpen: (IslandActivity) -> Unit) {
    val media = activity?.content as? ActivityContent.Media
    if (activity == null || media == null) {
        WidgetCard {
            IconText(
                Icons.Rounded.MusicNote,
                stringResource(R.string.island_widget_nothing_playing),
                stringResource(R.string.island_widget_nothing_playing_hint),
                DeckStyle.TextFaint,
            )
        }
        return
    }
    WidgetCard(width = 232.dp, onClick = { onOpen(activity) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val art = media.art
            if (art != null) {
                Image(
                    art.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)),
                )
            } else {
                Box(
                    Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(DeckStyle.SurfaceHigh),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.MusicNote, null, tint = DeckStyle.TextMuted, modifier = Modifier.size(20.dp)) }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(media.title.ifBlank { media.appName }, style = DeckStyle.Title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(media.subtitle.ifBlank { media.appName }, style = DeckStyle.Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            val playPause = activity.actions.firstOrNull { it.id == "play_pause" }
            val next = activity.actions.firstOrNull { it.id == "next" }
            if (playPause != null) {
                WidgetButton(
                    if (media.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    stringResource(if (media.isPlaying) R.string.island_action_pause else R.string.island_action_play),
                    onClick = { playPause.perform(null) },
                    filled = true,
                )
            }
            if (next != null) {
                WidgetButton(Icons.Rounded.SkipNext, stringResource(R.string.island_action_next), onClick = { next.perform(null) })
            }
        }
    }
}

// ---- Next alarm --------------------------------------------------------------------------------

@Composable
fun NextAlarmWidget(triggerAt: Long?, onOpen: () -> Unit) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val parts = remember(triggerAt, locale) {
        if (triggerAt == null) {
            null
        } else {
            val time = DateFormat.getTimeFormat(context).format(Date(triggerAt))
            val dayMs = 24 * 60 * 60 * 1000L
            val day = when {
                triggerAt - System.currentTimeMillis() < dayMs && isSameDay(triggerAt, System.currentTimeMillis()) -> AlarmDay.Today
                triggerAt - System.currentTimeMillis() < 2 * dayMs -> AlarmDay.Tomorrow
                else -> AlarmDay.Weekday(SimpleDateFormat("EEE", locale).format(Date(triggerAt)))
            }
            day to time
        }
    }
    val text = parts?.let { (day, time) ->
        when (day) {
            AlarmDay.Today -> stringResource(R.string.island_widget_alarm_today, time)
            AlarmDay.Tomorrow -> stringResource(R.string.island_widget_alarm_tomorrow, time)
            is AlarmDay.Weekday -> "${day.name} · $time"
        }
    }
    WidgetCard(onClick = onOpen) {
        IconText(
            Icons.Rounded.Alarm,
            text ?: stringResource(R.string.island_widget_no_alarm),
            stringResource(if (text == null) R.string.island_widget_no_alarm_hint else R.string.island_widget_next_alarm),
        )
    }
}

private sealed interface AlarmDay {
    data object Today : AlarmDay
    data object Tomorrow : AlarmDay
    data class Weekday(val name: String) : AlarmDay
}

private fun isSameDay(a: Long, b: Long): Boolean {
    val fmt = SimpleDateFormat("yyyyDDD", Locale.ROOT)
    return fmt.format(Date(a)) == fmt.format(Date(b))
}

// ---- Battery -----------------------------------------------------------------------------------

@Composable
fun BatteryWidget(battery: BatteryInfo, buds: ActivityContent.Bluetooth?) {
    WidgetCard(width = if (buds != null) 200.dp else DeckStyle.WidgetWidth) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconText(
                if (battery.charging) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryStd,
                if (battery.level >= 0) percentText(battery.level) else stringResource(R.string.island_widget_battery),
                stringResource(
                    when {
                        battery.full -> R.string.island_widget_battery_full
                        battery.charging -> R.string.island_widget_battery_charging
                        else -> R.string.island_widget_battery_phone
                    },
                ),
                tint = if (battery.level in 0..15 && !battery.charging) DeckStyle.Warn else DeckStyle.Accent,
            )
            if (buds != null) {
                Spacer(Modifier.width(12.dp))
                val level = buds.battery ?: listOfNotNull(buds.left, buds.right).minOrNull()
                IconText(
                    Icons.Rounded.Headphones,
                    level?.let { percentText(it) } ?: "—",
                    buds.deviceName.take(14),
                    tint = DeckStyle.TextMuted,
                )
            }
        }
    }
}

// ---- Timer -------------------------------------------------------------------------------------

@Composable
fun TimerWidget(
    snapshot: TimerSnapshot,
    now: () -> Long,
    onPreset: (Int) -> Unit,
    onToggle: () -> Unit,
    onReset: () -> Unit,
) {
    var tick by remember { mutableLongStateOf(now()) }
    LaunchedEffect(snapshot.running) {
        while (snapshot.running) {
            tick = now()
            delay(250)
        }
        tick = now()
    }
    WidgetCard(width = 232.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Timer, null, tint = if (snapshot.running) DeckStyle.Accent else DeckStyle.TextMuted, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(TimerSnapshot.format(snapshot.remainingMs(tick)), style = DeckStyle.Big, maxLines = 1)
                if (snapshot.idle) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TimerSnapshot.PRESETS_MIN.forEach { minutes ->
                            val selected = snapshot.durationMs == minutes * 60_000L
                            Box(
                                Modifier
                                    .height(20.dp)
                                    .clip(DeckStyle.PillShape)
                                    .background(if (selected) DeckStyle.Accent.copy(alpha = 0.25f) else DeckStyle.SurfaceHigh)
                                    .clickable { onPreset(minutes) }
                                    .padding(horizontal = 8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(pluralStringResource(R.plurals.island_widget_timer_preset, minutes, minutes), style = DeckStyle.Label.copy(color = if (selected) DeckStyle.Accent else DeckStyle.TextMuted))
                            }
                        }
                    }
                } else {
                    Text(
                        stringResource(if (snapshot.running) R.string.island_widget_timer_running else R.string.island_widget_timer_paused),
                        style = DeckStyle.Body,
                    )
                }
            }
            if (!snapshot.idle) {
                WidgetButton(Icons.Rounded.Refresh, stringResource(R.string.island_action_reset), onClick = onReset)
            }
            WidgetButton(
                if (snapshot.running) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                stringResource(if (snapshot.running) R.string.island_action_pause else R.string.island_widget_timer_start),
                onClick = onToggle,
                filled = true,
            )
        }
    }
}

// ---- Calendar ----------------------------------------------------------------------------------

@Composable
fun CalendarWidget(state: CalendarState, onOpen: (CalendarState.Next?) -> Unit) {
    val context = LocalContext.current
    when (state) {
        CalendarState.NoPermission -> WidgetCard(width = 200.dp) {
            IconText(
                Icons.Rounded.CalendarToday,
                stringResource(R.string.island_widget_calendar),
                stringResource(R.string.island_widget_calendar_grant),
                DeckStyle.TextFaint,
            )
        }
        CalendarState.Empty -> WidgetCard(onClick = { onOpen(null) }) {
            IconText(
                Icons.Rounded.CalendarToday,
                stringResource(R.string.island_widget_calendar_empty),
                stringResource(R.string.island_widget_calendar_empty_hint),
            )
        }
        is CalendarState.Next -> WidgetCard(width = 200.dp, onClick = { onOpen(state) }) {
            val whenText = if (state.allDay) {
                stringResource(R.string.island_widget_calendar_all_day)
            } else {
                DateFormat.getTimeFormat(context).format(Date(state.startAt))
            }
            IconText(Icons.Rounded.CalendarToday, state.title ?: stringResource(R.string.island_widget_calendar_no_title), whenText)
        }
    }
}
