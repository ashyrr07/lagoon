package com.lagoon.island.sources

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds

/** Declares the (possibly empty) set of sources; each source adds itself with @Binds @IntoSet. */
@Module
@InstallIn(SingletonComponent::class)
abstract class SourcesModule {
    @Multibinds
    abstract fun sources(): Set<IslandSource>
}
