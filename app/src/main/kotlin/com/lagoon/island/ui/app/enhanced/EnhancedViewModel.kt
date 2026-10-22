package com.lagoon.island.ui.app.enhanced

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lagoon.island.BuildConfig
import com.lagoon.island.data.EnhancedSettings
import com.lagoon.island.data.SettingsRepository
import com.lagoon.island.service.EnhancedModeManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** State for the Enhanced-mode disclosure screen. */
data class EnhancedStatus(
    /** False in the Play flavor until the accessibility declaration is approved. */
    val available: Boolean = BuildConfig.ENHANCED_MODE_AVAILABLE,
    /** The service is switched on in Accessibility settings (it may still be binding). */
    val enabledInSettings: Boolean = false,
    /** The service is bound and hosting the island. */
    val connected: Boolean = false,
    /** Non-store install: Android 13+ needs "Allow restricted settings" first. */
    val sideloaded: Boolean = false,
)

@HiltViewModel
class EnhancedViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: SettingsRepository,
    private val manager: EnhancedModeManager,
) : ViewModel() {

    val settings: StateFlow<EnhancedSettings> = repository.settings
        .map { it.enhanced }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EnhancedSettings())

    private val systemStatus = MutableStateFlow(readStatus())

    val status: StateFlow<EnhancedStatus> = combine(systemStatus, manager.host) { status, host ->
        status.copy(connected = host != null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), systemStatus.value)

    /** Call from onResume: the switch lives in a system screen we cannot observe. */
    fun refresh() {
        systemStatus.value = readStatus()
    }

    fun setConsented(consented: Boolean) = update { it.copy(consented = consented) }

    fun setShowOnLockScreen(show: Boolean) = update { it.copy(showOnLockScreen = show) }

    fun openAccessibilitySettings() = manager.openAccessibilitySettings(context)

    fun openAppInfo() = manager.openAppInfo(context)

    private fun update(transform: (EnhancedSettings) -> EnhancedSettings) {
        viewModelScope.launch { repository.update { it.copy(enhanced = transform(it.enhanced)) } }
    }

    private fun readStatus() = EnhancedStatus(
        enabledInSettings = BuildConfig.ENHANCED_MODE_AVAILABLE && manager.isServiceEnabled(context),
        connected = manager.isConnected,
        sideloaded = manager.isSideloaded(context),
    )
}
