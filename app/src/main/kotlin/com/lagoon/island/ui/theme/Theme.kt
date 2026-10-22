package com.lagoon.island.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/** Lagoon's brand accent: the water inside the ring. */
val LagoonTeal = Color(0xFF4FD1C5)
val LagoonInk = Color(0xFF0B0F14)

private val DarkScheme = darkColorScheme(
    primary = LagoonTeal,
    onPrimary = LagoonInk,
    background = LagoonInk,
    surface = LagoonInk,
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF00897B),
)

@Composable
fun LagoonTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val scheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkScheme
        else -> LightScheme
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
