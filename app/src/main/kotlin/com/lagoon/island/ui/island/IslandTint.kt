package com.lagoon.island.ui.island

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import com.lagoon.island.data.Appearance
import com.lagoon.island.data.Tint
import com.lagoon.island.engine.ActivityContent
import com.lagoon.island.engine.IslandActivity
import java.util.WeakHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Dominant colour of album art, computed once per bitmap off the main thread. */
object ArtColors {
    private val cache = WeakHashMap<Bitmap, Color>()

    suspend fun of(bitmap: Bitmap): Color? {
        synchronized(cache) { cache[bitmap] }?.let { return it }
        val color = withContext(Dispatchers.Default) {
            runCatching {
                val palette = Palette.from(bitmap).maximumColorCount(24).generate()
                val swatch = palette.vibrantSwatch ?: palette.lightVibrantSwatch ?: palette.dominantSwatch
                swatch?.let { Color(it.rgb) }
            }.getOrNull()
        } ?: return null
        synchronized(cache) { cache[bitmap] = color }
        return color
    }
}

@Composable
fun rememberArtColor(bitmap: Bitmap?): State<Color?> = produceState<Color?>(initialValue = null, bitmap) {
    value = bitmap?.let { ArtColors.of(it) }
}

/** The fill and glow for the current appearance and activity. */
@Composable
fun rememberSurfaceStyle(appearance: Appearance, activity: IslandActivity?): State<SurfaceStyle> {
    val context = LocalContext.current
    val art = (activity?.content as? ActivityContent.Media)?.art
    val artColor by rememberArtColor(if (appearance.tint == Tint.AlbumArt) art else null)
    val materialYou = remember(appearance.tint) {
        if (appearance.tint == Tint.MaterialYou && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val scheme = dynamicDarkColorScheme(context)
            SurfaceStyle(base = scheme.surfaceContainer, glow = scheme.primary)
        } else {
            null
        }
    }
    val style = when (appearance.tint) {
        Tint.Black -> SurfaceStyle()
        Tint.Custom -> SurfaceStyle(base = Color(appearance.customColor))
        Tint.AlbumArt -> SurfaceStyle(base = Color.Black, glow = artColor)
        Tint.MaterialYou -> materialYou ?: SurfaceStyle()
    }
    return rememberUpdatedState(style)
}
