package com.lagoon.island.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lagoon.island.data.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * After boot (or an app update) the system rebinds the notification listener on its own; this only
 * restarts the optional keep-alive service when the user turned it on.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var repository: SettingsRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (repository.settings.first().keepAlive.foregroundService) {
                    KeepAliveService.start(context.applicationContext)
                }
                IslandNotificationListener.requestRebind(context.applicationContext)
            } finally {
                pending.finish()
            }
        }
    }
}
