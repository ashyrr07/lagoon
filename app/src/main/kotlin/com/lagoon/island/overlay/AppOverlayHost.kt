package com.lagoon.island.overlay

import android.content.Context
import android.hardware.display.DisplayManager
import android.view.Display
import android.view.View
import android.view.WindowManager

/** Standard mode: TYPE_APPLICATION_OVERLAY windows, which need "Display over other apps". */
class AppOverlayHost(appContext: Context) : OverlayHost {
    override val kind = OverlayHost.Kind.App
    override val windowType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

    override val windowContext: Context = run {
        val display: Display = appContext.getSystemService(DisplayManager::class.java)
            .getDisplay(Display.DEFAULT_DISPLAY)
        appContext.createDisplayContext(display)
            .createWindowContext(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, null)
    }

    private val windowManager: WindowManager = windowContext.getSystemService(WindowManager::class.java)

    override fun attach(view: View, params: WindowManager.LayoutParams) = windowManager.addView(view, params)
    override fun update(view: View, params: WindowManager.LayoutParams) = windowManager.updateViewLayout(view, params)
    override fun detach(view: View) {
        runCatching { windowManager.removeViewImmediate(view) }
    }
}
