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
import com.lagoon.island.ui.app.enhanced.EnhancedDisclosure
import com.lagoon.island.ui.app.health.openSystemScreen
import com.lagoon.island.ui.app.health.setupChecks
import com.lagoon.island.ui.app.position.PositionControls
import com.lagoon.island.ui.theme.LagoonPreviewTheme
import com.lagoon.island.util.Oem
import com.lagoon.island.util.OemInfo

/** First run: one light checklist, then the look, then the apps. Everything else lives in settings. */
enum class Step { Setup, Style, Apps }

@Composable
fun OnboardingScreen(
    onDone: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()
    val island by viewModel.islandState.collectAsStateWithLifecycle()
    val enhanced: com.lagoon.island.ui.app.enhanced.EnhancedViewModel = hiltViewModel()
    val enhancedStatus by enhanced.status.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        viewModel.refreshPermissions()
        onPauseOrDispose { }
    }

    OnboardingContent(
        settings = settings,
        permissions = permissions,
        island = island,
        setCalibration = { transform -> viewModel.update { it.copy(calibration = transform(it.calibration)) } },
        setAppearance = { transform -> viewModel.update { it.copy(appearance = transform(it.appearance)) } },
        enhancedReady = !com.lagoon.island.BuildConfig.ENHANCED_MODE_AVAILABLE ||
            enhancedStatus.connected || enhancedStatus.enabledInSettings,
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
    setAppearance: ((com.lagoon.island.data.Appearance) -> com.lagoon.island.data.Appearance) -> Unit = {},
    initialStep: Step = Step.Setup,
    enhancedReady: Boolean = true,
) {
    val context = LocalContext.current
    val steps = Step.entries
    var index by rememberSaveable { mutableIntStateOf(steps.indexOf(initialStep).coerceAtLeast(0)) }
    val step = steps[index]
    fun next() { if (index < steps.lastIndex) index++ else onFinish() }
    BackHandler(enabled = index > 0) { index-- }

    // Every row on the checklist is required (Enhanced mode too, where the build has it).
    val setupComplete = permissions.readyForIsland() && enhancedReady
    val primary: Pair<String, () -> Unit> = when (step) {
        Step.Setup -> stringResource(if (setupComplete) R.string.onb_continue else R.string.onb_finish_checks) to ::next
        Step.Style -> stringResource(R.string.style_action) to ::next
        Step.Apps -> stringResource(R.string.onb_finish) to onFinish
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
            if (step == Step.Style) {
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
                    Step.Setup -> SetupChecklistPage(permissions)
                    Step.Style -> StyleStep(settings, setAppearance)
                    Step.Apps -> AppsPage()
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
            PrimaryButton(primary.first, onClick = primary.second, enabled = step != Step.Setup || setupComplete)
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
