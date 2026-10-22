package com.lagoon.island.ui.island.content

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.lagoon.island.engine.IconSource

/** Resolve an [IconSource] to a painter; platform icons are rasterised once at [rasterDp]. */
@Composable
fun rememberIconPainter(source: IconSource?, rasterDp: Dp = 48.dp): Painter? {
    val context = LocalContext.current
    val px = with(LocalDensity.current) { rasterDp.roundToPx() }
    return when (source) {
        null -> null
        is IconSource.Res -> painterResource(source.resId)
        is IconSource.Image -> remember(source.bitmap) { BitmapPainter(source.bitmap.asImageBitmap()) }
        is IconSource.Platform -> remember(source.icon, px) {
            runCatching {
                source.icon.loadDrawable(context)?.toBitmap(px, px)?.asImageBitmap()
            }.getOrNull()?.let { BitmapPainter(it) }
        }
    }
}

@Composable
fun rememberBitmap(bitmap: Bitmap?): ImageBitmap? = remember(bitmap) { bitmap?.asImageBitmap() }

/**
 * An icon from a source, or [fallback]. With [tint] the icon is drawn as a monochrome glyph
 * (notification small icons); without it, full colour (app icons, avatars, art).
 */
@Composable
fun IslandIcon(
    source: IconSource?,
    fallback: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color? = null,
    contentDescription: String? = null,
) {
    val painter = rememberIconPainter(source)
    if (painter == null) {
        Icon(fallback, contentDescription, tint = tint ?: Color.White, modifier = modifier.size(size))
    } else if (tint != null) {
        Icon(painter, contentDescription, tint = tint, modifier = modifier.size(size))
    } else {
        Image(painter, contentDescription, modifier = modifier.size(size), contentScale = ContentScale.Fit)
    }
}

/** A coloured disc with a glyph in it: the leading element of most compact layouts. */
@Composable
fun GlyphDisc(
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 26.dp,
    onDisc: Color = Color.Black,
    contentDescription: String? = null,
) {
    Box(
        modifier.size(size).clip(CircleShape).background(color),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = onDisc, modifier = Modifier.size(size * 0.62f))
    }
}
