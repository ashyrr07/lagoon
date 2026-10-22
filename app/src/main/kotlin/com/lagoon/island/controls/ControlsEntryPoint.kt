package com.lagoon.island.controls

import android.content.Context
import com.lagoon.island.data.SettingsRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * The island composables run inside an overlay window with no Activity or ViewModel, so the deck
 * pulls its singletons straight from the application component.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ControlsEntryPoint {
    fun registry(): ControlsRegistry
    fun widgets(): Widgets
    fun timer(): LagoonTimer
    fun shortcuts(): AppShortcuts
    fun settings(): SettingsRepository

    companion object {
        fun from(context: Context): ControlsEntryPoint =
            EntryPointAccessors.fromApplication(context.applicationContext, ControlsEntryPoint::class.java)
    }
}
