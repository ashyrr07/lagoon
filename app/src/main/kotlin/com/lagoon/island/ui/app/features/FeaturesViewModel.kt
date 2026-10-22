package com.lagoon.island.ui.app.features

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lagoon.island.data.FeatureSettings
import com.lagoon.island.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Runtime permissions some sources depend on; granted from this screen, never required. */
data class SourcePermissions(
    val bluetooth: Boolean = false,
    val phoneState: Boolean = false,
)

@HiltViewModel
class FeaturesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: SettingsRepository,
) : ViewModel() {

    val features: StateFlow<FeatureSettings> = repository.settings
        .map { it.features }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FeatureSettings())

    private val _permissions = MutableStateFlow(readPermissions())
    val permissions: StateFlow<SourcePermissions> = _permissions.asStateFlow()

    fun update(transform: (FeatureSettings) -> FeatureSettings) {
        viewModelScope.launch { repository.update { it.copy(features = transform(it.features)) } }
    }

    fun refreshPermissions() {
        _permissions.value = readPermissions()
    }

    private fun readPermissions() = SourcePermissions(
        bluetooth = granted(Manifest.permission.BLUETOOTH_CONNECT) && granted(Manifest.permission.BLUETOOTH_SCAN),
        phoneState = granted(Manifest.permission.READ_PHONE_STATE),
    )

    private fun granted(permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    companion object {
        val BLUETOOTH_PERMISSIONS = arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
        val PHONE_PERMISSIONS = arrayOf(Manifest.permission.READ_PHONE_STATE)
    }
}
