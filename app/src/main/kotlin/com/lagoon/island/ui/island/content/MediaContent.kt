package com.lagoon.island.ui.island.content

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lagoon.island.engine.ActivityContent
import com.lagoon.island.ui.island.TimeFormat

@Composable
fun MediaArt(media: ActivityContent.Media, size: Dp, corner: Dp, modifier: Modifier = Modifier) {
    val image = rememberBitmap(media.art)
    Box(
        modifier.size(size).clip(RoundedCornerShape(corner)).background(Color.White.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        if (image != null) {
            Image(image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(size))
        } else {
            Icon(Icons.Rounded.MusicNote, null, tint = Color.White, modifier = Modifier.size(size * 0.6f))
        }
    }
}

@Composable
fun MediaCompactLeading(scope: ContentScope, media: ActivityContent.Media, slotHeight: Dp) {
    MediaArt(media, size = (slotHeight - 14.dp).coerceAtLeast(20.dp), corner = 6.dp)
}

@Composable
fun MediaCompactTrailing(scope: ContentScope, media: ActivityContent.Media) {
    Waveform(playing = media.isPlaying, color = scope.accent, reduceMotion = scope.reduceMotion)
}

@Composable
fun MediaExpanded(scope: ContentScope, media: ActivityContent.Media) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MediaArt(media, size = 56.dp, corner = 12.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(media.title, style = IslandType.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(media.subtitle, style = IslandType.bodyDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(12.dp))
            Waveform(playing = media.isPlaying, color = scope.accent, reduceMotion = scope.reduceMotion)
        }
        Spacer(Modifier.height(14.dp))
        SeekBar(scope, media)
        Spacer(Modifier.height(6.dp))
        val prev = scope.action("prev", "previous", "skip_previous")
        val playPause = scope.action("play_pause", "play", "pause", "toggle")
        val next = scope.action("next", "skip_next")
        if (prev != null || playPause != null || next != null) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                prev?.let {
                    RoundIconButton(Icons.Rounded.SkipPrevious, it.label, { scope.act(it, null) }, container = Color.Transparent, iconSize = 30.dp)
                }
                playPause?.let {
                    RoundIconButton(
                        if (media.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        it.label,
                        { scope.act(it, null) },
                        size = 52.dp,
                        container = Color.White.copy(alpha = 0.14f),
                        iconSize = 32.dp,
                    )
                }
                next?.let {
                    RoundIconButton(Icons.Rounded.SkipNext, it.label, { scope.act(it, null) }, container = Color.Transparent, iconSize = 30.dp)
                }
            }
        }
    }
}

/**
 * Position bar extrapolated from the last sample with a frame clock; the fill is read in the
 * draw lambda so playback never recomposes. Dragging seeks through the "seek" action, when any.
 */
@Composable
private fun SeekBar(scope: ContentScope, media: ActivityContent.Media) {
    val duration = media.durationMs
    val seek = scope.action("seek")
    val position = remember { mutableLongStateOf(TimeFormat.mediaPosition(media, System.currentTimeMillis())) }
    val fraction = remember { mutableFloatStateOf(0f) }
    val dragging = remember { mutableFloatStateOf(-1f) }
    LaunchedEffect(media) {
        fun sample() {
            val pos = TimeFormat.mediaPosition(media, System.currentTimeMillis())
            position.longValue = pos
            fraction.floatValue = if (duration > 0) (pos.toFloat() / duration).coerceIn(0f, 1f) else 0f
        }
        sample()
        // Only a playing track needs the frame clock; a paused one is sampled once.
        while (media.isPlaying) {
            withFrameMillis { sample() }
        }
    }
    val shownSeconds by remember { androidx.compose.runtime.derivedStateOf { position.longValue / 1000 } }
    val accent = scope.accent
    Column(Modifier.fillMaxWidth()) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(20.dp)
                .then(
                    if (seek != null && duration > 0) {
                        Modifier
                            .pointerInput(media) {
                                detectTapGestures { offset ->
                                    val f = (offset.x / size.width).coerceIn(0f, 1f)
                                    scope.act(seek, (f * duration).toLong().toString())
                                }
                            }
                            .pointerInput(media) {
                                detectHorizontalDragGestures(
                                    onDragStart = { dragging.floatValue = (it.x / size.width).coerceIn(0f, 1f) },
                                    onDragEnd = {
                                        val f = dragging.floatValue
                                        dragging.floatValue = -1f
                                        if (f >= 0f) scope.act(seek, (f * duration).toLong().toString())
                                    },
                                    onDragCancel = { dragging.floatValue = -1f },
                                ) { change, _ ->
                                    dragging.floatValue = (change.position.x / size.width).coerceIn(0f, 1f)
                                }
                            }
                    } else {
                        Modifier
                    },
                ),
        ) {
            val trackH = 4.dp.toPx()
            val y = (size.height - trackH) / 2f
            drawRoundRect(IslandType.white30, Offset(0f, y), Size(size.width, trackH), CornerRadius(trackH / 2f))
            val f = if (dragging.floatValue >= 0f) dragging.floatValue else fraction.floatValue
            drawRoundRect(accent, Offset(0f, y), Size(size.width * f, trackH), CornerRadius(trackH / 2f))
            val knobR = if (dragging.floatValue >= 0f) 7.dp.toPx() else 0f
            if (knobR > 0f) drawCircle(Color.White, knobR, Offset(size.width * f, size.height / 2f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(TimeFormat.position(shownSeconds * 1000), style = IslandType.label)
            Text(if (duration > 0) "-" + TimeFormat.position(duration - shownSeconds * 1000) else "", style = IslandType.label)
        }
    }
}
