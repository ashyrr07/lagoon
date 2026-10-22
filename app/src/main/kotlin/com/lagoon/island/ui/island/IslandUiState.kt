package com.lagoon.island.ui.island

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.lagoon.island.cutout.CutoutInfo
import com.lagoon.island.cutout.IdleFrame
import com.lagoon.island.cutout.PxRect
import com.lagoon.island.data.IslandSettings

/** Snapshot state handed to the island composable; mutated only from the main thread. */
class IslandUiState {
    var idle: IdleFrame? by mutableStateOf(null)
        private set
    var windowRect: PxRect by mutableStateOf(PxRect(0, 0, 0, 0))
        private set
    var cutout: CutoutInfo? by mutableStateOf(null)
        private set
    var settings: IslandSettings by mutableStateOf(IslandSettings())
        private set

    var onTap: () -> Unit = {}
    var onOutsideTouch: () -> Unit = {}

    fun update(idle: IdleFrame, windowRect: PxRect, cutout: CutoutInfo, settings: IslandSettings) {
        this.idle = idle
        this.windowRect = windowRect
        this.cutout = cutout
        this.settings = settings
    }
}
