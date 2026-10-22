package com.lagoon.island.ui.app.onboarding

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lagoon.island.R
import com.lagoon.island.data.Calibration
import com.lagoon.island.data.IslandSettings
import com.lagoon.island.service.IslandController
import com.lagoon.island.ui.app.Permissions
import com.lagoon.island.ui.app.SettingsViewModel
import com.lagoon.island.ui.app.components.ContentRow
import com.lagoon.island.ui.app.components.Health
import com.lagoon.island.ui.app.components.IslandPreview
import com.lagoon.island.ui.app.components.Note
import com.lagoon.island.ui.app.components.PrimaryButton
import com.lagoon.island.ui.app.components.SettingGroup
import com.lagoon.island.ui.app.components.StatusRow
import com.lagoon.island.ui.app.health.CheckId
import com.lagoon.island.ui.app.health.OemGuideGroup
import com.lagoon.island.ui.app.health.openAppInfo
import com.lagoon.island.ui.app.health.openSystemScreen
import com.lagoon.island.ui.app.health.setupChecks
import com.lagoon.island.ui.app.position.PositionControls
import com.lagoon.island.ui.theme.LagoonPreviewTheme
import com.lagoon.island.util.Oem
import com.lagoon.island.util.OemInfo

enum class Step { Welcome, Overlay, Notifications, KeepAlive, Calibrate, Done }

@Composable
fun OnboardingScreen(
    onDone: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()
    val island by viewModel.islandState.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        viewModel.refreshPermissions()
        onPauseOrDispose { }
    }

    OnboardingContent(
        settings = settings,
        permissions = permissions,
        island = island,
        setCalibration = { transform -> viewModel.update { it.copy(calibration = transform(it.calibration)) } },
        onFinish = {
            viewModel.finishOnboarding()
            onDone()
        },
    )
}

@Composable
private fun OnboardingContent(
    settings: IslandSettings,
    permissions: Permissions,
    island: IslandController.State,
    setCalibration: ((Calibration) -> Calibration) -> Unit,
    onFinish: () -> Unit,
    initialStep: Step = Step.Welcome,
) {
    val context = LocalContext.current
    val steps = Step.entries
    var index by rememberSaveable { mutableIntStateOf(initialStep.ordinal) }
    val step = steps[index]
    fun next() { if (index < steps.lastIndex) index++ else onFinish() }
    BackHandler(enabled = index > 0) { index-- }

    val primary: Pair<String, () -> Unit> = when (step) {
        Step.Welcome -> stringResource(R.string.setup_action_set_it_up) to ::next
        Step.Overlay ->
            if (permissions.overlay) stringResource(R.string.setup_action_continue) to ::next
            else stringResource(R.string.setup_action_allow_overlay) to { openSystemScreen(context, CheckId.Overlay) }
        Step.Notifications ->
            if (permissions.notificationAccess) stringResource(R.string.setup_action_continue) to ::next
            else stringResource(R.string.setup_action_open_notification_access) to
                { openSystemScreen(context, CheckId.NotificationAccess) }
        Step.KeepAlive -> stringResource(R.string.setup_action_continue) to ::next
        Step.Calibrate -> stringResource(R.string.setup_action_looks_right) to ::next
        Step.Done -> stringResource(R.string.setup_action_open_lagoon) to onFinish
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Progress(count = steps.size, current = index, modifier = Modifier.weight(1f))
            if (step != Step.Done) {
                TextButton(onClick = onFinish) { Text(stringResource(R.string.setup_action_skip)) }
            } else {
                Spacer(Modifier.height(48.dp))
            }
        }

        AnimatedContent(
            targetState = step,
            modifier = Modifier.weight(1f),
            transitionSpec = {
                if (settings.appearance.reduceMotion) {
                    fadeIn(tween(200)) togetherWith fadeOut(tween(150))
                } else {
                    val forward = targetState.ordinal >= initialState.ordinal
                    val dir = if (forward) 1 else -1
                    (slideInHorizontally(tween(320)) { it / 6 * dir } + fadeIn(tween(240))) togetherWith
                        (slideOutHorizontally(tween(220)) { -it / 8 * dir } + fadeOut(tween(160)))
                }
            },
            label = "onboarding",
        ) { target ->
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                when (target) {
                    Step.Welcome -> WelcomeStep()
                    Step.Overlay -> OverlayStep(settings, permissions)
                    Step.Notifications -> NotificationsStep(permissions)
                    Step.KeepAlive -> KeepAliveStep(permissions.oem)
                    Step.Calibrate -> CalibrateStep(settings, island, setCalibration)
                    Step.Done -> DoneStep(settings, permissions, island)
                }
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 16.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
        ) {
            PrimaryButton(primary.first, onClick = primary.second)
        }
    }
}

/** Progress as a row of small islands: the current step is the long pill. */
@Composable
private fun Progress(count: Int, current: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { i ->
            val done = i <= current
            Box(
                Modifier
                    .height(8.dp)
                    .then(if (i == current) Modifier.weight(2.2f) else Modifier.weight(1f))
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (done) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceContainerHighest,
                    ),
            )
        }
    }
}

