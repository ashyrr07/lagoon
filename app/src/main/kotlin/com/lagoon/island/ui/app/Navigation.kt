package com.lagoon.island.ui.app

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.lagoon.island.ui.app.enhanced.EnhancedScreen
import com.lagoon.island.ui.app.home.HomeScreen
import com.lagoon.island.ui.app.position.PositionScreen
import kotlinx.serialization.Serializable

@Serializable data object HomeKey : NavKey
@Serializable data object PositionKey : NavKey
@Serializable data object EnhancedKey : NavKey

@Composable
fun LagoonNavigation() {
    val backStack = rememberNavBackStack(HomeKey)
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<HomeKey> {
                HomeScreen(
                    onOpenPosition = { backStack.add(PositionKey) },
                    onOpenEnhanced = { backStack.add(EnhancedKey) },
                )
            }
            entry<PositionKey> { PositionScreen(onBack = { backStack.removeLastOrNull() }) }
            entry<EnhancedKey> { EnhancedScreen(onBack = { backStack.removeLastOrNull() }) }
        },
    )
}
