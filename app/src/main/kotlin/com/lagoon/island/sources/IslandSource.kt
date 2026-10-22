package com.lagoon.island.sources

import kotlinx.coroutines.CoroutineScope

/**
 * A producer of [com.lagoon.island.engine.IslandActivity]. Sources post into
 * [com.lagoon.island.engine.ActivityRegistry]; they never touch the UI.
 *
 * Register with Hilt multibinding:
 * ```
 * @Module @InstallIn(SingletonComponent::class)
 * abstract class MediaSourceModule { @Binds @IntoSet abstract fun media(s: MediaSource): IslandSource }
 * ```
 */
interface IslandSource {
    /** Begin producing. [scope] is cancelled on [stop]; use it for collectors and tickers. */
    fun start(scope: CoroutineScope)
    fun stop()
}
