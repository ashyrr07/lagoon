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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lagoon.island.R

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
                title = { Text(stringResource(R.string.screens_enhanced_title)) },
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                stringResource(R.string.screens_enhanced_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (!status.available) {
                NotAvailableCard()
            } else {
                StatusCard(status)
            }

            Section(
                title = stringResource(R.string.screens_enhanced_unlocks_title),
                items = listOf(
                    stringResource(R.string.screens_enhanced_unlocks_taps),
                    stringResource(R.string.screens_enhanced_unlocks_lock_screen),
                    stringResource(R.string.screens_enhanced_unlocks_tiles),
                    stringResource(R.string.screens_enhanced_unlocks_app_detection),
                ),
            )

            Section(
                title = stringResource(R.string.screens_enhanced_never_title),
                items = listOf(
                    stringResource(R.string.screens_enhanced_never_reads),
                    stringResource(R.string.screens_enhanced_never_gestures),
                    stringResource(R.string.screens_enhanced_never_app_name),
                    stringResource(R.string.screens_enhanced_never_data),
                ),
            )

            Text(
                stringResource(R.string.screens_enhanced_warning),
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
                        Text(
                            stringResource(
                                if (status.enabledInSettings) R.string.screens_enhanced_open_settings else R.string.screens_enhanced_turn_on,
                            ),
                        )
                    }
                    Text(
                        stringResource(
                            R.string.screens_enhanced_choose_in_list,
                            stringResource(R.string.enhanced_service_label),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                ToggleRow(
                    title = stringResource(R.string.screens_enhanced_lock_screen),
                    body = stringResource(R.string.screens_enhanced_lock_screen_detail),
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
    val (titleRes, bodyRes) = when {
        status.connected -> R.string.screens_enhanced_status_connected to R.string.screens_enhanced_status_connected_detail
        status.enabledInSettings -> R.string.screens_enhanced_status_waiting to R.string.screens_enhanced_status_waiting_detail
        else -> R.string.screens_enhanced_status_off to R.string.screens_enhanced_status_off_detail
    }
    val title = stringResource(titleRes)
    val body = stringResource(bodyRes)
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
            Text(stringResource(R.string.screens_enhanced_unavailable_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.screens_enhanced_unavailable_review),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(R.string.screens_enhanced_unavailable_foss),
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
            Text(stringResource(R.string.screens_enhanced_restricted_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.screens_enhanced_restricted_body),
                style = MaterialTheme.typography.bodyMedium,
            )
            Bullet(stringResource(R.string.screens_enhanced_restricted_step1))
            Bullet(stringResource(R.string.screens_enhanced_restricted_step2))
            Bullet(stringResource(R.string.screens_enhanced_restricted_step3))
            Text(
                stringResource(
                    R.string.screens_enhanced_restricted_missing,
                    stringResource(R.string.enhanced_service_label),
                ),
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedButton(onClick = onOpenAppInfo, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.screens_enhanced_open_app_info)) }
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
                stringResource(R.string.screens_enhanced_consent),
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
