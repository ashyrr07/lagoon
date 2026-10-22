package com.lagoon.island.overlay

import android.graphics.PixelFormat
import android.util.Log
import android.view.Gravity
import android.view.WindowInsets
import android.view.WindowManager
import androidx.compose.runtime.Composable
import com.lagoon.island.cutout.PxRect

/**
 * One overlay window holding one ComposeView. The window is kept as small as the current frame
 * (plus shadow padding) so touches outside it reach the app underneath untouched.
 *
 * Resize discipline: callers change the window rect at most once per transition, never per frame.
 */
class IslandWindow(
    host: OverlayHost,
    /** In-app language tag ("" = phone language); applied to the overlay only below Android 13. */
    private val language: String = "",
    onOutsideTouch: () -> Unit,
) {
    /** Current host; swapped by [rehost] when Enhanced mode connects or disconnects. */
    var host: OverlayHost = host
        private set

    private val owner = OverlayLifecycleOwner()
    private val view = IslandComposeView(localized(host.windowContext, language), onOutsideTouch).also { owner.attachTo(it) }

    private val params = WindowManager.LayoutParams(
        0, 0, 0, 0,
        host.windowType,
        BASE_FLAGS,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        fitInsetsTypes = 0
        title = "Lagoon island"
        // Opt out of the Android 12 untrusted-touch opacity rule by never being a pass-through window.
        alpha = 1f
    }

    /** The window rect currently requested, in screen pixels. */
    var rect: PxRect = PxRect(0, 0, 0, 0)
        private set

    /**
     * Tracked by us rather than read from View.isAttachedToWindow: the view only becomes attached
     * on the first traversal after addView, so the system flag lags behind by one frame.
     */
    var isShowing: Boolean = false
        private set

    fun setContent(content: @Composable () -> Unit) {
        view.content = content
    }

    /** Attach (or move) the window so it covers [newRect]. */
    fun show(newRect: PxRect) {
        rect = newRect
        params.x = newRect.left
        params.y = newRect.top
        params.width = newRect.width.coerceAtLeast(1)
        params.height = newRect.height.coerceAtLeast(1)
        if (isShowing) {
            host.update(view, params)
        } else {
            owner.resume()
            try {
                host.attach(view, params)
                isShowing = true
            } catch (e: WindowManager.BadTokenException) {
                Log.w(TAG, "Cannot attach island window (permission missing?)", e)
                owner.pause()
            }
        }
    }

    /** Let the reply field take the keyboard; everything else stays non-focusable. */
    fun setFocusable(focusable: Boolean) {
        val flags = if (focusable) BASE_FLAGS and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv() else BASE_FLAGS
        if (params.flags != flags) {
            params.flags = flags
            params.softInputMode = if (focusable) {
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING or WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
            } else {
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
            }
            if (isShowing) host.update(view, params)
        }
    }

    fun hide() {
        if (isShowing) host.detach(view)
        isShowing = false
        owner.pause()
    }

    /**
     * Move the window to another host (app overlay <-> accessibility overlay) keeping the same
     * params, rect and content. The composition is rebuilt on re-attach; all island state lives in
     * [com.lagoon.island.ui.island.IslandUiState], so nothing is lost.
     */
    fun rehost(newHost: OverlayHost) {
        if (newHost === host) return
        val wasShowing = isShowing
        if (wasShowing) host.detach(view)
        isShowing = false
        host = newHost
        params.type = newHost.windowType
        // WindowManager stamps its own token onto the params on addView; clear the old host's so the
        // new WindowManager (accessibility token vs. window-context token) can supply its own.
        params.token = null
        if (wasShowing) show(rect)
    }

    fun destroy() {
        hide()
        owner.destroy()
    }

    /** Root insets of the window; used to notice immersive mode and the keyboard. */
    fun rootInsets(): WindowInsets? = view.rootWindowInsets

    companion object {
        private const val TAG = "IslandWindow"
        private const val BASE_FLAGS =
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
    }
}

/**
 * Android 13+ applies Lagoon's per-app language to every context in the process. Android 12 has no
 * per-app languages, so the overlay's view context gets the chosen locale explicitly.
 */
private fun localized(base: android.content.Context, language: String): android.content.Context {
    if (language.isEmpty() || android.os.Build.VERSION.SDK_INT >= 33) return base
    val config = android.content.res.Configuration(base.resources.configuration).apply {
        setLocales(android.os.LocaleList.forLanguageTags(language))
    }
    return android.view.ContextThemeWrapper(base, 0).apply { applyOverrideConfiguration(config) }
}
