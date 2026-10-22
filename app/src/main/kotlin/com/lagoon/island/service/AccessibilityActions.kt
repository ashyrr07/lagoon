package com.lagoon.island.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The three global actions Enhanced mode offers (screenshot, lock, recents). They go through
 * [AccessibilityService.performGlobalAction] and need nothing beyond the bound service: no
 * screen content, no gestures. [available] is false whenever the service is not connected, so
 * tiles can grey out instead of failing.
 */
@Singleton
class AccessibilityActions @Inject constructor() {
    private var service: AccessibilityService? = null

    private val _available = MutableStateFlow(false)
    val available: StateFlow<Boolean> = _available.asStateFlow()

    internal fun bind(service: AccessibilityService) {
        this.service = service
        _available.value = true
    }

    internal fun unbind(service: AccessibilityService) {
        if (this.service === service) {
            this.service = null
            _available.value = false
        }
    }

    fun takeScreenshot(): Boolean = perform(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT, "screenshot")

    fun lockScreen(): Boolean = perform(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN, "lock")

    fun openRecents(): Boolean = perform(AccessibilityService.GLOBAL_ACTION_RECENTS, "recents")

    private fun perform(action: Int, name: String): Boolean {
        val s = service ?: run {
            Log.w(TAG, "Global action '$name' requested without Enhanced mode")
            return false
        }
        return runCatching { s.performGlobalAction(action) }
            .onFailure { Log.w(TAG, "Global action '$name' failed", it) }
            .getOrDefault(false)
    }

    private companion object {
        const val TAG = "AccessibilityActions"
    }
}