@Composable
private fun ColumnScope.Title(text: String, body: String) {
    Text(text, style = MaterialTheme.typography.displaySmall, modifier = Modifier.padding(top = 8.dp))
    Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ColumnScope.WelcomeStep() {
    Image(
        painterResource(R.drawable.ic_lagoon_mark),
        contentDescription = null,
        modifier = Modifier.padding(top = 24.dp).size(96.dp).clip(RoundedCornerShape(26.dp)),
    )
    Title(
        stringResource(R.string.setup_welcome_title),
        stringResource(R.string.setup_welcome_body),
    )
    SettingGroup(null) {
        row {
            StatusRow(
                stringResource(R.string.setup_welcome_free_title),
                stringResource(R.string.setup_welcome_free_detail),
                Health.Ok,
            )
        }
        row {
            StatusRow(
                stringResource(R.string.setup_open_source),
                stringResource(R.string.setup_welcome_open_source_detail),
                Health.Ok,
            )
        }
        row {
            StatusRow(
                stringResource(R.string.setup_welcome_no_internet_title),
                stringResource(R.string.setup_welcome_no_internet_detail),
                Health.Ok,
            )
        }
    }
}

@Composable
private fun ColumnScope.OverlayStep(settings: IslandSettings, permissions: Permissions) {
    IslandPreview(settings, inhaleOnAppear = true)
    Title(
        stringResource(R.string.setup_overlay_title),
        stringResource(R.string.setup_overlay_body),
    )
    SettingGroup(null) {
        row {
            StatusRow(
                stringResource(R.string.setup_permission_overlay),
                if (permissions.overlay) {
                    stringResource(R.string.setup_status_allowed)
                } else {
                    stringResource(R.string.setup_overlay_pending)
                },
                if (permissions.overlay) Health.Ok else Health.Attention,
            )
        }
    }
}

@Composable
private fun ColumnScope.NotificationsStep(permissions: Permissions) {
    Title(
        stringResource(R.string.setup_permission_notification_access),
        stringResource(R.string.setup_notifications_body),
    )
    // Prominent disclosure, before the system screen (Play policy and plain honesty).
    SettingGroup(stringResource(R.string.setup_notifications_disclosure)) {
        row {
            ContentRow {
                Text(stringResource(R.string.setup_notifications_reads_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.setup_notifications_reads_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        row {
            ContentRow {
                Text(stringResource(R.string.setup_notifications_keeps_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.setup_notifications_keeps_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        row {
            ContentRow {
                Text(stringResource(R.string.setup_notifications_sends_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.setup_notifications_sends_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    if (permissions.sideloaded && Build.VERSION.SDK_INT >= 33) {
        val context = LocalContext.current
        SettingGroup(stringResource(R.string.setup_sideload_title)) {
            row {
                ContentRow {
                    Text(
                        stringResource(R.string.setup_sideload_intro),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(stringResource(R.string.setup_sideload_step1), style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(R.string.setup_sideload_step2), style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(R.string.setup_sideload_step3), style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = { openAppInfo(context) }) { Text(stringResource(R.string.setup_action_open_app_info)) }
                }
            }
        }
    }
    SettingGroup(null) {
        row {
            StatusRow(
                stringResource(R.string.setup_permission_notification_access),
                if (permissions.notificationAccess) {
                    stringResource(R.string.setup_status_granted)
                } else {
                    stringResource(R.string.setup_notifications_pending)
                },
                if (permissions.notificationAccess) Health.Ok else Health.Attention,
            )
        }
    }
}

@Composable
private fun ColumnScope.KeepAliveStep(oem: Oem) {
    Title(
        stringResource(R.string.setup_keepalive_title),
        if (oem == Oem.Other) {
            stringResource(R.string.setup_keepalive_body_stock)
        } else {
            stringResource(R.string.setup_keepalive_body_oem, oem.brandName)
        },
    )
    OemGuideGroup(OemInfo.guide(oem))
    Note(stringResource(R.string.setup_keepalive_note))
}

@Composable
private fun ColumnScope.CalibrateStep(
    settings: IslandSettings,
    island: IslandController.State,
    set: ((Calibration) -> Calibration) -> Unit,
) {
    IslandPreview(settings)
    Title(
        stringResource(R.string.setup_calibrate_title),
        stringResource(R.string.setup_calibrate_body),
    )
    PositionControls(settings, island.cutout, set)
}

@Composable
private fun ColumnScope.DoneStep(settings: IslandSettings, permissions: Permissions, island: IslandController.State) {
    IslandPreview(settings, inhaleOnAppear = true)
    val checks = setupChecks(permissions, island, settings)
    val ready = permissions.allGranted
    Title(
        if (ready) stringResource(R.string.setup_done_title_ready) else stringResource(R.string.setup_done_title_almost),
        if (ready) {
            stringResource(R.string.setup_done_body_ready)
        } else {
            stringResource(R.string.setup_done_body_almost)
        },
    )
    SettingGroup(stringResource(R.string.setup_health_checklist)) {
        checks.forEach { check ->
            row { StatusRow(check.title, check.detail, check.health) }
        }
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun OnboardingWelcomePreview() {
    LagoonPreviewTheme {
        OnboardingContent(IslandSettings(), Permissions(), IslandController.State(), {}, {})
    }
}

@Preview(showBackground = true, heightDp = 1400)
@Composable
private fun OnboardingNotificationsPreview() {
    LagoonPreviewTheme {
        OnboardingContent(
            IslandSettings(),
            Permissions(overlay = true, sideloaded = true),
            IslandController.State(),
            {},
            {},
            initialStep = Step.Notifications,
        )
    }
}

@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun OnboardingKeepAlivePreview() {
    LagoonPreviewTheme {
        OnboardingContent(
            IslandSettings(),
            Permissions(overlay = true, notificationAccess = true, oem = Oem.Xiaomi),
            IslandController.State(),
            {},
            {},
            initialStep = Step.KeepAlive,
        )
    }
}
