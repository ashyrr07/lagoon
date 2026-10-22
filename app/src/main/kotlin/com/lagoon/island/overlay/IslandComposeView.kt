package com.lagoon.island.overlay

import android.content.Context
import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.AbstractComposeView

/** ComposeView that reports touches landing outside the window (FLAG_WATCH_OUTSIDE_TOUCH). */
class IslandComposeView(
    context: Context,
    private val onOutsideTouch: () -> Unit,
) : AbstractComposeView(context) {

    var content: (@Composable () -> Unit)? by mutableStateOf(null)

    @Composable
    override fun Content() {
        content?.invoke()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_OUTSIDE) {
            onOutsideTouch()
            return true
        }
        return super.dispatchTouchEvent(ev)
    }
}
