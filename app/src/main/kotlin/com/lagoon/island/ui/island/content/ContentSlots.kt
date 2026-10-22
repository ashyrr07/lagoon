package com.lagoon.island.ui.island.content

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Downloading
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lagoon.island.engine.ActivityContent
import com.lagoon.island.engine.IslandActivity

/** Leading slot (left of the camera) for the compact and alert presentations. */
@Composable
fun CompactLeading(scope: ContentScope, slotHeight: Dp) {
    when (val c = scope.activity.content) {
        is ActivityContent.Media -> MediaCompactLeading(scope, c, slotHeight)
        is ActivityContent.Call -> CallCompactLeading(scope, c, slotHeight)
        is ActivityContent.Timer -> TimerCompactLeading(scope)
        is ActivityContent.Notification -> NotificationCompactLeading(scope, c, slotHeight)
        is ActivityContent.Progress -> ProgressCompactLeading(scope, c)
        is ActivityContent.Charging -> ChargingCompactLeading(scope, c)
        is ActivityContent.Bluetooth -> BluetoothCompactLeading(scope, c)
        is ActivityContent.Navigation -> NavigationCompactLeading(scope, c)
        is ActivityContent.Generic -> GenericCompactLeading(scope, c)
    }
}

/** Trailing slot (right of the camera) for the compact and alert presentations. */
@Composable
fun CompactTrailing(scope: ContentScope) {
    when (val c = scope.activity.content) {
        is ActivityContent.Media -> MediaCompactTrailing(scope, c)
        is ActivityContent.Call -> CallCompactTrailing(scope, c)
        is ActivityContent.Timer -> TimerCompactTrailing(scope, c)
        is ActivityContent.Notification -> NotificationCompactTrailing(scope, c)
        is ActivityContent.Progress -> ProgressCompactTrailing(scope, c)
        is ActivityContent.Charging -> ChargingCompactTrailing(scope, c)
        is ActivityContent.Bluetooth -> BluetoothCompactTrailing(scope, c)
        is ActivityContent.Navigation -> NavigationCompactTrailing(scope, c)
        is ActivityContent.Generic -> GenericCompactTrailing(scope, c)
    }
}

/** Optional decoration drawn exactly around the camera (charging ring). */
@Composable
fun CompactAroundCamera(scope: ContentScope) {
    when (val c = scope.activity.content) {
        is ActivityContent.Charging -> if (c.state != ActivityContent.Charging.State.Disconnected) ChargingRing(scope, c)
        else -> Unit
    }
}

fun IslandActivity.hasCameraDecoration(): Boolean =
    (content as? ActivityContent.Charging)?.state?.let { it != ActivityContent.Charging.State.Disconnected } == true

/** The expanded card body for any activity. */
@Composable
fun ExpandedContent(scope: ContentScope) {
    when (val c = scope.activity.content) {
        is ActivityContent.Media -> MediaExpanded(scope, c)
        is ActivityContent.Call -> CallExpanded(scope, c)
        is ActivityContent.Timer -> TimerExpanded(scope, c)
        is ActivityContent.Notification -> NotificationExpanded(scope, c)
        is ActivityContent.Progress -> ProgressExpanded(scope, c)
        is ActivityContent.Charging -> ChargingExpanded(scope, c)
        is ActivityContent.Bluetooth -> BluetoothExpanded(scope, c)
        is ActivityContent.Navigation -> NavigationExpanded(scope, c)
        is ActivityContent.Generic -> GenericExpanded(scope, c)
    }
}

/** The tiny glyph inside the detached secondary bubble. */
@Composable
fun BubbleGlyph(activity: IslandActivity, accent: Color, bubbleSize: Dp) {
    val glyph = bubbleSize * 0.46f
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (val c = activity.content) {
            is ActivityContent.Media -> MediaArt(c, size = bubbleSize - 12.dp, corner = 6.dp)
            is ActivityContent.Call -> Icon(Icons.Rounded.Call, null, tint = accent, modifier = Modifier.size(glyph))
            is ActivityContent.Timer -> Icon(Icons.Rounded.Timer, null, tint = accent, modifier = Modifier.size(glyph))
            is ActivityContent.Notification -> IslandIcon(c.largeIcon ?: c.appIcon, Icons.Rounded.Notifications, size = bubbleSize - 14.dp)
            is ActivityContent.Progress -> IslandIcon(c.icon, Icons.Rounded.Downloading, size = glyph, tint = if (c.icon == null) accent else null)
            is ActivityContent.Charging -> Icon(Icons.Rounded.Bolt, null, tint = accent, modifier = Modifier.size(glyph))
            is ActivityContent.Bluetooth -> Icon(Icons.Rounded.Bluetooth, null, tint = accent, modifier = Modifier.size(glyph))
            is ActivityContent.Navigation -> IslandIcon(c.icon, Icons.Rounded.Navigation, size = glyph, tint = accent)
            is ActivityContent.Generic -> IslandIcon(c.icon, Icons.Rounded.Info, size = glyph, tint = if (c.icon == null) accent else null)
        }
    }
}
