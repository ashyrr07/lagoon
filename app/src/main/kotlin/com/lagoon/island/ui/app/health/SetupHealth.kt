package com.lagoon.island.ui.app.health

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri
import com.lagoon.island.data.IslandSettings
import com.lagoon.island.service.IslandController
import com.lagoon.island.ui.app.Permissions
import com.lagoon.island.ui.app.components.Health
import com.lagoon.island.util.Oem
import com.lagoon.island.util.OemInfo

enum class CheckId { Overlay, NotificationAccess, Listener, Battery, Oem }

/** One line of the setup checklist, shared by Home, Health and the last onboarding step. */
data class SetupCheck(
    val id: CheckId,
    val title: String,
    val detail: String,
    val health: Health,
    val actionLabel: String? = null,
)

fun setupChecks(
    permissions: Permissions,
    island: IslandController.State,
    settings: IslandSettings,
): List<SetupCheck> = buildList {
    add(
        SetupCheck(
            CheckId.Overlay,
            "Display over other apps",
            if (permissions.overlay) "Allowed" else "Needed to draw the island",
            if (permissions.overlay) Health.Ok else Health.Attention,
            actionLabel = if (permissions.overlay) null else "Allow",
        ),
    )
    add(
        SetupCheck(
            CheckId.NotificationAccess,
            "Notification access",
            if (permissions.notificationAccess) "Granted" else "Needed for music, calls and alerts",
            if (permissions.notificationAccess) Health.Ok else Health.Attention,
            actionLabel = if (permissions.notificationAccess) null else "Grant",
        ),
    )
    add(
        SetupCheck(
            CheckId.Listener,
            "Service running",
            when {
                !permissions.notificationAccess -> "Starts once notification access is granted"
                island.listenerBound && island.showing -> "Connected, island showing"
                island.listenerBound && !settings.islandEnabled -> "Connected, island switched off"
                island.listenerBound -> "Connected, island hidden for now"
                else -> "Waiting for Android to connect it"
            },
            when {
                !permissions.notificationAccess -> Health.Off
                island.listenerBound -> Health.Ok
                else -> Health.Attention
            },
        ),
    )
    add(
        SetupCheck(
            CheckId.Battery,
            "Battery",
            when {
                permissions.ignoringBatteryOptimizations -> "Unrestricted"
                settings.keepAlive.disconnectCount > 0 ->
                    "Optimised. The service was disconnected ${settings.keepAlive.disconnectCount}×"
                else -> "Optimised by Android (fine on most phones)"
            },
            when {
                permissions.ignoringBatteryOptimizations -> Health.Ok
                settings.keepAlive.disconnectCount > 0 -> Health.Attention
                else -> Health.Off
            },
        ),
    )
    if (permissions.oem != Oem.Other) {
        add(
            SetupCheck(
                CheckId.Oem,
                "${permissions.oem.brandName} background rules",
                "This brand stops background apps unless you allow Lagoon by hand",
                Health.Attention,
                actionLabel = "Guide",
            ),
        )
    }
}

/** Opens the system screen that fixes a check. OEM guides are in-app, handled by the caller. */
fun openSystemScreen(context: Context, id: CheckId) {
    val pkg = "package:${context.packageName}".toUri()
    val intent = when (id) {
        CheckId.Overlay -> Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, pkg)
        CheckId.NotificationAccess -> Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        CheckId.Listener -> Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        CheckId.Battery -> Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkg)
        CheckId.Oem -> OemInfo.guide().steps.firstNotNullOfOrNull { OemInfo.resolve(context, it) }
            ?: Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkg)
    }
    runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

fun openAppInfo(context: Context) {
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

/**
 * Play policy allows this for apps whose core function breaks under Doze; it is offered only after
 * the listener was observed disconnecting, never during onboarding.
 */
@SuppressLint("BatteryLife")
fun requestBatteryExemption(context: Context) {
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, "package:${context.packageName}".toUri())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
