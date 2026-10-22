package com.lagoon.island.ui.app.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lagoon.island.data.AppsRepository
import com.lagoon.island.data.IslandSettings
import com.lagoon.island.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Backs [AppsScreen]: the launchable-app list plus the two per-app sets in settings. */
@HiltViewModel
class AppsViewModel @Inject constructor(
    appsRepository: AppsRepository,
    private val repository: SettingsRepository,
) : ViewModel() {

    /** Null while loading. */
    val apps: StateFlow<List<AppsRepository.AppInfo>?> = flow { emit(appsRepository.launchableApps()) }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val settings: StateFlow<IslandSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), IslandSettings())

    /** Notification allowlist (`features.notificationApps`). */
    fun setNotificationsAllowed(packageName: String, allowed: Boolean) = viewModelScope.launch {
        repository.update { s ->
            val apps = if (allowed) s.features.notificationApps + packageName else s.features.notificationApps - packageName
            s.copy(features = s.features.copy(notificationApps = apps))
        }
    }

    /** Island blacklist (`visibility.hiddenInApps`). */
    fun setIslandHidden(packageName: String, hidden: Boolean) = viewModelScope.launch {
        repository.update { s ->
            val apps = if (hidden) s.visibility.hiddenInApps + packageName else s.visibility.hiddenInApps - packageName
            s.copy(visibility = s.visibility.copy(hiddenInApps = apps))
        }
    }
}
