package com.lagoon.island.cutout

import android.content.Context
import android.view.Surface
import android.view.WindowInsets
import android.view.WindowManager

/** Reads the display cutout and status bar from a window context (API 30+ WindowMetrics). */
class CutoutDetector(private val windowContext: Context) {

    fun detect(): CutoutInfo {
        val wm = windowContext.getSystemService(WindowManager::class.java)
        val metrics = wm.currentWindowMetrics
        val bounds = metrics.bounds
        val insets = metrics.windowInsets
        val density = windowContext.resources.displayMetrics.density
        val rotation = runCatching { windowContext.display.rotation }.getOrDefault(Surface.ROTATION_0)

        val cutoutRect = insets.displayCutout?.boundingRectTop
            ?.takeIf { !it.isEmpty }
            ?.let { PxRect(it.left, it.top, it.right, it.bottom) }

        var statusBar = insets.getInsets(WindowInsets.Type.statusBars()).top
        if (statusBar <= 0) statusBar = fallbackStatusBarHeight(density)

        return CutoutInfo(
            cutout = cutoutRect,
            shape = CutoutInfo.classify(cutoutRect, bounds.width()),
            statusBarHeight = statusBar,
            screenWidth = bounds.width(),
            screenHeight = bounds.height(),
            rotation = rotation,
            density = density,
        )
    }

    @Suppress("DiscouragedApi")
    private fun fallbackStatusBarHeight(density: Float): Int {
        val res = windowContext.resources
        val id = res.getIdentifier("status_bar_height", "dimen", "android")
        return if (id > 0) res.getDimensionPixelSize(id) else (24 * density).toInt()
    }
}
