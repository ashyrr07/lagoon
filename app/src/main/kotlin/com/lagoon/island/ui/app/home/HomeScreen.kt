package com.lagoon.island.ui.app.home

import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lagoon.island.ui.app.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenPosition: () -> Unit,
    onOpenApps: () -> Unit = {},
    onOpenEnhanced: () -> Unit = {}, // Enhanced mode disclosure screen
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()
    val island by viewModel.islandState.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        viewModel.refreshPermissions()
        onPauseOrDispose { }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Lagoon") }) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Island", style = MaterialTheme.typography.titleLarge)
                        Text(
                            when {
                                !permissions.allGranted -> "Needs permissions below"
                                !settings.islandEnabled -> "Off"
                                island.showing -> "Showing"
                                island.listenerBound -> "Hidden (landscape or no overlay yet)"
                                else -> "Waiting for notification access to bind"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = settings.islandEnabled,
                        onCheckedChange = { on -> viewModel.update { it.copy(islandEnabled = on) } },
                        enabled = permissions.allGranted,
                    )
                }
            }

            PermissionRow(
                title = "Display over other apps",
                body = "Draws the island on top of whatever you are doing.",
                granted = permissions.overlay,
            ) {
                context.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}"),
                    ),
                )
            }
            PermissionRow(
                title = "Notification access",
                body = "Lets Lagoon see what is playing and the notifications you choose. " +
                    "Nothing leaves your phone: Lagoon has no internet permission.",
                granted = permissions.notificationAccess,
            ) {
                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }

            OutlinedButton(onClick = onOpenPosition, modifier = Modifier.fillMaxWidth()) {
                Text("Position and size")
            }
            OutlinedButton(onClick = onOpenApps, modifier = Modifier.fillMaxWidth()) {
                Text("Apps")
            }
            OutlinedButton(onClick = onOpenEnhanced, modifier = Modifier.fillMaxWidth()) {
                Text("Enhanced mode (optional)")
            }

            island.cutout?.let { cutout ->
                Text(
                    "Detected: ${cutout.shape} ${cutout.cutout ?: ""}, status bar ${cutout.statusBarHeight}px, " +
                        "${cutout.screenWidth}x${cutout.screenHeight}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PermissionRow(title: String, body: String, granted: Boolean, onGrant: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (granted) {
                Text("Granted", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            } else {
                Button(onClick = onGrant) { Text("Grant") }
            }
        }
    }
}
