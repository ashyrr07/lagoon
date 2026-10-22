package com.lagoon.island.sources.notifications

import com.lagoon.island.sources.IslandSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationSourceModule {
    @Binds
    @IntoSet
    abstract fun notifications(source: NotificationSource): IslandSource
}
