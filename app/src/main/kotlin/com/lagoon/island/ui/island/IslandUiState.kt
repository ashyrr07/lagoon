package com.lagoon.island.ui.island

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.lagoon.island.cutout.CutoutInfo
import com.lagoon.island.cutout.IdleFrame
import com.lagoon.island.cutout.PxRect
import com.lagoon.island.data.IslandSettings
import com.lagoon.island.engine.IslandAction
import com.lagoon.island.engine.IslandActivity
import com.lagoon.island.engine.IslandPresentation
import com.lagoon.island.engine.SwipeDirection
import com.lagoon.island.overlay.OverlayHost

/**
 * Everything the island composable needs, as snapshot state, plus the callbacks it reports through.
 * Mutated only on the main thread by the controller.
 *
 * Coordinates: [idle] and [windowRect] are screen pixels. The composable draws in window-local
 * pixels: subtract [windowRect]'s origin (see [WindowGeometry.toLocal]).
 *
 * Window-size protocol (the window is only as big as the island):
 *  1. Before animating to a larger frame, call [onRequestWindow] with the screen rect the
 *     animation will need (union of current and target). The controller grows the window at once.
 *  2. When the frame animation has settled, call [onFrameSettled] with the final screen rect. The
 *     controller shrinks the window to fit (plus shadow padding).
 */
class IslandUiState {
    var presentation: IslandPresentation by mutableStateOf(IslandPresentation.Hidden)
    var idle: IdleFrame? by mutableStateOf(null)
    var windowRect: PxRect by mutableStateOf(PxRect(0, 0, 0, 0))
    var cutout: CutoutInfo? by mutableStateOf(null)
    var settings: IslandSettings by mutableStateOf(IslandSettings())
    var hostKind: OverlayHost.Kind by mutableStateOf(OverlayHost.Kind.App)
    /** True when the system animator scale is 0 or the user asked for reduced motion. */
    var reduceMotion: Boolean by mutableStateOf(false)

    var onTap: (IslandActivity?) -> Unit = {}
    var onLongPress: (IslandActivity?) -> Unit = {}
    var onSwipe: (SwipeDirection, IslandActivity?) -> Unit = { _, _ -> }
    var onOutsideTouch: () -> Unit = {}
    /** Any interaction inside an expanded view (keeps it open). */
    var onInteraction: () -> Unit = {}
    var onAction: (IslandActivity, IslandAction, String?) -> Unit = { _, _, _ -> }
    var onCollapse: () -> Unit = {}
    var onRequestWindow: (PxRect) -> Unit = {}
    var onFrameSettled: (PxRect) -> Unit = {}
    /** Ask for keyboard focus (reply field) or give it back. */
    var onRequestFocus: (Boolean) -> Unit = {}
}
