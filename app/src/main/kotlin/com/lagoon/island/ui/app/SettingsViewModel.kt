package com.lagoon.island.ui.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lagoon.island.data.IslandSettings
import com.lagoon.island.data.SettingsRepository
import com.lagoon.island.service.IslandController
import com.lagoon.island.service.IslandNotificationListener
import com.lagoon.island.service.KeepAliveService
import com.lagoon.island.util.Oem
import com.lagoon.island.util.OemInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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

/** Everything granted in system screens we cannot observe, re-read on every resume. */
data class Permissions(
    val overlay: Boolean = false,
    val notificationAccess: Boolean = false,
    val postNotifications: Boolean = true,
    /** Android's own "unrestricted" battery state for this app. */
    val ignoringBatteryOptimizations: Boolean = false,
    /** Installed by hand (GitHub APK) rather than a store: Android 13+ restricted settings apply. */
    val sideloaded: Boolean = false,
    val oem: Oem = Oem.Other,
) {
    val allGranted: Boolean get() = overlay && notificationAccess
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: SettingsRepository,
    private val controller: IslandController,
    enhanced: com.lagoon.island.service.EnhancedModeManager,
) : ViewModel() {

    /** True while the accessibility overlay hosts the island (taps work over the status bar too). */
    val enhancedActive: StateFlow<Boolean> = enhanced.host
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), enhanced.host.value != null)

    val settings: StateFlow<IslandSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), IslandSettings())

    /** Null until the store has been read once, so the start screen is chosen from real data. */
    val onboardingDone: StateFlow<Boolean?> = repository.settings
        .map { it.onboardingDone }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

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

    fun finishOnboarding() = update { it.copy(onboardingDone = true) }

    /** The keep-alive foreground service: persisted so [com.lagoon.island.service.BootReceiver] restarts it. */
    fun setKeepAliveService(enabled: Boolean) {
        if (enabled) KeepAliveService.start(context) else KeepAliveService.stop(context)
        update { it.copy(keepAlive = it.keepAlive.copy(foregroundService = enabled)) }
    }

    suspend fun exportTo(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val json = repository.exportJson(settings.value)
            context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(json.encodeToByteArray()) }
                ?: error("Cannot open the file")
        }
    }

    suspend fun importFrom(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val json = context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
                ?: error("Cannot read the file")
            repository.importJson(json)
            Unit
        }
    }

    private fun readPermissions(): Permissions {
        val power = context.getSystemService(PowerManager::class.java)
        return Permissions(
            overlay = Settings.canDrawOverlays(context),
            notificationAccess = IslandNotificationListener.isEnabled(context),
            postNotifications = Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED,
            ignoringBatteryOptimizations = power?.isIgnoringBatteryOptimizations(context.packageName) ?: false,
            sideloaded = isSideloaded(),
            oem = OemInfo.current,
        )
    }

    private fun isSideloaded(): Boolean {
        val initiating = runCatching {
            context.packageManager.getInstallSourceInfo(context.packageName).initiatingPackageName
        }.getOrNull() ?: return true
        return initiating !in STORES
    }

    companion object {
        private val STORES = setOf(
            "com.android.vending",
            "org.fdroid.fdroid",
            "org.fdroid.basic",
            "com.aurora.store",
            "com.sec.android.app.samsungapps",
            "com.huawei.appmarket",
            "com.amazon.venezia",
            "com.xiaomi.market",
            "com.heytap.market",
            "com.oppo.market",
            "com.bbk.appstore",
        )
    }
}
