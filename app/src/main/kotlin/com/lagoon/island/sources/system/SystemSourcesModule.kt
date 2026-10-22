package com.lagoon.island.sources.system

import com.lagoon.island.sources.IslandSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/** Registers every system-event source with [com.lagoon.island.sources.SourceManager]. */
@Module
@InstallIn(SingletonComponent::class)
abstract class SystemSourcesModule {
    @Binds @IntoSet abstract fun battery(source: BatterySource): IslandSource
    @Binds @IntoSet abstract fun bluetooth(source: BluetoothSource): IslandSource
    @Binds @IntoSet abstract fun call(source: CallSource): IslandSource
    @Binds @IntoSet abstract fun timer(source: TimerSource): IslandSource
    @Binds @IntoSet abstract fun alarm(source: AlarmSource): IslandSource
    @Binds @IntoSet abstract fun navigation(source: NavigationSource): IslandSource
    @Binds @IntoSet abstract fun liveUpdate(source: LiveUpdateSource): IslandSource
    @Binds @IntoSet abstract fun systemState(source: SystemStateSource): IslandSource
    @Binds @IntoSet abstract fun screenState(source: ScreenStateSource): IslandSource
}
