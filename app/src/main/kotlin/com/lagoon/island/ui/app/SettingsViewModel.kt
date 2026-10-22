package com.lagoon.island.ui.app

import android.content.Context
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lagoon.island.data.IslandSettings
import com.lagoon.island.data.SettingsRepository
import com.lagoon.island.service.IslandController
import com.lagoon.island.service.IslandNotificationListener
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class Permissions(
    val overlay: Boolean = false,
    val notificationAccess: Boolean = false,
) {
    val allGranted: Boolean get() = overlay && notificationAccess
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: SettingsRepository,
    private val controller: IslandController,
) : ViewModel() {

    val settings: StateFlow<IslandSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), IslandSettings())

    val islandState: StateFlow<IslandController.State> = controller.state

    private val _permissions = MutableStateFlow(readPermissions())
    val permissions: StateFlow<Permissions> = _permissions.asStateFlow()

    /** Call from onResume: permissions are granted in system screens we cannot observe. */
    fun refreshPermissions() {
        _permissions.value = readPermissions()
        IslandNotificationListener.requestRebind(context)
        controller.refresh()
    }

    fun update(transform: (IslandSettings) -> IslandSettings) {
        viewModelScope.launch { repository.update(transform) }
    }

    private fun readPermissions() = Permissions(
        overlay = Settings.canDrawOverlays(context),
        notificationAccess = IslandNotificationListener.isEnabled(context),
    )
}
