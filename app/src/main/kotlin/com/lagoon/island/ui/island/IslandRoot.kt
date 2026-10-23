package com.lagoon.island.ui.island

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Density
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.lagoon.island.engine.ActivityContent
import com.lagoon.island.engine.IslandActivity
import com.lagoon.island.engine.IslandPresentation
import com.lagoon.island.ui.island.content.Accent
import com.lagoon.island.ui.island.content.BubbleGlyph
import com.lagoon.island.ui.island.content.CompactAroundCamera
import com.lagoon.island.ui.island.content.CompactLeading
import com.lagoon.island.ui.island.content.CompactTrailing
import com.lagoon.island.ui.island.content.ContentScope
import com.lagoon.island.ui.island.content.ExpandedContent
import com.lagoon.island.ui.island.content.hasCameraDecoration
import com.lagoon.island.ui.island.deck.ControlDeck
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.collectLatest

/** What the content layer shows; the key decides when content cross-fades. */
@Immutable
data class ContentSlot(val kind: FrameKind, val primary: IslandActivity?, val secondary: IslandActivity?) {
    val key: String get() = "$kind:${primary?.id}"

    companion object {
        fun of(p: IslandPresentation): ContentSlot = when (p) {
            IslandPresentation.Hidden, IslandPresentation.Idle -> ContentSlot(FrameKind.Idle, null, null)
            is IslandPresentation.Compact -> ContentSlot(FrameKind.Compact, p.primary, p.secondary)
            is IslandPresentation.Alert -> ContentSlot(FrameKind.Alert, p.activity, null)
            is IslandPresentation.Expanded -> ContentSlot(FrameKind.Expanded, p.activity, null)
            IslandPresentation.ControlDeck -> ContentSlot(FrameKind.Deck, null, null)
        }
    }
}

/**
 * The island. Geometry lives in [IslandFrameState] (animated, read from draw and layer lambdas);
 * content is laid out at its final position around the camera and revealed by the pill's clip.
 */
