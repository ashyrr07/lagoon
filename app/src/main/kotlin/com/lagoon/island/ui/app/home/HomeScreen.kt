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
import com.lagoon.island.ui.app.language.currentLanguageTag
import com.lagoon.island.ui.app.language.SupportedLanguages
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.TouchApp
import com.lagoon.island.ui.app.components.SettingGroup
import com.lagoon.island.ui.app.components.StatusRow
import com.lagoon.island.ui.app.health.CheckId
import com.lagoon.island.ui.app.health.SetupCheck
import com.lagoon.island.ui.app.health.openSystemScreen
import com.lagoon.island.ui.app.health.setupChecks
import com.lagoon.island.ui.theme.GroupRadius
import com.lagoon.island.ui.theme.LagoonPreviewTheme

enum class HomeDestination { Position, Appearance, Gestures, Features, Apps, Controls, Enhanced, Demo, Health, Language, About }

@Composable
fun HomeScreen(
    onNavigate: (HomeDestination) -> Unit,
    enhancedAvailable: Boolean,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()
    val island by viewModel.islandState.collectAsStateWithLifecycle()
    val enhancedActive by viewModel.enhancedActive.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        viewModel.refreshPermissions()
        onPauseOrDispose { }
    }

    HomeContent(
        settings = settings,
        permissions = permissions,
        island = island,
        enhancedAvailable = enhancedAvailable,
        enhancedActive = enhancedActive,
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
    enhancedActive: Boolean = false,
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

            if (enhancedAvailable && !enhancedActive && permissions.allGranted) {
                // Standard mode cannot receive taps over the status bar (Android gives that strip to
                // the system), so point people who want to tap right at the camera to Enhanced mode.
                SettingGroup(null) {
                    row {
                        NavRow(
                            stringResource(R.string.setup_home_enhanced_hint_title),
                            stringResource(R.string.setup_home_enhanced_hint_detail),
                            Icons.Rounded.TouchApp,
                            MaterialTheme.colorScheme.tertiary,
                        ) { onNavigate(HomeDestination.Enhanced) }
                    }
                }
            }

            SetupHealthGroup(
                checks = checks,
                attention = attention,
                onCheck = { check ->
                    when {
                        check.id == CheckId.Oem -> onNavigate(HomeDestination.Health)
                        // Straight to the system battery dialog for Lagoon, not via the Health screen.
                        check.id == CheckId.Battery -> openSystemScreen(context, check.id)
                        check.health == Health.Attention -> openSystemScreen(context, check.id)
                        else -> onNavigate(HomeDestination.Health)
                    }
                },
            )

            val look = MaterialTheme.colorScheme.primary
            val content = MaterialTheme.colorScheme.secondary
            val more = MaterialTheme.colorScheme.tertiary
            SettingGroup(stringResource(R.string.setup_home_group_look)) {
                row { NavRow(stringResource(R.string.setup_position_title), stringResource(R.string.setup_home_position_detail), Icons.Rounded.CenterFocusStrong, look) { onNavigate(HomeDestination.Position) } }
                row { NavRow(stringResource(R.string.setup_home_appearance), stringResource(R.string.setup_home_appearance_detail), Icons.Rounded.Palette, look) { onNavigate(HomeDestination.Appearance) } }
                row { NavRow(stringResource(R.string.setup_home_gestures), stringResource(R.string.setup_home_gestures_detail), Icons.Rounded.Gesture, look) { onNavigate(HomeDestination.Gestures) } }
            }
            SettingGroup(stringResource(R.string.setup_home_group_content)) {
                row { NavRow(stringResource(R.string.setup_home_features), stringResource(R.string.setup_home_features_detail), Icons.Rounded.Layers, content) { onNavigate(HomeDestination.Features) } }
                row { NavRow(stringResource(R.string.setup_home_apps), stringResource(R.string.setup_home_apps_detail), Icons.Rounded.Apps, content) { onNavigate(HomeDestination.Apps) } }
                row { NavRow(stringResource(R.string.setup_home_controls), stringResource(R.string.setup_home_controls_detail), Icons.Rounded.Tune, content) { onNavigate(HomeDestination.Controls) } }
            }
            SettingGroup(stringResource(R.string.setup_home_group_more)) {
                if (enhancedAvailable) {
                    row {
                        NavRow(
                            stringResource(R.string.setup_enhanced_mode),
                            stringResource(R.string.setup_home_enhanced_detail),
                            Icons.Rounded.AccessibilityNew,
                            more,
                        ) {
                            onNavigate(HomeDestination.Enhanced)
                        }
                    }
                }
                row { NavRow(stringResource(R.string.setup_home_demo), stringResource(R.string.setup_home_demo_detail), Icons.Rounded.AutoAwesome, more) { onNavigate(HomeDestination.Demo) } }
                row { NavRow(stringResource(R.string.setup_health_title), stringResource(R.string.setup_keep_lagoon_running), Icons.Rounded.MonitorHeart, more) { onNavigate(HomeDestination.Health) } }
                row {
                    NavRow(
                        stringResource(R.string.language_title),
                        SupportedLanguages.firstOrNull { it.tag == currentLanguageTag() }?.nativeName
                            ?: stringResource(R.string.language_system),
                        Icons.Rounded.Translate,
                        more,
                    ) { onNavigate(HomeDestination.Language) }
                }
                row { NavRow(stringResource(R.string.setup_about_title), stringResource(R.string.setup_home_about_detail), Icons.Rounded.Info, more) { onNavigate(HomeDestination.About) } }
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
            Icon(Icons.Rounded.Info, contentDescription = stringResource(R.string.setup_about_title), tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
        !permissions.allGranted -> stringResource(R.string.setup_home_status_finish_setup)
        !settings.islandEnabled -> stringResource(R.string.setup_home_status_off)
        island.showing -> stringResource(R.string.setup_home_status_showing)
        island.listenerBound -> stringResource(R.string.setup_home_status_hidden)
        else -> stringResource(R.string.setup_home_status_waiting)
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
                Text(stringResource(R.string.setup_home_island), style = MaterialTheme.typography.titleLarge)
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
                stringResource(R.string.setup_health_checklist),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                if (attention == 0) {
                    stringResource(R.string.setup_home_all_good)
                } else {
                    pluralStringResource(R.plurals.setup_home_things_to_check, attention, attention)
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
