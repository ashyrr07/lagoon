package com.lagoon.island.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode

/**
 * Dark-first. Dynamic colour follows the wallpaper on Android 12+; the brand palette is the
 * fallback and what previews render with.
 */
@Composable
fun LagoonTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val inPreview = LocalInspectionMode.current
    // minSdk 31: dynamic colour is always available on device.
    val scheme: ColorScheme = when {
        dynamicColor && !inPreview ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> LagoonDarkScheme
        else -> LagoonLightScheme
    }
    MaterialTheme(
        colorScheme = scheme,
        typography = LagoonTypography,
        shapes = LagoonShapes,
        content = content,
    )
}

/** Preview wrapper: always the brand palette, dark unless asked otherwise. */
@Composable
fun LagoonPreviewTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    LagoonTheme(darkTheme = darkTheme, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background, content = content)
    }
}
