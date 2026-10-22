package com.lagoon.island.sources

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/** Starts every bound [IslandSource] while the island is running. */
@Singleton
class SourceManager @Inject constructor(
    private val sources: Set<@JvmSuppressWildcards IslandSource>,
) {
    private var scope: CoroutineScope? = null

    fun startAll() {
        if (scope != null) return
        val s = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        scope = s
        sources.forEach { source ->
            runCatching { source.start(s) }.onFailure { Log.w(TAG, "Source failed to start: $source", it) }
        }
    }

    fun stopAll() {
        sources.forEach { source -> runCatching { source.stop() } }
        scope?.cancel()
        scope = null
    }

    companion object {
        private const val TAG = "SourceManager"
    }
}
