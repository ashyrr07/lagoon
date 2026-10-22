package com.lagoon.island.ui.island

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import com.lagoon.island.cutout.PxRect
import com.lagoon.island.ui.island.FrameMath.Companion.containsRect
import com.lagoon.island.ui.island.FrameMath.Companion.toPxRect
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** Motion specs shared by the frame and the content. */
object IslandMotion {
    const val FRAME_DAMPING = 0.72f
    const val FRAME_STIFFNESS = 380f
    const val CONTENT_DAMPING = Spring.DampingRatioNoBouncy
    const val CONTENT_STIFFNESS = 600f
    const val REDUCED_MS = 120
    const val TINT_FADE_MS = 400

    fun frameRect(reduceMotion: Boolean): FiniteAnimationSpec<Rect> =
        if (reduceMotion) tween(REDUCED_MS, easing = FastOutSlowInEasing)
        else spring(FRAME_DAMPING, FRAME_STIFFNESS, visibilityThreshold = Rect(0.5f, 0.5f, 0.5f, 0.5f))

    fun frameFloat(reduceMotion: Boolean): FiniteAnimationSpec<Float> =
        if (reduceMotion) tween(REDUCED_MS, easing = FastOutSlowInEasing)
        else spring(FRAME_DAMPING, FRAME_STIFFNESS, visibilityThreshold = 0.5f)

    fun content(reduceMotion: Boolean): FiniteAnimationSpec<Float> =
        if (reduceMotion) tween(REDUCED_MS, easing = FastOutSlowInEasing)
        else spring(CONTENT_DAMPING, CONTENT_STIFFNESS, visibilityThreshold = 0.005f)
}

/**
 * The animated island geometry. Every value is an [Animatable] read from draw / layer lambdas,
 * so a running animation never recomposes anything. [target] is written by the layout that
 * measures the content; [drive] animates toward it and runs the window-size protocol.
 */
@Stable
class IslandFrameState(initial: FrameSpec) {
    val pill = Animatable(initial.pill, Rect.VectorConverter)
    val cornerRadius = Animatable(initial.cornerRadius)
    val bubble = Animatable(initial.bubble ?: collapsedBubble(initial.pill), Rect.VectorConverter)
    val bubbleAlpha = Animatable(if (initial.bubble != null) 1f else 0f)
    /** 0 = compact look (no shadow), 1 = card look (soft shadow). */
    val elevation = Animatable(if (initial.kind.isCard) 1f else 0f)
    /** Horizontal "inhale" overshoot on arrival of a new activity. */
    val inhale = Animatable(1f)
    /** Uniform pulse for alerts. */
    val pulse = Animatable(1f)

    var target: FrameSpec by mutableStateOf(initial)
    var reduceMotion: Boolean by mutableStateOf(false)

    /** The on-screen bounds right now (pill plus the bubble while it is visible). */
    fun currentBounds(): Rect =
        if (bubbleAlpha.value > 0.01f) FrameSpec.union(pill.value, bubble.value) else pill.value

    /** Animate everything toward [spec]; growing the window first and fitting it afterwards. */
    suspend fun animateTo(spec: FrameSpec, onRequestWindow: (PxRect) -> Unit, onSettled: (PxRect) -> Unit) {
        val needed = spec.bounds
        val current = currentBounds()
        if (!current.containsRect(needed)) {
            onRequestWindow(FrameSpec.union(current, needed).toPxRect())
        }
        val reduce = reduceMotion
        coroutineScope {
            launch { pill.animateTo(spec.pill, IslandMotion.frameRect(reduce)) }
            launch { cornerRadius.animateTo(spec.cornerRadius, IslandMotion.frameFloat(reduce)) }
            launch { elevation.animateTo(if (spec.kind.isCard) 1f else 0f, IslandMotion.frameFloat(reduce)) }
            launch {
                val b = spec.bubble
                if (b != null) {
                    if (bubbleAlpha.value == 0f) bubble.snapTo(collapsedBubble(spec.pill, b))
                    launch { bubble.animateTo(b, IslandMotion.frameRect(reduce)) }
                    bubbleAlpha.animateTo(1f, IslandMotion.content(reduce))
                } else {
                    launch { bubble.animateTo(collapsedBubble(spec.pill, bubble.value), IslandMotion.frameRect(reduce)) }
                    bubbleAlpha.animateTo(0f, IslandMotion.content(reduce))
                }
            }
        }
        onSettled(needed.toPxRect())
    }

    suspend fun snapTo(spec: FrameSpec) {
        pill.snapTo(spec.pill)
        cornerRadius.snapTo(spec.cornerRadius)
        elevation.snapTo(if (spec.kind.isCard) 1f else 0f)
        bubble.snapTo(spec.bubble ?: collapsedBubble(spec.pill))
        bubbleAlpha.snapTo(if (spec.bubble != null) 1f else 0f)
    }

    /** Brief scaleX overshoot (~1.04) then settle; no-op with reduced motion. */
    suspend fun playInhale() {
        if (reduceMotion) return
        inhale.snapTo(1f)
        inhale.animateTo(1.04f, tween(90, easing = FastOutSlowInEasing))
        inhale.animateTo(1f, spring(0.55f, 420f))
    }

    suspend fun playPulse() {
        if (reduceMotion) return
        pulse.snapTo(1f)
        pulse.animateTo(1.06f, tween(110, easing = FastOutSlowInEasing))
        pulse.animateTo(1f, spring(0.6f, 380f))
    }

    companion object {
        /** A zero-size circle tucked at the pill's trailing edge: where a bubble grows from / shrinks to. */
        fun collapsedBubble(pill: Rect, near: Rect? = null): Rect {
            val cy = pill.top + pill.height / 2f
            val cx = if (near != null && near.center.x < pill.center.x) pill.left else pill.right
            return Rect(cx, cy, cx, cy)
        }
    }
}

val FrameKind.isCard: Boolean get() = this == FrameKind.Expanded || this == FrameKind.Deck

@Composable
fun rememberIslandFrameState(initial: FrameSpec): IslandFrameState = remember { IslandFrameState(initial) }
