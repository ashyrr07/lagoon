package com.lagoon.island.ui.island

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.dp
import com.lagoon.island.engine.IslandActivity
import com.lagoon.island.engine.SwipeDirection

/** What a touch landed on. */
sealed interface IslandHit {
    data object Outside : IslandHit
    /** The attached pill (idle, compact, alert) or the detached bubble; [activity] is what is under the finger. */
    data class Pill(val activity: IslandActivity?) : IslandHit
    /** An expanded card or the control deck: children handle their own clicks. */
    data object Card : IslandHit
}

/** Everything the pill reports; the engine decides what the gestures mean. */
class IslandGestureCallbacks(
    val onTap: (IslandActivity?) -> Unit,
    val onLongPress: (IslandActivity?) -> Unit,
    val onSwipe: (SwipeDirection, IslandActivity?) -> Unit,
    val onOutsideTouch: () -> Unit,
    val onInteraction: () -> Unit,
)

/**
 * One pointer handler for the whole window. Touches on the pill are owned here (tap, long press,
 * swipe by dominant axis on release); touches on a card are only observed in the initial pass so
 * buttons inside keep working, and a drag up beyond 40 dp collapses it.
 */
fun Modifier.islandGestures(
    hitTest: (Offset) -> IslandHit,
    callbacks: IslandGestureCallbacks,
): Modifier = pointerInput(hitTest, callbacks) {
    val slop = viewConfiguration.touchSlop
    val longPressMs = viewConfiguration.longPressTimeoutMillis
    val collapseDrag = 40.dp.toPx()
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        when (val hit = hitTest(down.position)) {
            IslandHit.Outside -> callbacks.onOutsideTouch()
            IslandHit.Card -> {
                callbacks.onInteraction()
                val total = observeUntilUp(down.id, PointerEventPass.Initial, consume = false)
                if (total != null && -total.y >= collapseDrag && -total.y > kotlin.math.abs(total.x)) {
                    callbacks.onSwipe(SwipeDirection.Up, null)
                }
            }
            is IslandHit.Pill -> {
                down.consume()
                handlePill(down.id, hit.activity, slop, longPressMs, callbacks)
            }
        }
    }
}

/** Track [id] until it lifts; returns the total drag, or null when the pointer was cancelled. */
private suspend fun AwaitPointerEventScope.observeUntilUp(
    id: PointerId,
    pass: PointerEventPass,
    consume: Boolean,
): Offset? {
    var total = Offset.Zero
    while (true) {
        val event = awaitPointerEvent(pass)
        val change = event.changes.firstOrNull { it.id == id } ?: return null
        total += change.positionChange()
        if (consume) change.consume()
        if (!change.pressed) return total
    }
}

private suspend fun AwaitPointerEventScope.handlePill(
    id: PointerId,
    activity: IslandActivity?,
    slop: Float,
    longPressMs: Long,
    callbacks: IslandGestureCallbacks,
) {
    var total = Offset.Zero
    var lifted = false
    var cancelled = false
    // Phase 1: until the long-press timeout, unless the finger lifts or leaves the slop first.
    val movedBeyondSlop = withTimeoutOrNull(longPressMs) {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Main)
            val change = event.changes.firstOrNull { it.id == id }
            if (change == null) { cancelled = true; return@withTimeoutOrNull false }
            total += change.positionChange()
            change.consume()
            if (!change.pressed) { lifted = true; return@withTimeoutOrNull false }
            if (GestureMath.classifySwipe(total.x, total.y, slop) != null) return@withTimeoutOrNull true
        }
        @Suppress("UNREACHABLE_CODE") false
    }
    when {
        cancelled -> return
        lifted -> callbacks.onTap(activity)
        movedBeyondSlop == null -> {
            // Long press: report now, then swallow the rest of the gesture.
            callbacks.onLongPress(activity)
            consumeUntilUp(id)
        }
        else -> {
            // Phase 2: a drag; classify by the dominant axis when the finger lifts.
            val rest = observeUntilUp(id, PointerEventPass.Main, consume = true) ?: return
            total += rest
            GestureMath.classifySwipe(total.x, total.y, slop)?.let { callbacks.onSwipe(it, activity) }
        }
    }
}

private suspend fun AwaitPointerEventScope.consumeUntilUp(id: PointerId) {
    while (true) {
        val event = awaitPointerEvent(PointerEventPass.Main)
        val change: PointerInputChange = event.changes.firstOrNull { it.id == id } ?: return
        change.consume()
        if (!change.pressed) return
    }
}
