package com.lagoon.island.ui.island.content

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import com.lagoon.island.R
import com.lagoon.island.ui.island.TimeFormat

/** "82%" in the app's language ("82 %" where the locale puts a space before the sign). */
@Composable
@ReadOnlyComposable
fun percentText(value: Int): String = stringResource(R.string.island_percent, value)

/** "27 W" with the locale's unit symbol; null when the wattage is unknown. */
@Composable
@ReadOnlyComposable
fun wattsText(watts: Float?): String? = TimeFormat.watts(watts)?.let { stringResource(R.string.island_watts, it) }
