package com.lagoon.island.ui.app.features

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lagoon.island.R
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
                title = { Text(stringResource(R.string.screens_features_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.screens_back))
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
            SectionTitle(stringResource(R.string.screens_features_section_media))
            FeatureRow(stringResource(R.string.screens_features_media), stringResource(R.string.screens_features_media_detail), features.media) { set { f -> f.copy(media = it) } }
            FeatureRow(stringResource(R.string.screens_features_notifications), stringResource(R.string.screens_features_notifications_detail), features.notifications) { set { f -> f.copy(notifications = it) } }
            FeatureRow(
                stringResource(R.string.screens_features_dismiss_system),
                stringResource(R.string.screens_features_dismiss_system_detail),
                features.dismissSystemNotification,
                enabled = features.notifications,
            ) { set { f -> f.copy(dismissSystemNotification = it) } }
            LabeledSlider(R.string.screens_features_notification_seconds, features.notificationSeconds, 2..20) { set { f -> f.copy(notificationSeconds = it) } }

            SectionTitle(stringResource(R.string.screens_features_section_calls))
            FeatureRow(
                stringResource(R.string.screens_features_calls),
                stringResource(R.string.screens_features_calls_detail),
                features.calls,
            ) { set { f -> f.copy(calls = it) } }
            PermissionRow(
                stringResource(R.string.screens_features_phone_state),
                stringResource(R.string.screens_features_phone_state_detail),
                granted = permissions.phoneState,
            ) { requestPermissions.launch(FeaturesViewModel.PHONE_PERMISSIONS) }
            FeatureRow(stringResource(R.string.screens_features_timers), stringResource(R.string.screens_features_timers_detail), features.timers) { set { f -> f.copy(timers = it) } }
            FeatureRow(stringResource(R.string.screens_features_alarms), stringResource(R.string.screens_features_alarms_detail), features.alarms) { set { f -> f.copy(alarms = it) } }

            SectionTitle(stringResource(R.string.screens_features_section_power))
            FeatureRow(stringResource(R.string.screens_features_charging), stringResource(R.string.screens_features_charging_detail), features.charging) { set { f -> f.copy(charging = it) } }
            FeatureRow(stringResource(R.string.screens_features_low_battery), stringResource(R.string.screens_features_low_battery_detail), features.lowBattery) { set { f -> f.copy(lowBattery = it) } }
            FeatureRow(
                stringResource(R.string.screens_features_bluetooth),
                stringResource(R.string.screens_features_bluetooth_detail),
                features.bluetooth,
            ) { set { f -> f.copy(bluetooth = it) } }
            PermissionRow(
                stringResource(R.string.screens_features_nearby),
                stringResource(R.string.screens_features_nearby_detail),
                granted = permissions.bluetooth,
            ) { requestPermissions.launch(FeaturesViewModel.BLUETOOTH_PERMISSIONS) }

            SectionTitle(stringResource(R.string.screens_features_section_on_the_go))
            FeatureRow(stringResource(R.string.screens_features_navigation), stringResource(R.string.screens_features_navigation_detail), features.navigation) { set { f -> f.copy(navigation = it) } }
            FeatureRow(
                stringResource(R.string.screens_features_live_updates),
                stringResource(R.string.screens_features_live_updates_detail),
                features.liveUpdates,
            ) { set { f -> f.copy(liveUpdates = it) } }

            SectionTitle(stringResource(R.string.screens_features_section_system))
            FeatureRow(stringResource(R.string.screens_features_ringer), stringResource(R.string.screens_features_ringer_detail), features.ringerMode) { set { f -> f.copy(ringerMode = it) } }
            FeatureRow(stringResource(R.string.screens_features_torch), stringResource(R.string.screens_features_torch_detail), features.torch) { set { f -> f.copy(torch = it) } }
            FeatureRow(stringResource(R.string.screens_features_screen_recording), stringResource(R.string.screens_features_screen_recording_detail), features.screenRecording) { set { f -> f.copy(screenRecording = it) } }
            FeatureRow(stringResource(R.string.screens_features_no_internet), stringResource(R.string.screens_features_no_internet_detail), features.noInternet) { set { f -> f.copy(noInternet = it) } }
            LabeledSlider(R.string.screens_features_alert_seconds, features.alertSeconds, 1..10) { set { f -> f.copy(alertSeconds = it) } }

            Spacer(Modifier.padding(4.dp))
            Text(
                stringResource(R.string.screens_features_privacy),
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
                Text(stringResource(R.string.screens_features_granted), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            } else {
                TextButton(onClick = onGrant) { Text(stringResource(R.string.screens_features_grant)) }
            }
        }
    }
}

/** [label] is a format string that takes the current [value] as its only argument. */
@Composable
private fun LabeledSlider(@StringRes label: Int, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Column {
        Text(stringResource(label, value), style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.roundToInt().coerceIn(range)) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = (range.last - range.first - 1).coerceAtLeast(0),
        )
    }
}
