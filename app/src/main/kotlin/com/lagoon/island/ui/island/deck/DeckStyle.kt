package com.lagoon.island.ui.island.deck

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lagoon.island.ui.theme.LagoonInk
import com.lagoon.island.ui.theme.LagoonTeal

/**
 * The deck's palette and metrics. The island card is pure black; everything here sits on it.
 * Dark, iOS control-centre proportions with a single accent for "on".
 */
object DeckStyle {
    val Accent = LagoonTeal
    val OnAccent = LagoonInk
    val Surface = Color(0xFF1C1C1E)
    val SurfaceHigh = Color(0xFF2C2C2E)
    val Text = Color.White
    val TextMuted = Color.White.copy(alpha = 0.6f)
    val TextFaint = Color.White.copy(alpha = 0.38f)
    val Warn = Color(0xFFFF9F0A)

    val TileShape = RoundedCornerShape(18.dp)
    val WidgetShape = RoundedCornerShape(18.dp)
    val PillShape = RoundedCornerShape(50)

    val TileHeight = 56.dp
    val SliderHeight = 36.dp
    val WidgetHeight = 64.dp
    val WidgetWidth = 168.dp
    val ShortcutSize = 44.dp
    val Gap = 8.dp
    val SectionGap = 8.dp
    val TouchTarget = 44.dp

    val Label = TextStyle(fontSize = 11.sp, lineHeight = 13.sp, fontWeight = FontWeight.Medium, color = Text)
    val Title = TextStyle(fontSize = 14.sp, lineHeight = 17.sp, fontWeight = FontWeight.SemiBold, color = Text)
    val Body = TextStyle(fontSize = 12.sp, lineHeight = 15.sp, fontWeight = FontWeight.Normal, color = TextMuted)
    val Big = TextStyle(fontSize = 20.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold, color = Text)
}
