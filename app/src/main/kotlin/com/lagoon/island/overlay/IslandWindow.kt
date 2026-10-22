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
    private val host: OverlayHost,
    onOutsideTouch: () -> Unit,
) {
    private val owner = OverlayLifecycleOwner()
    private val view = IslandComposeView(host.windowContext, onOutsideTouch).also { owner.attachTo(it) }

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

    val isShowing: Boolean get() = view.isAttachedToWindow

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
        if (view.isAttachedToWindow) {
            host.update(view, params)
        } else {
            owner.resume()
            try {
                host.attach(view, params)
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
            if (view.isAttachedToWindow) host.update(view, params)
        }
    }

    fun hide() {
        if (view.isAttachedToWindow) host.detach(view)
        owner.pause()
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
