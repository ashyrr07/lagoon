package com.lagoon.island.ui.app.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lagoon.island.cutout.CutoutInfo
import com.lagoon.island.cutout.IslandGeometry
import com.lagoon.island.cutout.PxRect
import com.lagoon.island.data.IslandSettings
import com.lagoon.island.engine.IslandPresentation
import com.lagoon.island.ui.island.IslandRoot
import com.lagoon.island.ui.island.IslandUiState
import com.lagoon.island.ui.theme.LagoonColors
import com.lagoon.island.ui.theme.LagoonPreviewTheme
import kotlin.math.roundToInt

/** Pixel-like geometry for the fake phone: a round hole just under the top edge. */
private object PreviewPhone {
    const val STATUS_BAR_DP = 38f
    const val HOLE_DP = 22f
    const val HOLE_TOP_DP = 10f
}

/**
 * The top of a phone, at real scale, with the island rendered by the real [IslandRoot] from a fake
 * [IslandUiState]. The frame is what makes the Home hero: the pill wraps the camera the way it will
 * on the device, and Position and Appearance changes move it live.
 */
@Composable
fun IslandPreview(
    settings: IslandSettings,
    modifier: Modifier = Modifier,
    presentation: IslandPresentation = IslandPresentation.Idle,
    screenHeight: Dp = 150.dp,
    inhaleOnAppear: Boolean = false,
) {
    val density = LocalDensity.current
    val bezel = 7.dp
    val frameShape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp)
    val screenShape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp)
    val bezelColor = MaterialTheme.colorScheme.surfaceContainerHighest

    val scaleX = remember { Animatable(1f) }
    LaunchedEffect(inhaleOnAppear) {
        if (!inhaleOnAppear || settings.appearance.reduceMotion) return@LaunchedEffect
        scaleX.snapTo(0.92f)
        scaleX.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow))
    }

    Box(
        modifier
            .fillMaxWidth()
            .height(screenHeight + bezel)
            .clip(frameShape)
            .background(bezelColor)
            .padding(start = bezel, top = bezel, end = bezel),
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .clip(screenShape)
                .background(
                    Brush.verticalGradient(
                        0f to LagoonColors.Shelf,
                        0.4f to LagoonColors.Reef,
                        1f to LagoonColors.Abyss,
                    ),
                ),
        ) {
            val widthPx = with(density) { maxWidth.roundToPx() }
            val heightPx = with(density) { maxHeight.roundToPx() }
            val state = remember(settings, widthPx, heightPx, presentation, density) {
                fakeUiState(settings, widthPx, heightPx, presentation, density.density)
            }
            // Wallpaper horizon: a low, wide glow so the black pill has something to sit against.
            Box(
                Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawCircle(
                            Brush.radialGradient(
                                0f to LagoonColors.Lagoon.copy(alpha = 0.28f),
                                1f to Color.Transparent,
                                center = Offset(size.width * 0.5f, size.height * 1.05f),
                                radius = size.width * 0.75f,
                            ),
                            radius = size.width * 0.75f,
                            center = Offset(size.width * 0.5f, size.height * 1.05f),
                        )
                    },
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        this.scaleX = scaleX.value
                        transformOrigin = TransformOrigin(0.5f, 0f)
                    },
            ) {
                IslandRoot(state)
            }
            // The camera itself, drawn over the pill: a physical hole the island wraps.
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = PreviewPhone.HOLE_TOP_DP.dp)
                    .size(PreviewPhone.HOLE_DP.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF07090B))
                    .drawBehind {
                        drawCircle(Color(0xFF203037), radius = size.minDimension * 0.22f)
                        drawCircle(
                            Color(0xFF6FA6B5).copy(alpha = 0.6f),
                            radius = size.minDimension * 0.07f,
                            center = Offset(size.width * 0.38f, size.height * 0.36f),
                        )
                    },
            )
        }
    }
}

private fun fakeUiState(
    settings: IslandSettings,
    widthPx: Int,
    heightPx: Int,
    presentation: IslandPresentation,
    density: Float,
): IslandUiState {
    fun dp(v: Float) = (v * density).roundToInt()
    val hole = dp(PreviewPhone.HOLE_DP)
    val holeTop = dp(PreviewPhone.HOLE_TOP_DP)
    val cutout = PxRect(widthPx / 2 - hole / 2, holeTop, widthPx / 2 + hole / 2, holeTop + hole)
    val info = CutoutInfo(
        cutout = cutout,
        shape = CutoutInfo.classify(cutout, widthPx),
        statusBarHeight = dp(PreviewPhone.STATUS_BAR_DP),
        screenWidth = widthPx,
        screenHeight = heightPx,
        rotation = 0,
        density = density,
    )
    return IslandUiState().apply {
        this.settings = settings
        this.cutout = info
        this.idle = IslandGeometry.idleFrame(info, settings.calibration)
        this.windowRect = PxRect(0, 0, widthPx, heightPx)
        this.presentation = presentation
        this.reduceMotion = settings.appearance.reduceMotion
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun IslandPreviewPreview() {
    LagoonPreviewTheme {
        IslandPreview(IslandSettings(), Modifier.padding(20.dp))
    }
}
