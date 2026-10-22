package com.lagoon.island.ui.app.features

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lagoon.island.data.FeatureSettings
import kotlin.math.roundToInt

/** One switch per source, each with a line saying what it watches and what it never reads. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeaturesScreen(
    onBack: () -> Unit,
    viewModel: FeaturesViewModel = hiltViewModel(),
) {
    val features by viewModel.features.collectAsStateWithLifecycle()
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()
    val requestPermissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        viewModel.refreshPermissions()
    }
    fun set(transform: (FeatureSettings) -> FeatureSettings) = viewModel.update(transform)

    LifecycleResumeEffect(Unit) {
        viewModel.refreshPermissions()
        onPauseOrDispose { }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Features") },
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionTitle("Media and notifications")
            FeatureRow("Now playing", "Album art, title and controls for whatever is playing.", features.media) { set { f -> f.copy(media = it) } }
            FeatureRow("Notifications", "Show notifications from the apps you pick, with reply and actions.", features.notifications) { set { f -> f.copy(notifications = it) } }
            FeatureRow(
                "Clear the system notification",
                "After the island showed a notification, remove it from the shade.",
                features.dismissSystemNotification,
                enabled = features.notifications,
            ) { set { f -> f.copy(dismissSystemNotification = it) } }
            LabeledSlider("Notification stays for", features.notificationSeconds, 2..20, "s") { set { f -> f.copy(notificationSeconds = it) } }

            SectionTitle("Calls and timers")
            FeatureRow(
                "Calls",
                "Caller, answer, decline and hang up, mirrored from your dialer's notification. No call log or contacts.",
                features.calls,
            ) { set { f -> f.copy(calls = it) } }
            PermissionRow(
                "Phone state",
                "Optional: lets the island notice ringing and answered calls even before the dialer posts its notification.",
                granted = permissions.phoneState,
            ) { requestPermissions.launch(FeaturesViewModel.PHONE_PERMISSIONS) }
            FeatureRow("Timers and stopwatch", "Any Clock app's running timer, with its pause and stop buttons.", features.timers) { set { f -> f.copy(timers = it) } }
            FeatureRow("Alarms", "A ringing alarm with snooze and dismiss; the next alarm for the deck widget.", features.alarms) { set { f -> f.copy(alarms = it) } }

            SectionTitle("Power and devices")
            FeatureRow("Charging", "Plug-in alert with level and wattage, plus a note when the battery is full.", features.charging) { set { f -> f.copy(charging = it) } }
            FeatureRow("Low battery", "One alert per charge at 15 %.", features.lowBattery) { set { f -> f.copy(lowBattery = it) } }
            FeatureRow(
                "Bluetooth",
                "Connected and disconnected devices, with earbud and case battery when the device reports it.",
                features.bluetooth,
            ) { set { f -> f.copy(bluetooth = it) } }
            PermissionRow(
                "Nearby devices",
                "Needed for device names and battery. The scan is short, filtered to earbuds, and never used for location.",
                granted = permissions.bluetooth,
            ) { requestPermissions.launch(FeaturesViewModel.BLUETOOTH_PERMISSIONS) }

            SectionTitle("On the go")
            FeatureRow("Navigation", "Next turn, distance and ETA from Google Maps or Waze.", features.navigation) { set { f -> f.copy(navigation = it) } }
            FeatureRow(
                "Live Updates",
                "Progress activities such as deliveries, rides and downloads (Android 16 Live Updates and classic progress bars).",
                features.liveUpdates,
            ) { set { f -> f.copy(liveUpdates = it) } }

            SectionTitle("System")
            FeatureRow("Ring, vibrate, silent", "A short alert when the ringer mode or Do Not Disturb changes.", features.ringerMode) { set { f -> f.copy(ringerMode = it) } }
            FeatureRow("Flashlight", "Shows while the torch is on, with a button to turn it off.", features.torch) { set { f -> f.copy(torch = it) } }
            FeatureRow("Screen recording", "Shows while the system is recording the screen, with its stop button.", features.screenRecording) { set { f -> f.copy(screenRecording = it) } }
            FeatureRow("No internet", "A quiet indicator while the phone has no working connection.", features.noInternet) { set { f -> f.copy(noInternet = it) } }
            LabeledSlider("System alerts hold the island for", features.alertSeconds, 1..10, "s") { set { f -> f.copy(alertSeconds = it) } }

            Spacer(Modifier.padding(4.dp))
            Text(
                "Everything stays on this phone: Lagoon has no internet permission.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp),
    )
}

@Composable
private fun FeatureRow(
    title: String,
    body: String,
    checked: Boolean,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

@Composable
private fun PermissionRow(title: String, body: String, granted: Boolean, onGrant: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium)
                Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(12.dp))
            if (granted) {
                Text("Granted", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            } else {
                TextButton(onClick = onGrant) { Text("Grant") }
            }
        }
    }
}

@Composable
private fun LabeledSlider(label: String, value: Int, range: IntRange, unit: String, onChange: (Int) -> Unit) {
    Column {
        Text("$label $value$unit", style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.roundToInt().coerceIn(range)) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = (range.last - range.first - 1).coerceAtLeast(0),
        )
    }
}
