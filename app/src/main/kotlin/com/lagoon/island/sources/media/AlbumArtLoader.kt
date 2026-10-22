package com.lagoon.island.sources.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadata
import android.net.Uri
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Pulls album art out of [MediaMetadata]: the embedded bitmap keys first, then the `*_URI`
 * variants (content:// or file://; http never works, Lagoon has no INTERNET permission).
 * Everything decodes on [Dispatchers.IO] and is downscaled to [MediaRules.ART_MAX_PX] as a
 * software ARGB bitmap so the UI can run Palette over it.
 */
@Singleton
class AlbumArtLoader @Inject constructor(@ApplicationContext private val context: Context) {

    /** A stable identity for the art in [metadata], so unchanged art is not re-decoded on every update. */
    fun key(metadata: MediaMetadata?): String? {
        metadata ?: return null
        BITMAP_KEYS.forEach { k -> metadata.getBitmap(k)?.let { return "bmp:$k:${System.identityHashCode(it)}" } }
        URI_KEYS.forEach { k -> metadata.getString(k)?.takeIf { it.isNotBlank() }?.let { return "uri:$it" } }
        return null
    }

    suspend fun load(metadata: MediaMetadata?): Bitmap? {
        metadata ?: return null
        return withContext(Dispatchers.IO) {
            BITMAP_KEYS.firstNotNullOfOrNull { k -> metadata.getBitmap(k) }?.let { return@withContext downscale(it) }
            URI_KEYS.firstNotNullOfOrNull { k -> metadata.getString(k)?.takeIf { it.isNotBlank() } }
                ?.let { loadUri(Uri.parse(it)) }
        }
    }

    private fun loadUri(uri: Uri): Bitmap? = try {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            null
        } else {
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, MediaRules.ART_MAX_PX)
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }?.let { downscale(it) }
        }
    } catch (e: Exception) {
        Log.d(TAG, "Art uri failed: $uri (${e.javaClass.simpleName})")
        null
    }

    private fun downscale(source: Bitmap): Bitmap {
        val max = MediaRules.ART_MAX_PX
        val longest = maxOf(source.width, source.height)
        val isSoftwareArgb = source.config == Bitmap.Config.ARGB_8888
        if (longest <= max && isSoftwareArgb) return source
        val scale = minOf(1f, max.toFloat() / longest)
        val w = (source.width * scale).toInt().coerceAtLeast(1)
        val h = (source.height * scale).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(source, w, h, true)
        return if (scaled.config == Bitmap.Config.ARGB_8888) scaled else scaled.copy(Bitmap.Config.ARGB_8888, false)
    }

    private fun sampleSize(width: Int, height: Int, max: Int): Int {
        var sample = 1
        while (width / (sample * 2) >= max && height / (sample * 2) >= max) sample *= 2
        return sample
    }

    companion object {
        private const val TAG = "AlbumArtLoader"
        private val BITMAP_KEYS = listOf(
            MediaMetadata.METADATA_KEY_ALBUM_ART,
            MediaMetadata.METADATA_KEY_ART,
            MediaMetadata.METADATA_KEY_DISPLAY_ICON,
        )
        private val URI_KEYS = listOf(
            MediaMetadata.METADATA_KEY_ALBUM_ART_URI,
            MediaMetadata.METADATA_KEY_ART_URI,
            MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI,
        )
    }
}
