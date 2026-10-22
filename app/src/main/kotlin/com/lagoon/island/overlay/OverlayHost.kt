package com.lagoon.island.overlay

import android.content.Context
import android.view.View
import android.view.WindowManager

/**
 * Where the island window is attached. Standard mode uses an app overlay window; Enhanced mode
 * (accessibility service) provides a host whose windows sit above the status bar and keyguard.
 */
interface OverlayHost {
    enum class Kind { App, Accessibility }

    val kind: Kind

    /** The WindowManager.LayoutParams.type to use for windows attached through this host. */
    val windowType: Int

    /** A UI context bound to the display this host draws on (for metrics, insets, theming). */
    val windowContext: Context

    fun attach(view: View, params: WindowManager.LayoutParams)
    fun update(view: View, params: WindowManager.LayoutParams)
    fun detach(view: View)
}
