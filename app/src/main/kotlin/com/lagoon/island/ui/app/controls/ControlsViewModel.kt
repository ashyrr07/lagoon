package com.lagoon.island.ui.app.controls

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lagoon.island.controls.AppInfo
import com.lagoon.island.controls.AppShortcuts
import com.lagoon.island.controls.ControlLayout
import com.lagoon.island.controls.ControlsRegistry
import com.lagoon.island.controls.Widgets
import com.lagoon.island.data.ControlSettings
import com.lagoon.island.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** A row in one of the reorderable lists. */
data class ControlsItem(val id: String, val label: String)

/** Which list a reorder/add/remove applies to. */
enum class ControlsSection { Tiles, Sliders, Widgets }

@HiltViewModel
class ControlsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val registry: ControlsRegistry,
    private val shortcuts: AppShortcuts,
) : ViewModel() {

    val settings: StateFlow<ControlSettings> = repository.settings
        .map { it.controls }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ControlSettings())

    val globalsAvailable: StateFlow<Boolean> = registry.globalsAvailable

    private val _apps = MutableStateFlow<List<AppInfo>?>(null)
    /** Launchable apps for the picker; null until loaded. */
    val apps: StateFlow<List<AppInfo>?> = _apps.asStateFlow()

    /** Ids this device offers for [section], in registry order; Enhanced-only tiles only when active. */
    fun available(section: ControlsSection): List<ControlsItem> = when (section) {
        ControlsSection.Tiles -> registry.tiles.filter { it.isAvailable() }.map { ControlsItem(it.id, it.label) }
        ControlsSection.Sliders -> registry.sliders.filter { it.isAvailable() }.map { ControlsItem(it.id, it.label) }
        ControlsSection.Widgets -> Widgets.ALL.map { ControlsItem(it.id, it.label) }
    }

    fun labelOf(id: String): String =
        registry[id]?.label ?: Widgets.ALL.firstOrNull { it.id == id }?.label ?: id

    fun chosen(section: ControlsSection, settings: ControlSettings): List<String> {
        val available = available(section).map { it.id }
        return ControlLayout.resolve(idsOf(section, settings), available)
    }

    fun remaining(section: ControlsSection, settings: ControlSettings): List<ControlsItem> {
        val available = available(section)
        val rest = ControlLayout.remaining(idsOf(section, settings), available.map { it.id }).toSet()
        return available.filter { it.id in rest }
    }

    fun move(section: ControlsSection, from: Int, to: Int) = edit(section) { ControlLayout.move(it, from, to) }

    fun add(section: ControlsSection, id: String) = edit(section) { if (id in it) it else it + id }

    fun remove(section: ControlsSection, id: String) = edit(section) { ControlLayout.remove(it, id) }

    fun addShortcut(packageName: String) = update {
        it.copy(shortcuts = if (packageName in it.shortcuts) it.shortcuts else it.shortcuts + packageName)
    }

    fun removeShortcut(packageName: String) = update { it.copy(shortcuts = ControlLayout.remove(it.shortcuts, packageName)) }

    fun moveShortcut(from: Int, to: Int) = update { it.copy(shortcuts = ControlLayout.move(it.shortcuts, from, to)) }

    fun resetToDefaults() = update { ControlSettings() }

    fun shortcutLabel(packageName: String): String = shortcuts.label(packageName) ?: packageName

    fun shortcutIcon(packageName: String) = shortcuts.icon(packageName)

    fun loadApps() {
        if (_apps.value != null) return
        viewModelScope.launch {
            _apps.value = withContext(Dispatchers.IO) { shortcuts.launchableApps() }
        }
    }

    private fun edit(section: ControlsSection, transform: (List<String>) -> List<String>) = update { c ->
        when (section) {
            ControlsSection.Tiles -> c.copy(tiles = transform(c.tiles))
            ControlsSection.Sliders -> c.copy(sliders = transform(c.sliders))
            ControlsSection.Widgets -> c.copy(widgets = transform(c.widgets))
        }
    }

    private fun update(transform: (ControlSettings) -> ControlSettings) {
        viewModelScope.launch { repository.update { it.copy(controls = transform(it.controls)) } }
    }

    private fun idsOf(section: ControlsSection, settings: ControlSettings): List<String> = when (section) {
        ControlsSection.Tiles -> settings.tiles
        ControlsSection.Sliders -> settings.sliders
        ControlsSection.Widgets -> settings.widgets
    }
}
