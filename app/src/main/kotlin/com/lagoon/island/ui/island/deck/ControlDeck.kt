package com.lagoon.island.ui.island.deck

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lagoon.island.controls.Control
import com.lagoon.island.controls.ControlKind
import com.lagoon.island.controls.ControlLayout
import com.lagoon.island.controls.ControlsEntryPoint
import com.lagoon.island.controls.Widgets
import com.lagoon.island.engine.IslandActivity
import com.lagoon.island.ui.island.IslandUiState
import androidx.compose.runtime.collectAsState

/**
 * Quick controls, widgets and app shortcuts, drawn inside the expanded island card.
 *
 * Layout (top to bottom): 3-column tile grid, slider rows, a horizontal widgets row, a row of app
 * icons. Everything comes from [IslandUiState.settings] (what to show, in which order) and the
 * controls singletons (live state). Total height stays around 330 dp with the default layout and
 * scrolls if the user picked more.
 */
@Composable
fun ControlDeck(state: IslandUiState, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val deps = remember(context) { ControlsEntryPoint.from(context) }
    val registry = deps.registry()
    val widgets = deps.widgets()
    val timer = deps.timer()
    val shortcuts = deps.shortcuts()

    DisposableEffect(registry, widgets) {
        val a = registry.observe()
        val b = widgets.observe()
        onDispose {
            a.close()
            b.close()
        }
    }

    val layout = state.settings.controls
    val globals by registry.globalsAvailable.collectAsState()
    val tiles = remember(layout.tiles, globals) {
        ControlLayout.resolve(layout.tiles, registry.tiles.map { it.id })
            .mapNotNull { registry[it] }
            .filter { it.isAvailable() }
    }
    val sliders = remember(layout.sliders) {
        ControlLayout.resolve(layout.sliders, registry.sliders.map { it.id })
            .mapNotNull { registry[it] }
            .filter { it.isAvailable() }
    }
    val widgetIds = remember(layout.widgets) { ControlLayout.resolve(layout.widgets, Widgets.IDS) }
    val shortcutPackages = remember(layout.shortcuts) { layout.shortcuts.filter { shortcuts.isInstalled(it) } }

    fun perform(control: Control, value: Float?) {
        state.onInteraction()
        when (control.kind) {
            ControlKind.Toggle, ControlKind.Slider -> control.perform(value)
            ControlKind.Launch, ControlKind.Panel, ControlKind.Global -> {
                state.onCollapse()
                control.perform(value)
            }
        }
    }

    Column(
        modifier
            .fillMaxWidth()
            .heightIn(max = MAX_HEIGHT)
            // Every touch anywhere in the deck pushes the auto-collapse deadline out.
            .pointerInput(state) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    state.onInteraction()
                }
            }
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(DeckStyle.SectionGap),
    ) {
        // ---- tiles -----------------------------------------------------------------------
        if (tiles.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(DeckStyle.Gap)) {
                ControlLayout.rows(tiles, COLUMNS).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(DeckStyle.Gap)) {
                        row.forEach { control ->
                            val controlState by control.state.collectAsState()
                            DeckTile(
                                control = control,
                                state = controlState,
                                onPerform = { perform(control, null) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }

        // ---- sliders ---------------------------------------------------------------------
        if (sliders.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                sliders.forEach { control ->
                    val controlState by control.state.collectAsState()
                    DeckSlider(
                        control = control,
                        state = controlState,
                        onChange = { perform(control, it) },
                    )
                }
            }
        }

        // ---- widgets ---------------------------------------------------------------------
        if (widgetIds.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().height(DeckStyle.WidgetHeight).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(DeckStyle.Gap),
            ) {
                widgetIds.forEach { id ->
                    when (id) {
                        Widgets.ID_NOW_PLAYING -> {
                            val playing by widgets.nowPlaying.collectAsState()
                            NowPlayingWidget(playing, onOpen = { activity: IslandActivity ->
                                state.onInteraction()
                                activity.onTap?.let { tap ->
                                    state.onCollapse()
                                    tap()
                                }
                            })
                        }
                        Widgets.ID_NEXT_ALARM -> {
                            val alarm by widgets.nextAlarm.collectAsState()
                            NextAlarmWidget(alarm, onOpen = {
                                state.onCollapse()
                                widgets.openAlarms()
                            })
                        }
                        Widgets.ID_BATTERY -> {
                            val battery by widgets.battery.collectAsState()
                            val buds by widgets.bluetoothBattery.collectAsState()
                            BatteryWidget(battery, buds)
                        }
                        Widgets.ID_TIMER -> {
                            val snapshot by timer.snapshot.collectAsState()
                            TimerWidget(
                                snapshot = snapshot,
                                now = timer::now,
                                onPreset = { state.onInteraction(); timer.setPresetMinutes(it) },
                                onToggle = { state.onInteraction(); timer.toggle() },
                                onReset = { state.onInteraction(); timer.reset() },
                            )
                        }
                        Widgets.ID_CALENDAR -> {
                            val calendar by widgets.calendar.collectAsState()
                            CalendarWidget(calendar, onOpen = { event ->
                                state.onCollapse()
                                widgets.openCalendar(event)
                            })
                        }
                    }
                }
            }
        }

        // ---- shortcuts -------------------------------------------------------------------
        if (shortcutPackages.isNotEmpty()) {
            ShortcutsRow(
                packages = shortcutPackages,
                shortcuts = shortcuts,
                onLaunch = { pkg ->
                    state.onCollapse()
                    shortcuts.launch(pkg)
                },
                modifier = Modifier.fillMaxWidth().height(DeckStyle.TouchTarget),
            )
        }
    }
}

private const val COLUMNS = 3
private val MAX_HEIGHT = 330.dp
