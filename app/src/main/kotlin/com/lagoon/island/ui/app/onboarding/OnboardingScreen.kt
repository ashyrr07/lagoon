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
        Step.Welcome -> "Set it up" to ::next
        Step.Overlay ->
            if (permissions.overlay) "Continue" to ::next
            else "Allow display over other apps" to { openSystemScreen(context, CheckId.Overlay) }
        Step.Notifications ->
            if (permissions.notificationAccess) "Continue" to ::next
            else "Open notification access" to { openSystemScreen(context, CheckId.NotificationAccess) }
        Step.KeepAlive -> "Continue" to ::next
        Step.Calibrate -> "Looks right" to ::next
        Step.Done -> "Open Lagoon" to onFinish
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
                TextButton(onClick = onFinish) { Text("Skip") }
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
        "A Dynamic Island for your Android",
        "A pill around your camera that shows music, calls, timers, charging, earbuds and the notifications you " +
            "choose. Tap it for more, hold it for quick controls.",
    )
    SettingGroup(null) {
        row {
            StatusRow("Free, for good", "No ads, no subscriptions, no accounts", Health.Ok)
        }
        row {
            StatusRow("Open source", "GPL-3.0, on GitHub", Health.Ok)
        }
        row {
            StatusRow("No internet", "Lagoon has no network permission. Nothing can leave your phone.", Health.Ok)
        }
    }
}

@Composable
private fun ColumnScope.OverlayStep(settings: IslandSettings, permissions: Permissions) {
    IslandPreview(settings, inhaleOnAppear = true)
    Title(
        "Let it float over other apps",
        "The island is a small window that sits above whatever you are doing. Android calls this " +
            "\"Display over other apps\" and asks you to switch it on by hand.",
    )
    SettingGroup(null) {
        row {
            StatusRow(
                "Display over other apps",
                if (permissions.overlay) "Allowed" else "Switch it on, then come back",
                if (permissions.overlay) Health.Ok else Health.Attention,
            )
        }
    }
}

@Composable
private fun ColumnScope.NotificationsStep(permissions: Permissions) {
    Title(
        "Notification access",
        "This is how Lagoon knows what is playing, who is calling and what your apps want to tell you. " +
            "Here is exactly what that means.",
    )
    // Prominent disclosure, before the system screen (Play policy and plain honesty).
    SettingGroup("What Lagoon does with it") {
        row {
            ContentRow {
                Text("Reads", style = MaterialTheme.typography.titleMedium)
                Text(
                    "The title, text, icon and buttons of notifications from the apps you pick, media playback " +
                        "state and album art, and the dialer's call notification.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        row {
            ContentRow {
                Text("Keeps", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Nothing. Content stays in memory while it is on the island and is dropped afterwards. " +
                        "There is no log and no database of your notifications.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        row {
            ContentRow {
                Text("Sends", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Nothing. Lagoon has no internet permission, so it cannot upload anything, ever.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    if (permissions.sideloaded && Build.VERSION.SDK_INT >= 33) {
        val context = LocalContext.current
        SettingGroup("Installed from outside a store?") {
            row {
                ContentRow {
                    Text(
                        "Android 13 and newer grey out this setting for apps installed by hand until you allow it:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text("1. Open the setting below and try to switch Lagoon on. If it says \"restricted\", come back.", style = MaterialTheme.typography.bodyMedium)
                    Text("2. Open App info, tap the ⋮ menu, choose \"Allow restricted settings\".", style = MaterialTheme.typography.bodyMedium)
                    Text("3. Open the setting again. It will switch on now.", style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = { openAppInfo(context) }) { Text("Open App info") }
                }
            }
        }
    }
    SettingGroup(null) {
        row {
            StatusRow(
                "Notification access",
                if (permissions.notificationAccess) "Granted" else "Grant it, then come back",
                if (permissions.notificationAccess) Health.Ok else Health.Attention,
            )
        }
    }
}

@Composable
private fun ColumnScope.KeepAliveStep(oem: Oem) {
    Title(
        "Keep it running",
        if (oem == Oem.Other) {
            "Stock Android keeps Lagoon's service alive on its own. If the island ever disappears, the Health " +
                "screen shows what happened and how to fix it."
        } else {
            "${oem.brandName} phones close background apps that are not allowed by hand, and the island goes " +
                "with them. Two minutes now saves a lot of wondering later."
        },
    )
    OemGuideGroup(OemInfo.guide(oem))
    Note("Lagoon does not ask to be excluded from battery optimisation here. That option appears on the Health screen only if the service is actually stopped.")
}

@Composable
private fun ColumnScope.CalibrateStep(
    settings: IslandSettings,
    island: IslandController.State,
    set: ((Calibration) -> Calibration) -> Unit,
) {
    IslandPreview(settings)
    Title(
        "Wrap the camera",
        "Lagoon read your screen's cutout. Fine-tune it so the ring sits evenly; the real island at the top " +
            "of the screen moves as you drag.",
    )
    PositionControls(settings, island.cutout, set)
}

@Composable
private fun ColumnScope.DoneStep(settings: IslandSettings, permissions: Permissions, island: IslandController.State) {
    IslandPreview(settings, inhaleOnAppear = true)
    val checks = setupChecks(permissions, island, settings)
    val ready = permissions.allGranted
    Title(
        if (ready) "You're set" else "Almost there",
        if (ready) {
            "The island is live. Everything below can be changed later from the Home screen."
        } else {
            "The island needs the two permissions above before it can show. Home keeps this checklist until it is done."
        },
    )
    SettingGroup("Setup health") {
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
