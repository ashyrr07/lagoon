package com.lagoon.island.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import com.lagoon.island.controls.GlobalActions
import com.lagoon.island.overlay.AccessibilityOverlayHost
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Enhanced mode. Declared only in the flavors whose store listing allows it (see
 * src/foss/AndroidManifest.xml) and configured by res/xml/accessibility_service_config.xml:
 * no window content, no interactive windows, no gestures, window-state events only.
 *
 * It does three things and nothing else:
 *  1. hands the controller an [AccessibilityOverlayHost] so the island window can sit above the
 *     status bar and the lock screen;
 *  2. publishes the foreground package from TYPE_WINDOW_STATE_CHANGED (package name only);
 *  3. performs screenshot / lock / recents through [AccessibilityActions] when a tile asks.
 *
 * When the system unbinds it, the manager is cleared and the controller falls back to the app
 * overlay window without losing island state.
 */
@AndroidEntryPoint
class IslandAccessibilityService : AccessibilityService() {

    @Inject lateinit var manager: EnhancedModeManager
    @Inject lateinit var actions: AccessibilityActions
    @Inject lateinit var globalActions: GlobalActions

    private var host: AccessibilityOverlayHost? = null
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        val host = AccessibilityOverlayHost(this).also { host = it }
        actions.bind(this)
        manager.onServiceConnected(host)
        globalActions.delegate = object : GlobalActions.Delegate {
            // Let the deck's collapse animation finish before the capture.
            override fun screenshot() { mainHandler.postDelayed({ actions.takeScreenshot() }, 350) }
            override fun lock() { actions.lockScreen() }
            override fun recents() { actions.openRecents() }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString()?.takeIf { it.isNotBlank() } ?: return
        if (pkg == packageName || pkg == SYSTEM_UI || pkg in imePackages()) return
        manager.onForegroundPackage(pkg)
    }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        release()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        release()
        super.onDestroy()
    }

    private fun release() {
        globalActions.delegate = null
        host?.let { manager.onServiceDisconnected(it) }
        host = null
        actions.unbind(this)
    }

    /** Keyboards also raise window-state changes; they are never the "foreground app". */
    private fun imePackages(): Set<String> {
        val now = System.currentTimeMillis()
        if (now - imeCacheAt > IME_CACHE_MS) {
            imeCache = runCatching {
                getSystemService(InputMethodManager::class.java)
                    ?.enabledInputMethodList
                    ?.map { it.packageName }
                    ?.toSet()
            }.getOrNull().orEmpty()
            imeCacheAt = now
        }
        return imeCache
    }

    private var imeCache: Set<String> = emptySet()
    private var imeCacheAt = 0L

    private companion object {
        const val SYSTEM_UI = "com.android.systemui"
        const val IME_CACHE_MS = 60_000L
    }
}
