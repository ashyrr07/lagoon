package com.lagoon.island.ui.app.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lagoon.island.BuildConfig
import com.lagoon.island.R
import com.lagoon.island.ui.app.Permissions
import com.lagoon.island.ui.app.apps.AppRow
import com.lagoon.island.ui.app.apps.Allowlist
import com.lagoon.island.ui.app.apps.AppsViewModel
import com.lagoon.island.ui.app.enhanced.EnhancedDisclosure
import com.lagoon.island.ui.app.enhanced.EnhancedViewModel
import com.lagoon.island.util.SystemScreens

/** Whether the setup checklist has what the island needs to run (the optional row does not count). */
fun Permissions.readyForIsland(): Boolean = overlay && notificationAccess

/**
 * Page 1: a light checklist, one row per switch, each opening exactly the right system screen.
 * Ticks update when the person comes back from Settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ColumnScope.SetupChecklistPage(
    permissions: Permissions,
    enhanced: EnhancedViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val enhancedSettings by enhanced.settings.collectAsStateWithLifecycle()
    val enhancedStatus by enhanced.status.collectAsStateWithLifecycle()
    var showEnhanced by rememberSaveable { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        enhanced.refresh()
        onPauseOrDispose { }
    }

    Spacer(Modifier.heightIn(min = 24.dp))
    Text(
        stringResource(R.string.onb_title),
        style = MaterialTheme.typography.displaySmall,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    Text(
        stringResource(R.string.onb_subtitle),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.heightIn(min = 8.dp))

    ChecklistRow(
        title = stringResource(R.string.onb_overlay),
        detail = stringResource(R.string.onb_overlay_detail),
        done = permissions.overlay,
        onClick = { SystemScreens.overlay(context) },
    )
    ChecklistRow(
        title = stringResource(R.string.onb_notif),
        detail = stringResource(R.string.onb_notif_detail),
        done = permissions.notificationAccess,
        onClick = { SystemScreens.notificationAccess(context) },
    )
    if (permissions.sideloaded && !permissions.notificationAccess) {
        RestrictedHint(onClick = { SystemScreens.appInfo(context) })
    }
    if (BuildConfig.ENHANCED_MODE_AVAILABLE) {
        ChecklistRow(
            title = stringResource(R.string.onb_enhanced),
            detail = stringResource(R.string.onb_enhanced_detail),
            done = enhancedStatus.connected || enhancedStatus.enabledInSettings,
            onClick = { showEnhanced = true },
        )
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Rounded.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.onb_privacy_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.onb_privacy_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (showEnhanced) {
        // Play policy: the prominent disclosure and consent come before the system screen.
        ModalBottomSheet(
            onDismissRequest = { showEnhanced = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(stringResource(R.string.onb_enhanced_sheet_title), style = MaterialTheme.typography.headlineSmall)
                if (enhancedStatus.connected) {
                    Text(
                        stringResource(R.string.onb_enhanced_done),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                EnhancedDisclosure(enhancedSettings, enhancedStatus, enhanced, showLockScreenToggle = false)
            }
        }
    }
}

@Composable
private fun ChecklistRow(
    title: String,
    detail: String,
    done: Boolean,
    onClick: () -> Unit,
    badge: String? = null,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .clip24()
            .clickable(onClick = onClick),
    ) {
        Row(
            Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (badge != null && !done) {
                    Text(
                        badge.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(16.dp))
            StatusDot(done)
        }
    }
}

/** Filled tick when done; an outlined circle with a chevron while it still needs a tap. */
@Composable
private fun StatusDot(done: Boolean) {
    val primary = MaterialTheme.colorScheme.primary
    Box(
        Modifier
            .size(40.dp)
            .then(
                if (done) {
                    Modifier.background(primary, CircleShape)
                } else {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (done) Icons.Rounded.Check else Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = if (done) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RestrictedHint(onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        Text(
            stringResource(R.string.onb_restricted),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onClick) { Text(stringResource(R.string.onb_restricted_action)) }
    }
}

private fun Modifier.clip24(): Modifier = this.then(Modifier.clip(RoundedCornerShape(24.dp)))

/** Page 3: which apps' notifications appear on the island. Empty selection = all apps. */
@Composable
internal fun ColumnScope.AppsPage(viewModel: AppsViewModel = hiltViewModel()) {
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val allowlist = settings.features.notificationApps
    val all = remember(apps) { apps.orEmpty().map { it.packageName }.toSet() }

    Text(stringResource(R.string.onb_apps_title), style = MaterialTheme.typography.displaySmall)
    Text(
        stringResource(R.string.onb_apps_body),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.onb_apps_all), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.onb_apps_all_detail),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = allowlist.isEmpty(),
                    onCheckedChange = { on -> if (on) viewModel.showAllApps() },
                )
            }
            val list = apps
            if (list == null) {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                list.forEach { app ->
                    AppRow(
                        app = app,
                        checked = Allowlist.isShown(allowlist, app.packageName),
                        onCheckedChange = { show -> viewModel.setShown(app.packageName, show, all) },
                    )
                }
            }
        }
    }
}
