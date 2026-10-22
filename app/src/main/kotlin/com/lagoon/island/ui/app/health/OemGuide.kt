package com.lagoon.island.ui.app.health

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lagoon.island.R
import com.lagoon.island.ui.app.components.Note
import com.lagoon.island.ui.app.components.SettingGroup
import com.lagoon.island.ui.theme.LagoonPreviewTheme
import com.lagoon.island.util.Oem
import com.lagoon.island.util.OemGuide
import com.lagoon.island.util.OemInfo

/**
 * The per-brand keep-alive guide. Steps are a real sequence on the phone, so they are numbered.
 * Where the OEM's settings activity is known and present, the step gets an Open button.
 */
@Composable
fun OemGuideGroup(guide: OemGuide, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingGroup(guide.oem.brandName) {
            guide.steps.forEachIndexed { index, step ->
                row {
                    val intent = remember(step) { OemInfo.resolve(context, step) }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(
                            "${index + 1}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(step.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                step.detail,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (intent != null) {
                            TextButton(onClick = { runCatching { context.startActivity(intent) } }) { Text(stringResource(R.string.setup_action_open)) }
                        }
                    }
                }
            }
        }
        Note(guide.summary)
    }
}

@Preview(showBackground = true)
@Composable
private fun OemGuidePreview() {
    LagoonPreviewTheme {
        OemGuideGroup(OemInfo.guide(Oem.Xiaomi), Modifier.padding(20.dp))
    }
}
