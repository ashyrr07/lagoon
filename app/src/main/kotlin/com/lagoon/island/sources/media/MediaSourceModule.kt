package com.lagoon.island.sources.media

import com.lagoon.island.sources.IslandSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class MediaSourceModule {
    @Binds
    @IntoSet
    abstract fun media(source: MediaSource): IslandSource
}