@Composable
fun IslandRoot(state: IslandUiState) {
    val idle = state.idle ?: return
    val presentation = state.presentation
    if (presentation is IslandPresentation.Hidden) return

    val density = LocalDensity.current
    val appearance = state.settings.appearance
    val screenWidth = state.cutout?.screenWidth ?: maxOf(state.windowRect.right, idle.pill.right + idle.pill.left)
    val statusBarTappable = state.hostKind == com.lagoon.island.overlay.OverlayHost.Kind.Accessibility
    val math = remember(idle, screenWidth, density.density, appearance.scale, statusBarTappable) {
        FrameMath(idle, screenWidth, density.density, appearance.scale, statusBarTappable)
    }
    val frame = remember { IslandFrameState(math.idle()) }
    val slot = remember(presentation) { ContentSlot.of(presentation) }
    val currentSlot = rememberUpdatedState(slot)
    val smoothing = appearance.cornerSmoothing

    SideEffect {
        frame.reduceMotion = state.reduceMotion
        if (slot.kind == FrameKind.Idle) frame.target = math.idle()
    }

    // Frame animation + window-size protocol, restarted whenever the target changes.
    LaunchedEffect(frame) {
        snapshotFlow { frame.target }.collectLatest { target ->
            frame.animateTo(target, state.onRequestWindow, state.onFrameSettled)
        }
    }
    // A notification arriving on a resting island drops in from the top; a new activity replacing
    // one already shown just "breathes" so the island doesn't jump around while in use.
    var lastPrimaryId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(slot.primary?.id) {
        val id = slot.primary?.id
        val wasResting = lastPrimaryId == null
        lastPrimaryId = id
        if (id != null) {
            if (wasResting) frame.playDrop() else frame.playInhale()
        }
    }
    LaunchedEffect(slot.kind, slot.primary?.updatedAt) { if (slot.kind == FrameKind.Alert) frame.playPulse() }

    val view = LocalView.current
    val haptics = appearance.haptics
    LaunchedEffect(slot.kind.isCard) {
        if (slot.kind.isCard && haptics) view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
    }

    val style = rememberSurfaceStyle(appearance, slot.primary)
    val glowAlpha = animateFloatAsState(
        if (style.value.glow != null) 1f else 0f,
        tween(if (state.reduceMotion) IslandMotion.REDUCED_MS else IslandMotion.TINT_FADE_MS),
        label = "glow",
    )
    val artColor by rememberArtColor((slot.primary?.content as? ActivityContent.Media)?.art)

    val windowOrigin = remember(state) { { Offset(state.windowRect.left.toFloat(), state.windowRect.top.toFloat()) } }
    val hitTest = remember(frame, windowOrigin) {
        { pos: Offset ->
            val screen = pos + windowOrigin()
            val s = currentSlot.value
            when {
                frame.target.kind.isCard -> if (frame.pill.value.contains(screen)) IslandHit.Card else IslandHit.Outside
                frame.bubbleAlpha.value > 0.5f && frame.bubble.value.contains(screen) -> IslandHit.Pill(s.secondary)
                frame.pill.value.contains(screen) -> IslandHit.Pill(s.primary)
                else -> IslandHit.Outside
            }
        }
    }
    val callbacks = remember(state) {
        IslandGestureCallbacks(
            onTap = { state.onTap(it) },
            onLongPress = { state.onLongPress(it) },
            onSwipe = { d, a -> state.onSwipe(d, a) },
            onOutsideTouch = { state.onOutsideTouch() },
            onInteraction = { state.onInteraction() },
        )
    }

    fun scopeFor(activity: IslandActivity, kind: FrameKind) = ContentScope(
        activity = activity,
        accent = Accent.of(activity, artColor),
        reduceMotion = state.reduceMotion,
        alert = kind == FrameKind.Alert,
        act = { action, text ->
            if (haptics) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            state.onAction(activity, action, text)
        },
        requestFocus = state.onRequestFocus,
    )

    val pivot = remember(state, math, idle) {
        {
            val w = state.windowRect
            val px = if (w.width > 0) ((math.camera.center.x - w.left) / w.width).coerceIn(0f, 1f) else 0.5f
            val py = if (w.height > 0) ((idle.pill.centerY - w.top).toFloat() / w.height).coerceIn(0f, 1f) else 0f
            TransformOrigin(px, py)
        }
    }

    Box(Modifier.fillMaxSize().islandGestures(hitTest, callbacks)) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // Drop-in: from a small capsule tucked above the camera down to full size. Pivot
                    // on the top centre so it grows down out of the screen edge, not out of thin air.
                    // The spring overshoots past 1 on landing; that overshoot becomes a short vertical
                    // squash, so the island never grows past its final size (its window is exactly
                    // that size and would clip it).
                    val d = frame.drop.value
                    val p = d.coerceAtMost(1f)
                    val squash = (d - 1f).coerceAtLeast(0f)
                    translationY = -(1f - p) * math.compactHeight * 0.9f
                    scaleX = 0.55f + 0.45f * p
                    scaleY = (0.4f + 0.6f * p - 0.6f * squash).coerceAtLeast(0.7f)
                    alpha = (d * 2.5f).coerceIn(0f, 1f)
                    transformOrigin = TransformOrigin(pivot().pivotFractionX, 0f)
                }
                .graphicsLayer {
                    scaleX = frame.inhale.value * frame.pulse.value
                    scaleY = frame.pulse.value
                    transformOrigin = pivot()
                }
                .islandSurface(frame, style, glowAlpha, smoothing, windowOrigin),
        ) {
            Box(Modifier.fillMaxSize().clipToPill(frame, smoothing, windowOrigin)) {
                val reduce = state.reduceMotion
                AnimatedContent(
                    targetState = slot,
                    contentKey = { it.key },
                    transitionSpec = {
                        val spec = IslandMotion.content(reduce)
                        (fadeIn(spec) + scaleIn(spec, initialScale = 0.9f, transformOrigin = pivot()))
                            .togetherWith(fadeOut(spec) + scaleOut(spec, targetScale = 0.9f, transformOrigin = pivot()))
                            .using(null)
                    },
                    label = "content",
                ) { s ->
                    // Only the current slot may steer the frame; the outgoing one just fades.
                    val report: ((FrameSpec) -> Unit)? =
                        if (s.key == currentSlot.value.key) { spec -> frame.target = spec } else null
                    when (s.kind) {
                        FrameKind.Idle -> Unit
                        FrameKind.Compact, FrameKind.Alert -> s.primary?.let { activity ->
                            // Size also scales what is inside the compact island (text, icons, art),
                            // otherwise Small and Large looked the same once content appeared.
                            CompositionLocalProvider(
                                LocalDensity provides Density(density.density * appearance.scale, density.fontScale),
                            ) {
                                CompactLayout(math, s, scopeFor(activity, s.kind), report, windowOrigin)
                            }
                        }
                        FrameKind.Expanded -> s.primary?.let { activity ->
                            ExpandedLayout(math, s.kind, report, windowOrigin) { ExpandedContent(scopeFor(activity, s.kind)) }
                        }
                        FrameKind.Deck -> ExpandedLayout(math, s.kind, report, windowOrigin) { ControlDeck(state) }
                    }
                }
            }
            slot.secondary?.let { secondary ->
                BubbleLayer(frame, secondary, Accent.of(secondary, null), windowOrigin)
            }
        }
    }
}

