package com.lagoon.island.ui.app.health

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lagoon.island.data.IslandSettings
import com.lagoon.island.data.KeepAliveSettings
import com.lagoon.island.service.IslandController
import com.lagoon.island.ui.app.Permissions
import com.lagoon.island.ui.app.SettingsViewModel
import com.lagoon.island.ui.app.components.ContentRow
import com.lagoon.island.ui.app.components.Note
import com.lagoon.island.ui.app.components.ScreenScaffold
import com.lagoon.island.ui.app.components.SettingGroup
import com.lagoon.island.ui.app.components.StatusRow
import com.lagoon.island.ui.app.components.SwitchRow
import com.lagoon.island.ui.theme.LagoonPreviewTheme
import com.lagoon.island.util.Oem
import com.lagoon.island.util.OemInfo
import java.text.DateFormat
import java.util.Date

@Composable
fun HealthScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()
    val island by viewModel.islandState.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        viewModel.refreshPermissions()
        onPauseOrDispose { }
    }

    HealthContent(
        settings = settings,
        permissions = permissions,
        island = island,
        onBack = onBack,
        onKeepAlive = { on -> viewModel.setKeepAliveService(on) },
    )
}

@Composable
private fun HealthContent(
    settings: IslandSettings,
    permissions: Permissions,
    island: IslandController.State,
    onBack: () -> Unit,
    onKeepAlive: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val checks = setupChecks(permissions, island, settings)
    val keepAlive = settings.keepAlive

    ScreenScaffold(
        title = "Health",
        subtitle = "Everything Lagoon needs to stay on screen, and what to do when a phone fights it.",
        onBack = onBack,
    ) {
        SettingGroup("Setup") {
            checks.forEach { check ->
                row {
                    StatusRow(
                        title = check.title,
                        detail = check.detail,
                        health = check.health,
                        actionLabel = check.actionLabel?.takeIf { check.id != CheckId.Oem },
                        onAction = if (check.id == CheckId.Oem) null else ({ openSystemScreen(context, check.id) }),
                    )
                }
            }
        }

        SettingGroup("Keep Lagoon running") {
            if (keepAlive.disconnectCount > 0) {
                row {
                    ContentRow {
                        Text("Android stopped the service", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${keepAlive.disconnectCount} ${if (keepAlive.disconnectCount == 1) "time" else "times"}, " +
                                "last on ${DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(keepAlive.lastDisconnectAt))}. " +
                                "Excluding Lagoon from battery optimisation tells Android to leave it alone.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (!permissions.ignoringBatteryOptimizations) {
                            FilledTonalButton(onClick = { requestBatteryExemption(context) }) {
                                Text("Request battery exemption")
                            }
                        }
                    }
                }
            }
            row {
                ContentRow {
                    Text("Battery optimisation", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "If the island disappears after a while, let Lagoon run without battery limits.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!permissions.ignoringBatteryOptimizations) {
                        FilledTonalButton(onClick = { requestBatteryExemption(context) }) {
                            Text("Open battery setting")
                        }
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OemGuideGroup(OemInfo.guide(permissions.oem))
            if (permissions.oem != Oem.Other) {
                Note(
                    "Why this is needed: some brands ship their own battery managers on top of Android's, and " +
                        "they close background apps, including notification listeners, without asking. " +
                        "dontkillmyapp.com keeps a longer list per brand.",
                )
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 1200)
@Composable
private fun HealthPreview() {
    LagoonPreviewTheme {
        HealthContent(
            settings = IslandSettings(keepAlive = KeepAliveSettings(disconnectCount = 3, lastDisconnectAt = 1_760_000_000_000)),
            permissions = Permissions(overlay = true, notificationAccess = true, oem = Oem.Samsung),
            island = IslandController.State(listenerBound = true, showing = true),
            onBack = {},
            onKeepAlive = {},
        )
    }
}
