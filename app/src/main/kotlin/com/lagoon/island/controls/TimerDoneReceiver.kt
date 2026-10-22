package com.lagoon.island.controls

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Fired by AlarmManager when the built-in timer reaches zero. Declared in the manifest.
 * Resolves [LagoonTimer] through the application component rather than @AndroidEntryPoint so the
 * receiver works identically with or without Hilt's bytecode transform.
 */
class TimerDoneReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FIRE) return
        ControlsEntryPoint.from(context).timer().onFired()
    }

    companion object {
        const val ACTION_FIRE = "com.lagoon.island.action.TIMER_FIRE"
    }
}
