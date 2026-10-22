package com.lagoon.island.ui.app.about

import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.lagoon.island.BuildConfig
import com.lagoon.island.R
import com.lagoon.island.ui.app.SettingsViewModel
import com.lagoon.island.ui.app.components.ContentRow
import com.lagoon.island.ui.app.components.NavRow
import com.lagoon.island.ui.app.components.Note
import com.lagoon.island.ui.app.components.ScreenScaffold
import com.lagoon.island.ui.app.components.SettingGroup
import com.lagoon.island.ui.app.components.ValueRow
import com.lagoon.island.ui.theme.LagoonPreviewTheme
import kotlinx.coroutines.launch

@Composable
fun AboutScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    fun toast(text: String) = Toast.makeText(context, text, Toast.LENGTH_SHORT).show()

    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            viewModel.exportTo(uri)
                .onSuccess { toast("Settings exported") }
                .onFailure { toast("Export failed: ${it.message}") }
        }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            viewModel.importFrom(uri)
                .onSuccess { toast("Settings imported") }
                .onFailure { toast("That file is not a Lagoon backup") }
        }
    }

    AboutContent(
        onBack = onBack,
        onExport = { exporter.launch("lagoon-settings.json") },
        onImport = { importer.launch(arrayOf("application/json", "text/plain", "*/*")) },
    )
}

@Composable
private fun AboutContent(onBack: () -> Unit, onExport: () -> Unit, onImport: () -> Unit) {
    val context = LocalContext.current
    val github = stringResource(R.string.github_url)

    ScreenScaffold(title = "About", onBack = onBack) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Image(
                painterResource(R.drawable.ic_lagoon_mark),
                contentDescription = null,
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(18.dp)),
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Lagoon", style = MaterialTheme.typography.headlineMedium)
                Text(
                    stringResource(R.string.app_tagline),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.FLAVOR})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        SettingGroup("Open source") {
            row {
                ValueRow(
                    "Source code",
                    "Copy link",
                    subtitle = github.removePrefix("https://"),
                    onClick = {
                        val clipboard = context.getSystemService(ClipboardManager::class.java)
                        clipboard.setPrimaryClip(ClipData.newPlainText("Lagoon on GitHub", github))
                        Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show()
                    },
                )
            }
            row {
                ValueRow(
                    "Licence",
                    "GPL-3.0",
                    subtitle = "Free to use, study, share and improve. Forks must stay open.",
                )
            }
        }

        SettingGroup("Privacy") {
            row {
                ContentRow {
                    Text("Nothing leaves your phone", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Lagoon has no internet permission, so it cannot send anything anywhere: no analytics, " +
                            "no crash reports, no accounts. Notification access is used only to show what is " +
                            "playing, who is calling and the notifications you choose; the text stays in memory " +
                            "and is dropped when the island clears it.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        SettingGroup("Backup") {
            row { NavRow("Export settings", "Save a JSON file you can keep or move to another phone", onClick = onExport) }
            row { NavRow("Import settings", "Replace the current settings with a saved file", onClick = onImport) }
        }
        Note("Made for every Android with a camera cutout. Thank you for using free software.")
    }
}

@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun AboutPreview() {
    LagoonPreviewTheme {
        AboutContent(onBack = {}, onExport = {}, onImport = {})
    }
}
