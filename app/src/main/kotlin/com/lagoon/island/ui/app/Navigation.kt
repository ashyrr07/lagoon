package com.lagoon.island.ui.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.lagoon.island.BuildConfig
import com.lagoon.island.ui.app.about.AboutScreen
import com.lagoon.island.ui.app.appearance.AppearanceScreen
import com.lagoon.island.ui.app.components.ScreenScaffold
import com.lagoon.island.ui.app.components.Note
import com.lagoon.island.ui.app.gestures.GesturesScreen
import com.lagoon.island.ui.app.health.HealthScreen
import com.lagoon.island.ui.app.home.HomeDestination
import com.lagoon.island.ui.app.home.HomeScreen
import com.lagoon.island.ui.app.onboarding.OnboardingScreen
import com.lagoon.island.ui.app.position.PositionScreen
import kotlinx.serialization.Serializable

@Serializable data object HomeKey : NavKey
@Serializable data object PositionKey : NavKey
@Serializable data object OnboardingKey : NavKey
@Serializable data object AppearanceKey : NavKey
@Serializable data object GesturesKey : NavKey
@Serializable data object HealthKey : NavKey
@Serializable data object AboutKey : NavKey

@Composable
fun LagoonNavigation(viewModel: SettingsViewModel = hiltViewModel()) {
    val onboardingDone by viewModel.onboardingDone.collectAsStateWithLifecycle()
    when (onboardingDone) {
        // Settings not loaded yet: hold the window background rather than flashing onboarding.
        null -> Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        else -> LagoonNavGraph(startAtOnboarding = onboardingDone == false)
    }
}

@Composable
private fun LagoonNavGraph(startAtOnboarding: Boolean) {
    val backStack = rememberNavBackStack(if (startAtOnboarding) OnboardingKey else HomeKey)
    fun back() { backStack.removeLastOrNull() }
    fun open(key: NavKey) { backStack.add(key) }

    NavDisplay(
        backStack = backStack,
        onBack = { back() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider(
            // Keys declared by other parts of the app (Demo, Apps, Features, Controls, Enhanced) land
            // here until their screens are wired in.
            fallback = { key -> NavEntry(key) { ComingSoonScreen(onBack = ::back) } },
        ) {
            entry<OnboardingKey> {
                OnboardingScreen(
                    onDone = {
                        backStack.clear()
                        backStack.add(HomeKey)
                    },
                )
            }
            entry<HomeKey> {
                HomeScreen(
                    onNavigate = { destination ->
                        open(
                            when (destination) {
                                HomeDestination.Position -> PositionKey
                                HomeDestination.Appearance -> AppearanceKey
                                HomeDestination.Gestures -> GesturesKey
                                HomeDestination.Health -> HealthKey
                                HomeDestination.About -> AboutKey
                                HomeDestination.Features -> FeaturesKey
                                HomeDestination.Apps -> AppsKey
                                HomeDestination.Controls -> ControlsKey
                                HomeDestination.Enhanced -> EnhancedKey
                                HomeDestination.Demo -> DemoKey
                            },
                        )
                    },
                    enhancedAvailable = BuildConfig.ENHANCED_MODE_AVAILABLE,
                )
            }
            entry<PositionKey> { PositionScreen(onBack = ::back) }
            entry<AppearanceKey> { AppearanceScreen(onBack = ::back) }
            entry<GesturesKey> { GesturesScreen(onBack = ::back) }
            entry<HealthKey> { HealthScreen(onBack = ::back) }
            entry<AboutKey> { AboutScreen(onBack = ::back) }
        },
    )
}

@Composable
private fun ComingSoonScreen(onBack: () -> Unit) {
    ScreenScaffold(title = "Not in this build", onBack = onBack) {
        Note("This screen is wired in a later part of the app.")
    }
}
