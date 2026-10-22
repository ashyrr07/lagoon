package com.lagoon.island.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Brand palette, used when dynamic colour is off or unavailable. Named after the place: deep water,
 * the reef under it, the lagoon itself, and the foam and driftwood on the shore.
 */
object LagoonColors {
    val Abyss = Color(0xFF06141B)
    val Trench = Color(0xFF0A1C25)
    val Reef = Color(0xFF0F2530)
    val Shoal = Color(0xFF163542)
    val Shelf = Color(0xFF1E4252)
    val Lagoon = Color(0xFF5FE3D0)
    val LagoonDeep = Color(0xFF00695E)
    val LagoonInk = Color(0xFF00201C)
    val Foam = Color(0xFFF2EFE6)
    val Driftwood = Color(0xFF9FB1B4)
    val Kelp = Color(0xFF32505A)
    val Sunlit = Color(0xFFFFC56B)
    val SunlitInk = Color(0xFF3D2600)
    val Sky = Color(0xFF8FC8F0)
    val Coral = Color(0xFFFF8A80)

    val Paper = Color(0xFFEEF5F3)
    val PaperRaised = Color(0xFFF7FBFA)
    val PaperLow = Color(0xFFE1ECE9)
    val PaperLower = Color(0xFFD3E2DE)
    val Ink = Color(0xFF10232A)
    val InkSoft = Color(0xFF45605F)
}

/** Island tint on the settings previews and the pill itself. Shared with the island renderer. */
val LagoonTeal = LagoonColors.Lagoon
val LagoonInk = LagoonColors.Abyss

val LagoonDarkScheme = darkColorScheme(
    primary = LagoonColors.Lagoon,
    onPrimary = LagoonColors.LagoonInk,
    primaryContainer = LagoonColors.LagoonDeep,
    onPrimaryContainer = Color(0xFFB8FFF3),
    inversePrimary = LagoonColors.LagoonDeep,
    secondary = LagoonColors.Sky,
    onSecondary = Color(0xFF00344B),
    secondaryContainer = LagoonColors.Shelf,
    onSecondaryContainer = Color(0xFFD3EEFF),
    tertiary = LagoonColors.Sunlit,
    onTertiary = LagoonColors.SunlitInk,
    tertiaryContainer = Color(0xFF5A3F00),
    onTertiaryContainer = Color(0xFFFFE1B0),
    error = LagoonColors.Coral,
    onError = Color(0xFF4A0400),
    errorContainer = Color(0xFF7A1F1A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = LagoonColors.Abyss,
    onBackground = LagoonColors.Foam,
    surface = LagoonColors.Abyss,
    onSurface = LagoonColors.Foam,
    surfaceVariant = LagoonColors.Shoal,
    onSurfaceVariant = LagoonColors.Driftwood,
    surfaceDim = Color(0xFF040E13),
    surfaceBright = LagoonColors.Shelf,
    surfaceContainerLowest = Color(0xFF030B10),
    surfaceContainerLow = LagoonColors.Trench,
    surfaceContainer = LagoonColors.Reef,
    surfaceContainerHigh = LagoonColors.Shoal,
    surfaceContainerHighest = LagoonColors.Shelf,
    inverseSurface = LagoonColors.Foam,
    inverseOnSurface = LagoonColors.Abyss,
    outline = Color(0xFF5E7A80),
    outlineVariant = LagoonColors.Kelp,
    scrim = Color(0xFF000000),
)

val LagoonLightScheme = lightColorScheme(
    primary = LagoonColors.LagoonDeep,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFA8F2E4),
    onPrimaryContainer = LagoonColors.LagoonInk,
    inversePrimary = LagoonColors.Lagoon,
    secondary = Color(0xFF2E6584),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCCE6F8),
    onSecondaryContainer = Color(0xFF07202F),
    tertiary = Color(0xFF8A5A00),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFE1B0),
    onTertiaryContainer = LagoonColors.SunlitInk,
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = LagoonColors.Paper,
    onBackground = LagoonColors.Ink,
    surface = LagoonColors.Paper,
    onSurface = LagoonColors.Ink,
    surfaceVariant = LagoonColors.PaperLow,
    onSurfaceVariant = LagoonColors.InkSoft,
    surfaceDim = LagoonColors.PaperLower,
    surfaceBright = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = LagoonColors.PaperRaised,
    surfaceContainer = Color(0xFFE7F0EE),
    surfaceContainerHigh = LagoonColors.PaperLow,
    surfaceContainerHighest = LagoonColors.PaperLower,
    inverseSurface = LagoonColors.Ink,
    inverseOnSurface = LagoonColors.Paper,
    outline = Color(0xFF6F8A8A),
    outlineVariant = Color(0xFFBFD0CE),
    scrim = Color(0xFF000000),
)
