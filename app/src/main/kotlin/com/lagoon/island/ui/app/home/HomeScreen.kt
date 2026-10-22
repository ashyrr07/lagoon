package com.lagoon.island.ui.app.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Gesture
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lagoon.island.R
import com.lagoon.island.data.IslandSettings
import com.lagoon.island.service.IslandController
import com.lagoon.island.ui.app.Permissions
import com.lagoon.island.ui.app.SettingsViewModel
import com.lagoon.island.ui.app.components.Health
import com.lagoon.island.ui.app.components.IslandPreview
import com.lagoon.island.ui.app.components.NavRow
import com.lagoon.island.ui.app.components.SettingGroup
import com.lagoon.island.ui.app.components.StatusRow
import com.lagoon.island.ui.app.health.CheckId
import com.lagoon.island.ui.app.health.SetupCheck
import com.lagoon.island.ui.app.health.openSystemScreen
import com.lagoon.island.ui.app.health.setupChecks
import com.lagoon.island.ui.theme.GroupRadius
import com.lagoon.island.ui.theme.LagoonPreviewTheme

enum class HomeDestination { Position, Appearance, Gestures, Features, Apps, Controls, Enhanced, Demo, Health, About }

@Composable
fun HomeScreen(
    onNavigate: (HomeDestination) -> Unit,
    enhancedAvailable: Boolean,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()
    val island by viewModel.islandState.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        viewModel.refreshPermissions()
        onPauseOrDispose { }
    }

    HomeContent(
        settings = settings,
        permissions = permissions,
        island = island,
        enhancedAvailable = enhancedAvailable,
        onToggleIsland = { on -> viewModel.update { it.copy(islandEnabled = on) } },
        onNavigate = onNavigate,
    )
}

@Composable
private fun HomeContent(
    settings: IslandSettings,
    permissions: Permissions,
    island: IslandController.State,
    enhancedAvailable: Boolean,
    onToggleIsland: (Boolean) -> Unit,
    onNavigate: (HomeDestination) -> Unit,
) {
    val context = LocalContext.current
    val checks = setupChecks(permissions, island, settings)
    val attention = checks.count { it.health == Health.Attention }

    Scaffold(containerColor = MaterialTheme.colorScheme.background, contentWindowInsets = WindowInsets(0)) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Header(onAbout = { onNavigate(HomeDestination.About) })

            Hero(
                settings = settings,
                permissions = permissions,
                island = island,
                onToggle = onToggleIsland,
            )

            SetupHealthGroup(
                checks = checks,
                attention = attention,
                onCheck = { check ->
                    when {
                        check.id == CheckId.Oem || check.id == CheckId.Battery -> onNavigate(HomeDestination.Health)
                        check.health == Health.Attention -> openSystemScreen(context, check.id)
                        else -> onNavigate(HomeDestination.Health)
                    }
                },
            )

            val look = MaterialTheme.colorScheme.primary
            val content = MaterialTheme.colorScheme.secondary
            val more = MaterialTheme.colorScheme.tertiary
            SettingGroup("Look") {
                row { NavRow("Position", "Offset, width, ring", Icons.Rounded.CenterFocusStrong, look) { onNavigate(HomeDestination.Position) } }
                row { NavRow("Appearance", "Tint, size, motion", Icons.Rounded.Palette, look) { onNavigate(HomeDestination.Appearance) } }
                row { NavRow("Gestures", "Tap, hold and swipe", Icons.Rounded.Gesture, look) { onNavigate(HomeDestination.Gestures) } }
            }
            SettingGroup("Content") {
                row { NavRow("Features", "Music, calls, timers, charging", Icons.Rounded.Layers, content) { onNavigate(HomeDestination.Features) } }
                row { NavRow("Apps", "Which notifications show", Icons.Rounded.Apps, content) { onNavigate(HomeDestination.Apps) } }
                row { NavRow("Controls", "Tiles, sliders and widgets", Icons.Rounded.Tune, content) { onNavigate(HomeDestination.Controls) } }
            }
            SettingGroup("More") {
                if (enhancedAvailable) {
                    row {
                        NavRow("Enhanced mode", "Lock screen and full-pill taps", Icons.Rounded.AccessibilityNew, more) {
                            onNavigate(HomeDestination.Enhanced)
                        }
                    }
                }
                row { NavRow("Try it", "Fire sample activities", Icons.Rounded.AutoAwesome, more) { onNavigate(HomeDestination.Demo) } }
                row { NavRow("Health", "Keep Lagoon running", Icons.Rounded.MonitorHeart, more) { onNavigate(HomeDestination.Health) } }
                row { NavRow("About", "Version, licence, backup", Icons.Rounded.Info, more) { onNavigate(HomeDestination.About) } }
            }
            Spacer(Modifier.height(WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 8.dp))
        }
    }
}

@Composable
private fun Header(onAbout: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Image(
            painterResource(R.drawable.ic_lagoon_mark),
            contentDescription = null,
            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)),
        )
        Text("Lagoon", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
        IconButton(onClick = onAbout) {
            Icon(Icons.Rounded.Info, contentDescription = "About", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** The phone at the top, the master switch under it. One surface, one memorable thing. */
@Composable
private fun Hero(
    settings: IslandSettings,
    permissions: Permissions,
    island: IslandController.State,
    onToggle: (Boolean) -> Unit,
) {
    val enabled = permissions.allGranted
    val status = when {
        !permissions.allGranted -> "Finish the setup below to turn it on"
        !settings.islandEnabled -> "Off"
        island.showing -> "Showing around your camera"
        island.listenerBound -> "Hidden for now (landscape or full-screen)"
        else -> "Waiting for Android to connect the service"
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GroupRadius))
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        IslandPreview(
            settings = settings,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
            screenHeight = 128.dp,
            inhaleOnAppear = true,
        )
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { onToggle(!settings.islandEnabled) }
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Island", style = MaterialTheme.typography.titleLarge)
                Text(status, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(12.dp))
            Switch(checked = settings.islandEnabled && enabled, onCheckedChange = onToggle, enabled = enabled)
        }
    }
}

@Composable
private fun SetupHealthGroup(checks: List<SetupCheck>, attention: Int, onCheck: (SetupCheck) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Setup health",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                when (attention) {
                    0 -> "All good"
                    1 -> "1 thing to check"
                    else -> "$attention things to check"
                },
                style = MaterialTheme.typography.labelMedium,
                color = if (attention == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
            )
        }
        SettingGroup(null) {
            checks.forEach { check ->
                row {
                    StatusRow(
                        title = check.title,
                        detail = check.detail,
                        health = check.health,
                        actionLabel = check.actionLabel,
                        onAction = { onCheck(check) },
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 1200)
@Composable
private fun HomePreview() {
    LagoonPreviewTheme {
        HomeContent(
            settings = IslandSettings(),
            permissions = Permissions(overlay = true, notificationAccess = false),
            island = IslandController.State(listenerBound = false),
            enhancedAvailable = true,
            onToggleIsland = {},
            onNavigate = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 1200)
@Composable
private fun HomeLightPreview() {
    LagoonPreviewTheme(darkTheme = false) {
        HomeContent(
            settings = IslandSettings(),
            permissions = Permissions(overlay = true, notificationAccess = true, ignoringBatteryOptimizations = true),
            island = IslandController.State(listenerBound = true, showing = true),
            enhancedAvailable = false,
            onToggleIsland = {},
            onNavigate = {},
        )
    }
}
