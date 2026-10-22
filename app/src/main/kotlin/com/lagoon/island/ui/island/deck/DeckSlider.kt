package com.lagoon.island.ui.island.deck

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lagoon.island.R
import com.lagoon.island.controls.Control
import com.lagoon.island.controls.ControlState
import kotlin.math.roundToInt

/**
 * A filled-track slider with the control's icon at its head. Drag anywhere on the track; the
 * value commits as integers (volume index, brightness 0..255) whenever the rounded value changes.
 */
@Composable
fun DeckSlider(
    control: Control,
    state: ControlState,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val max = state.max.coerceAtLeast(1f)
    var dragging by remember { mutableStateOf(false) }
    var local by remember { mutableFloatStateOf(state.value) }
    var widthPx by remember { mutableIntStateOf(0) }
    var lastSent by remember { mutableIntStateOf(state.value.roundToInt()) }

    // Follow the system while the finger is up (volume keys, auto-brightness).
    LaunchedEffect(state.value, dragging) {
        if (!dragging) {
            local = state.value
            lastSent = state.value.roundToInt()
        }
    }

    fun setFromX(x: Float) {
        if (widthPx <= 0) return
        val fraction = (x / widthPx).coerceIn(0f, 1f)
        local = fraction * max
        val rounded = local.roundToInt()
        if (rounded != lastSent) {
            lastSent = rounded
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            onChange(rounded.toFloat())
        }
    }

    val fraction = (local / max).coerceIn(0f, 1f)
    val icon = if (state.active) control.activeIcon ?: control.icon else control.icon

    Box(
        modifier
            .fillMaxWidth()
            .height(DeckStyle.SliderHeight)
            .clip(DeckStyle.PillShape)
            .background(DeckStyle.Surface)
            .alpha(if (state.enabled) 1f else 0.4f)
            .onSizeChanged { widthPx = it.width }
            .pointerInputIf(state.enabled) {
                detectTapGestures(onPress = { offset -> setFromX(offset.x) })
            }
            .pointerInputIf(state.enabled) {
                detectHorizontalDragGestures(
                    onDragStart = { offset -> dragging = true; setFromX(offset.x) },
                    onDragEnd = { dragging = false },
                    onDragCancel = { dragging = false },
                    onHorizontalDrag = { change, _ -> change.consume(); setFromX(change.position.x) },
                )
            },
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction)
                .background(DeckStyle.Accent.copy(alpha = 0.92f)),
        )
        Icon(
            icon,
            contentDescription = stringResource(control.labelRes),
            tint = if (fraction > 0.12f) DeckStyle.OnAccent else DeckStyle.Text,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 10.dp)
                .size(18.dp),
        )
        val caption = state.detail?.let { stringResource(it) }
            ?: if (state.needsPermission) stringResource(R.string.island_deck_slider_grant) else null
        if (caption != null) {
            Text(
                caption,
                style = DeckStyle.Label.copy(color = DeckStyle.TextMuted),
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp),
            )
        }
    }
}

private fun Modifier.pointerInputIf(
    enabled: Boolean,
    block: suspend PointerInputScope.() -> Unit,
): Modifier = if (enabled) pointerInput(enabled, block) else this
