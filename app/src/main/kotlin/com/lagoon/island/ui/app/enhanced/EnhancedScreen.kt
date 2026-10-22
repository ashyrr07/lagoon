package com.lagoon.island.ui.app.enhanced

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Prominent disclosure for Enhanced mode (the optional accessibility service). Play policy: say
 * what the service does and does not do, get explicit consent, and only then send the user to the
 * system grant screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedScreen(
    onBack: () -> Unit,
    viewModel: EnhancedViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Enhanced mode") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Optional. Lagoon works fully without it. Enhanced mode uses an accessibility service " +
                    "for one reason: an accessibility window is the only kind Android draws above the " +
                    "status bar and the lock screen.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (!status.available) {
                NotAvailableCard()
            } else {
                StatusCard(status)
            }

            Section(
                title = "What it unlocks",
                items = listOf(
                    "Taps anywhere on the pill, including the part inside the status bar, so the island can hug the camera and still be tappable.",
                    "The island on the lock screen (you can turn this off below).",
                    "Screenshot, lock screen and recent apps tiles in the control deck.",
                    "Exact detection of the app in front, for per-app hiding and app-aware activities.",
                ),
            )

            Section(
                title = "What it never does",
                items = listOf(
                    "It never reads what is on your screen: the service is declared without access to window content and without the ability to inspect other apps' windows.",
                    "It never performs gestures or taps for you.",
                    "It only receives the name of the app that came to the front, nothing from inside the app.",
                    "No data leaves your phone. Lagoon has no internet permission at all.",
                ),
            )

            Text(
                "Android will warn that the service can \"view and control your screen\". That is the generic " +
                    "accessibility warning; Lagoon's service is configured so that it cannot do either.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (status.available) {
                ConsentRow(
                    checked = settings.consented,
                    onCheckedChange = viewModel::setConsented,
                )

                if (settings.consented) {
                    if (status.sideloaded && !status.enabledInSettings) {
                        RestrictedSettingsCard(onOpenAppInfo = viewModel::openAppInfo)
                    }
                    Button(onClick = viewModel::openAccessibilitySettings, modifier = Modifier.fillMaxWidth()) {
                        Text(if (status.enabledInSettings) "Open Accessibility settings" else "Turn on in Accessibility settings")
                    }
                    Text(
                        "In the list, choose \"Lagoon Enhanced mode\" and switch it on. Come back here to check the status.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                ToggleRow(
                    title = "Show on lock screen",
                    body = "Keep the island visible while the phone is locked. Only possible in Enhanced mode.",
                    checked = settings.showOnLockScreen,
                    enabled = settings.consented,
                    onCheckedChange = viewModel::setShowOnLockScreen,
                )
            }
        }
    }
}

@Composable
private fun StatusCard(status: EnhancedStatus) {
    val (title, body) = when {
        status.connected -> "On and connected" to "The island is drawn above the status bar."
        status.enabledInSettings -> "On, waiting to connect" to
            "Switched on in Accessibility settings. If this stays here, toggle it off and on again there."
        else -> "Off" to "The island uses the standard overlay window."
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = if (status.connected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun NotAvailableCard() {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Not in this build yet", style = MaterialTheme.typography.titleMedium)
            Text(
                "Google Play reviews every app that offers an accessibility service. Enhanced mode will " +
                    "arrive in the Play build once that review is complete.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Want it now? Install the FOSS build from the Lagoon GitHub releases page or from F-Droid. " +
                    "It is the same app, open source, with Enhanced mode included. Uninstall this copy first; " +
                    "the two builds are signed differently and cannot be installed over each other.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RestrictedSettingsCard(onOpenAppInfo: () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("One extra step for this install", style = MaterialTheme.typography.titleMedium)
            Text(
                "This copy of Lagoon was not installed from an app store, so Android 13 and later hide the " +
                    "switch behind \"Restricted setting\" until you allow it:",
                style = MaterialTheme.typography.bodyMedium,
            )
            Bullet("1. Open Lagoon's App info page (button below).")
            Bullet("2. Tap the ⋮ menu in the top-right corner.")
            Bullet("3. Choose \"Allow restricted settings\" and confirm.")
            Text(
                "If the menu item is missing, first open Accessibility settings, tap Lagoon Enhanced mode once " +
                    "(Android shows the restricted-setting notice), then come back and try again.",
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedButton(onClick = onOpenAppInfo, modifier = Modifier.fillMaxWidth()) { Text("Open App info") }
        }
    }
}

@Composable
private fun ConsentRow(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Checkbox) { onCheckedChange(!checked) }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = checked, onCheckedChange = null)
            Spacer(Modifier.width(8.dp))
            Text(
                "I understand what Enhanced mode does and does not do, and I consent to Lagoon using an " +
                    "accessibility service for it.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    body: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun Section(title: String, items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        items.forEach { Bullet(it) }
    }
}

@Composable
private fun Bullet(text: String) {
    Row {
        Text("•", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(16.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
