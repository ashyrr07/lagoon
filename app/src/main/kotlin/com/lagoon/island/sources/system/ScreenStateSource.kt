package com.lagoon.island.sources.system

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.lagoon.island.sources.IslandSource
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ScreenState(
    val screenOn: Boolean = true,
    val keyguardLocked: Boolean = false,
) {
    /** True when an app overlay can be seen: screen on and the keyguard dismissed. */
    val overlayVisible: Boolean get() = screenOn && !keyguardLocked
}

/**
 * Screen on/off and keyguard state for the visibility policy. Posts nothing to the registry; the
 * integrator collects [state] and feeds `engine.setVisible(...)`.
 */
@Singleton
class ScreenStateSource @Inject constructor(
    @ApplicationContext private val context: Context,
) : IslandSource {
    private val receivers = Receivers(context)
    private val _state = MutableStateFlow(read())
    val state: StateFlow<ScreenState> = _state.asStateFlow()

    override fun start(scope: CoroutineScope) {
        _state.value = read()
        receivers.register(Intent.ACTION_SCREEN_ON, Intent.ACTION_SCREEN_OFF, Intent.ACTION_USER_PRESENT) { intent ->
            _state.value = when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> ScreenState(screenOn = false, keyguardLocked = keyguardLocked() || deviceSecure())
                Intent.ACTION_SCREEN_ON -> ScreenState(screenOn = true, keyguardLocked = keyguardLocked())
                Intent.ACTION_USER_PRESENT -> ScreenState(screenOn = true, keyguardLocked = false)
                else -> read()
            }
        }
    }

    override fun stop() {
        receivers.unregisterAll()
    }

    /** Re-reads the platform state; useful after the process was idle. */
    fun refresh() {
        _state.value = read()
    }

    private fun read(): ScreenState {
        val power = context.getSystemService(PowerManager::class.java)
        val on = power?.isInteractive ?: true
        return ScreenState(screenOn = on, keyguardLocked = keyguardLocked())
    }

    private fun keyguardLocked(): Boolean =
        context.getSystemService(KeyguardManager::class.java)?.isKeyguardLocked == true

    /** With a secure lock the keyguard engages on screen-off even if the manager lags behind. */
    private fun deviceSecure(): Boolean =
        context.getSystemService(KeyguardManager::class.java)?.isDeviceSecure == true
}
