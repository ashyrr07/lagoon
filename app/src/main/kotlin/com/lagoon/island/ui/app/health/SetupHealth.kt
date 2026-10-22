package com.lagoon.island.ui.app.health

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import com.lagoon.island.R
import com.lagoon.island.data.IslandSettings
import com.lagoon.island.service.IslandController
import com.lagoon.island.ui.app.Permissions
import com.lagoon.island.ui.app.components.Health
import com.lagoon.island.util.Oem
import com.lagoon.island.util.OemInfo
import com.lagoon.island.util.SystemScreens

enum class CheckId { Overlay, NotificationAccess, Listener, Battery, Oem }

/** One line of the setup checklist, shared by Home, Health and the last onboarding step. */
data class SetupCheck(
    val id: CheckId,
    val title: String,
    val detail: String,
    val health: Health,
    val actionLabel: String? = null,
)

@Composable
fun setupChecks(
    permissions: Permissions,
    island: IslandController.State,
    settings: IslandSettings,
): List<SetupCheck> = buildList {
    add(
        SetupCheck(
            CheckId.Overlay,
            stringResource(R.string.setup_permission_overlay),
            if (permissions.overlay) {
                stringResource(R.string.setup_status_allowed)
            } else {
                stringResource(R.string.setup_check_overlay_needed)
            },
            if (permissions.overlay) Health.Ok else Health.Attention,
            actionLabel = if (permissions.overlay) null else stringResource(R.string.setup_check_allow),
        ),
    )
    add(
        SetupCheck(
            CheckId.NotificationAccess,
            stringResource(R.string.setup_permission_notification_access),
            if (permissions.notificationAccess) {
                stringResource(R.string.setup_status_granted)
            } else {
                stringResource(R.string.setup_check_notification_needed)
            },
            if (permissions.notificationAccess) Health.Ok else Health.Attention,
            actionLabel = if (permissions.notificationAccess) null else stringResource(R.string.setup_check_grant),
        ),
    )
    add(
        SetupCheck(
            CheckId.Listener,
            stringResource(R.string.setup_check_service_title),
            when {
                !permissions.notificationAccess -> stringResource(R.string.setup_check_service_needs_access)
                island.listenerBound && island.showing -> stringResource(R.string.setup_check_service_showing)
                island.listenerBound && !settings.islandEnabled -> stringResource(R.string.setup_check_service_island_off)
                island.listenerBound -> stringResource(R.string.setup_check_service_hidden)
                else -> stringResource(R.string.setup_check_service_waiting)
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
            stringResource(R.string.setup_check_battery_title),
            when {
                permissions.ignoringBatteryOptimizations -> stringResource(R.string.setup_check_battery_unrestricted)
                settings.keepAlive.disconnectCount > 0 ->
                    pluralStringResource(
                        R.plurals.setup_check_battery_disconnected,
                        settings.keepAlive.disconnectCount,
                        settings.keepAlive.disconnectCount,
                    )
                else -> stringResource(R.string.setup_check_battery_optimised)
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
                stringResource(R.string.setup_check_oem_title, permissions.oem.brandName),
                stringResource(R.string.setup_check_oem_detail),
                Health.Attention,
                actionLabel = stringResource(R.string.setup_check_guide),
            ),
        )
    }
}

/** Opens the system screen that fixes a check. OEM guides are in-app, handled by the caller. */
fun openSystemScreen(context: Context, id: CheckId) {
    when (id) {
        CheckId.Overlay -> SystemScreens.overlay(context)
        CheckId.NotificationAccess, CheckId.Listener -> SystemScreens.notificationAccess(context)
        CheckId.Battery -> SystemScreens.battery(context)
        CheckId.Oem -> {
            val oemScreen = OemInfo.guide().steps.firstNotNullOfOrNull { OemInfo.resolve(context, it) }
            if (oemScreen == null || !SystemScreens.open(context, oemScreen)) SystemScreens.battery(context)
        }
    }
}

fun openAppInfo(context: Context) = SystemScreens.appInfo(context)

/**
 * Play policy allows this for apps whose core function breaks under Doze; it is only shown when
 * the user taps it themselves, never during onboarding.
 */
fun requestBatteryExemption(context: Context) = SystemScreens.battery(context)
