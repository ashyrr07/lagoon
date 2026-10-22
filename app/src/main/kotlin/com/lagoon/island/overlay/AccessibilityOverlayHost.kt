package com.lagoon.island.overlay

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.util.Log
import android.view.View
import android.view.WindowManager

/**
 * Enhanced mode: TYPE_ACCESSIBILITY_OVERLAY windows attached through the accessibility service's
 * WindowManager. They sit above the status bar and the keyguard, so the whole pill is tappable and
 * the island can be shown on the lock screen. No "Display over other apps" permission is needed.
 *
 * The service is a UI context on API 30+ (it has a display and a window token), so it serves as
 * [windowContext] directly. The WindowManager it hands out carries the accessibility window token
 * and assigns it to any LayoutParams whose token is null.
 */
class AccessibilityOverlayHost(service: AccessibilityService) : OverlayHost {
    override val kind = OverlayHost.Kind.Accessibility
    override val windowType = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
    override val windowContext: Context = service

    private val windowManager: WindowManager = service.getSystemService(WindowManager::class.java)

    override fun attach(view: View, params: WindowManager.LayoutParams) = windowManager.addView(view, params)
    override fun update(view: View, params: WindowManager.LayoutParams) = windowManager.updateViewLayout(view, params)

    override fun detach(view: View) {
        // When the service is being unbound the system may already have torn the window down.
        runCatching { windowManager.removeViewImmediate(view) }
            .onFailure { Log.w(TAG, "Island window already gone", it) }
    }

    private companion object {
        const val TAG = "AccessibilityOverlayHost"
    }
}
