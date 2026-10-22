package com.lagoon.island

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class LagoonApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Keep-alive service is disabled for now: stop any instance left over from an older build.
        com.lagoon.island.service.KeepAliveService.stop(this)
    }
}
