package com.lagoon.island.ui.island.content

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lagoon.island.engine.ActivityContent
import com.lagoon.island.engine.IslandAction
import com.lagoon.island.engine.IslandActivity
import kotlinx.coroutines.delay

/** Island type scale: 13 sp compact, 15–17 sp expanded; white at 100% / 60%. */
object IslandType {
    val white = Color.White
    val white60 = Color.White.copy(alpha = 0.6f)
    val white30 = Color.White.copy(alpha = 0.3f)
    val compact = TextStyle(fontSize = 13.sp, lineHeight = 16.sp, color = white, fontWeight = FontWeight.Medium)
    val compactDim = compact.copy(color = white60)
    val title = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, color = white, fontWeight = FontWeight.SemiBold)
    val body = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, color = white)
    val bodyDim = body.copy(color = white60)
    val label = TextStyle(fontSize = 13.sp, lineHeight = 16.sp, color = white60, fontWeight = FontWeight.Medium)
    val big = TextStyle(fontSize = 32.sp, lineHeight = 36.sp, color = white, fontWeight = FontWeight.SemiBold)
    /** Tabular figures so a ticking clock does not jitter. */
    const val TABULAR = "tnum"
}

/** Per-source accent colours (media takes the art colour when there is one). */
object Accent {
    val call = Color(0xFF34C759)
    val timer = Color(0xFFFF9F0A)
    val charging = Color(0xFF30D158)
    val bluetooth = Color(0xFF0A84FF)
    val navigation = Color(0xFF0A84FF)
    val media = Color(0xFFE5E5EA)
    val danger = Color(0xFFFF453A)
    val neutral = Color.White

    fun of(activity: IslandActivity, artColor: Color?): Color {
        activity.tint?.let { return Color(it) }
        return when (activity.content) {
            is ActivityContent.Media -> artColor ?: media
            is ActivityContent.Call -> call
            is ActivityContent.Timer -> timer
            is ActivityContent.Charging -> charging
            is ActivityContent.Bluetooth -> bluetooth
            is ActivityContent.Navigation -> navigation
            is ActivityContent.Notification, is ActivityContent.Progress, is ActivityContent.Generic -> neutral
        }
    }
}

/** What every content composable gets: the activity, its accent and the ways back to the engine. */
@Immutable
class ContentScope(
    val activity: IslandActivity,
    val accent: Color,
    val reduceMotion: Boolean,
    /** Alert presentation: the system event just happened. */
    val alert: Boolean,
    val act: (IslandAction, String?) -> Unit,
    val requestFocus: (Boolean) -> Unit,
) {
    fun action(vararg ids: String): IslandAction? =
        activity.actions.firstOrNull { a -> ids.any { it.equals(a.id, ignoreCase = true) } }
            ?: activity.actions.firstOrNull { a -> ids.any { a.label.contains(it, ignoreCase = true) } }
}

/** A rounded pill button with pressed scale feedback and no ripple (the island is not a Material surface). */
@Composable
fun IslandButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    container: Color = Color.White.copy(alpha = 0.12f),
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier
            .graphicsLayer {
                val s = if (pressed) 0.94f else 1f
                scaleX = s
                scaleY = s
            }
            .clip(CircleShape)
            .background(container)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .heightIn(min = 36.dp)
            .padding(PaddingValues(horizontal = 16.dp, vertical = 8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/** A round icon button (call answer / decline, media transport). */
@Composable
fun RoundIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    container: Color = Color.White.copy(alpha = 0.12f),
    tint: Color = Color.White,
    iconSize: Dp = 24.dp,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier
            .size(size)
            .graphicsLayer {
                val s = if (pressed) 0.9f else 1f
                scaleX = s
                scaleY = s
            }
            .clip(CircleShape)
            .background(container)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(iconSize))
    }
}

/** Text-only action buttons for whatever actions an activity carries, in one row. */
@Composable
fun ActionRow(scope: ContentScope, actions: List<IslandAction>, modifier: Modifier = Modifier) {
    if (actions.isEmpty()) return
    Row(modifier, horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
        actions.forEach { action ->
            IslandButton(onClick = { scope.act(action, null) }) {
                Text(action.label, style = IslandType.compact, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** A second-by-second clock for timers and call durations. */
@Composable
fun rememberTicking(): State<Long> = produceState(initialValue = System.currentTimeMillis()) {
    while (true) {
        value = System.currentTimeMillis()
        delay(1000L - (System.currentTimeMillis() % 1000L))
    }
}
