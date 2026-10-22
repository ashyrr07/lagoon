package com.lagoon.island.ui.island.deck

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lagoon.island.ui.island.IslandUiState

/** Placeholder: the control deck (tiles, sliders, widgets) is built in its own work and replaces this file. */
@Composable
fun ControlDeck(state: IslandUiState, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
        Text(
            "Controls",
            style = TextStyle(fontSize = 17.sp, color = Color.White, fontWeight = FontWeight.SemiBold),
        )
    }
}