/** Compact content: leading left of the camera, trailing right of it, optional ring around it. */
@Composable
private fun CompactLayout(
    math: FrameMath,
    slot: ContentSlot,
    scope: ContentScope,
    report: ((FrameSpec) -> Unit)?,
    windowOrigin: () -> Offset,
) {
    val density = LocalDensity.current
    val slotHeightDp = with(density) { math.compactHeight.toDp() }
    val decorated = scope.activity.hasCameraDecoration()
    Layout(
        content = {
            Box(Modifier.layoutId("leading"), contentAlignment = Alignment.Center) { CompactLeading(scope, slotHeightDp) }
            Box(Modifier.layoutId("trailing"), contentAlignment = Alignment.Center) { CompactTrailing(scope) }
            if (decorated) Box(Modifier.layoutId("camera")) { CompactAroundCamera(scope) }
        },
        modifier = Modifier.fillMaxSize(),
    ) { measurables, constraints ->
        val slotConstraints = Constraints(maxWidth = math.maxSlotWidth, maxHeight = math.compactHeight.roundToInt())
        val leading = measurables.first { it.layoutId == "leading" }.measure(slotConstraints)
        val trailing = measurables.first { it.layoutId == "trailing" }.measure(slotConstraints)
        val spec = math.compact(ContentMeasure(leading.width, trailing.width), slot.secondary != null, slot.kind)
        report?.invoke(spec)
        val ringPad = 3.dp.roundToPx()
        val camera = measurables.firstOrNull { it.layoutId == "camera" }?.measure(
            Constraints.fixed(math.cameraHole.width.roundToInt() + ringPad * 2, math.cameraHole.height.roundToInt() + ringPad * 2),
        )
        val origin = windowOrigin()
        layout(constraints.maxWidth, constraints.maxHeight) {
            leading.place(
                (spec.leading.left - origin.x).roundToInt(),
                (spec.leading.center.y - origin.y - leading.height / 2f).roundToInt(),
            )
            trailing.place(
                (spec.trailing.left - origin.x).roundToInt(),
                (spec.trailing.center.y - origin.y - trailing.height / 2f).roundToInt(),
            )
            camera?.place(
                (math.cameraHole.left - origin.x).roundToInt() - ringPad,
                (math.cameraHole.top - origin.y).roundToInt() - ringPad,
            )
        }
    }
}

/** Expanded card content: full card width, below the status bar and the camera. */
@Composable
private fun ExpandedLayout(
    math: FrameMath,
    kind: FrameKind,
    report: ((FrameSpec) -> Unit)?,
    windowOrigin: () -> Offset,
    content: @Composable () -> Unit,
) {
    Layout(
        content = { Box(Modifier.layoutId("content")) { content() } },
        modifier = Modifier.fillMaxSize(),
    ) { measurables, constraints ->
        val width = math.expanded(0, kind).content.width.roundToInt().coerceAtLeast(0)
        val placeable = measurables.first().measure(Constraints(minWidth = width, maxWidth = width))
        val spec = math.expanded(maxOf(placeable.height, math.minExpandedContentHeight), kind)
        report?.invoke(spec)
        val origin = windowOrigin()
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeable.place((spec.content.left - origin.x).roundToInt(), (spec.content.top - origin.y).roundToInt())
        }
    }
}

/** The secondary glyph, laid out at the target bubble and following the animated one via its layer. */
@Composable
private fun BubbleLayer(frame: IslandFrameState, secondary: IslandActivity, accent: androidx.compose.ui.graphics.Color, windowOrigin: () -> Offset) {
    val target = frame.target.bubble ?: return
    val density = LocalDensity.current
    val sizeDp = with(density) { target.height.toDp() }
    Layout(
        content = { BubbleGlyph(secondary, accent, sizeDp) },
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                val b = frame.bubble.value
                alpha = frame.bubbleAlpha.value
                translationX = b.center.x - target.center.x
                translationY = b.center.y - target.center.y
                val s = if (target.width > 0f) (b.width / target.width).coerceIn(0f, 1f) else 1f
                scaleX = s
                scaleY = s
                val o = windowOrigin()
                transformOrigin = TransformOrigin(
                    ((target.center.x - o.x) / size.width).coerceIn(0f, 1f),
                    ((target.center.y - o.y) / size.height).coerceIn(0f, 1f),
                )
            },
    ) { measurables, constraints ->
        val side = target.height.roundToInt()
        val placeables = measurables.map { it.measure(Constraints.fixed(side, side)) }
        val origin = windowOrigin()
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeables.forEach { it.place((target.left - origin.x).roundToInt(), (target.top - origin.y).roundToInt()) }
        }
    }
}

/** Clip the content to the animated pill shape (draw phase only). */
private fun Modifier.clipToPill(frame: IslandFrameState, smoothing: Float, windowOrigin: () -> Offset): Modifier {
    val path = Path()
    return drawWithContent {
        val o = windowOrigin()
        smoothRoundedPath(frame.pill.value.translate(-o.x, -o.y), frame.cornerRadius.value, smoothing, path)
        clipPath(path) { this@drawWithContent.drawContent() }
    }
}
