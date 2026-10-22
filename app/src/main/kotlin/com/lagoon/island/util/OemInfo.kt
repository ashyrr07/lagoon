package com.lagoon.island.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Locale

/**
 * Which phone brand we are on and what its battery manager wants from the user. The advice follows
 * dontkillmyapp.com: these OEMs stop background apps and unbind notification listeners unless the
 * app is excluded by hand, and none of it can be requested through a public API.
 */
enum class Oem(val brandName: String) {
    Xiaomi("Xiaomi / HyperOS"),
    Samsung("Samsung"),
    Huawei("Huawei / Honor"),
    Oppo("Oppo / OnePlus / Realme"),
    Vivo("Vivo / iQOO"),
    Other("Android"),
}

/** One thing the user should do on this brand, with an optional deep link to the right screen. */
data class OemStep(
    val title: String,
    val detail: String,
    /** Candidate settings activities, tried in order; null = no known deep link. */
    val targets: List<ComponentName> = emptyList(),
    val action: String? = null,
)

data class OemGuide(
    val oem: Oem,
    val summary: String,
    val steps: List<OemStep>,
)

object OemInfo {
    val current: Oem by lazy { detect(Build.MANUFACTURER, Build.BRAND) }

    /** True when the brand is known to kill background apps; drives the Health hint. */
    val isAggressive: Boolean get() = current != Oem.Other

    fun detect(manufacturer: String?, brand: String?): Oem {
        val m = (manufacturer ?: "").lowercase(Locale.ROOT)
        val b = (brand ?: "").lowercase(Locale.ROOT)
        fun has(vararg keys: String) = keys.any { m.contains(it) || b.contains(it) }
        return when {
            has("xiaomi", "redmi", "poco") -> Oem.Xiaomi
            has("samsung") -> Oem.Samsung
            has("huawei", "honor") -> Oem.Huawei
            has("oppo", "oneplus", "realme") -> Oem.Oppo
            has("vivo", "iqoo") -> Oem.Vivo
            else -> Oem.Other
        }
    }

    fun guide(oem: Oem = current): OemGuide = when (oem) {
        Oem.Xiaomi -> OemGuide(
            oem,
            "HyperOS and MIUI stop apps that are not on the Autostart list and apply their own battery " +
                "saver on top of Android's.",
            listOf(
                OemStep(
                    "Allow autostart",
                    "Security app > Manage apps > Lagoon > Autostart on.",
                    listOf(
                        ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
                    ),
                ),
                OemStep(
                    "No battery restrictions",
                    "Settings > Apps > Manage apps > Lagoon > Battery saver > No restrictions.",
                    listOf(ComponentName("com.miui.powerkeeper", "com.miui.powerkeeper.ui.HiddenAppsConfigActivity")),
                ),
                OemStep(
                    "Lock it in Recents",
                    "Open Recents, pull Lagoon's card down until a padlock appears, so a swipe-clear keeps it.",
                ),
            ),
        )
        Oem.Samsung -> OemGuide(
            oem,
            "One UI puts apps to sleep after a few days without use and may stop the notification " +
                "listener with them.",
            listOf(
                OemStep(
                    "Never sleeping apps",
                    "Settings > Battery > Background usage limits > Never sleeping apps > add Lagoon.",
                    listOf(
                        ComponentName("com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity"),
                        ComponentName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity"),
                    ),
                ),
                OemStep(
                    "Adaptive battery",
                    "On the same screen turn off \"Put unused apps to sleep\" if the island keeps vanishing.",
                ),
            ),
        )
        Oem.Huawei -> OemGuide(
            oem,
            "EMUI's app launch manager stops everything it does not manage by hand.",
            listOf(
                OemStep(
                    "Manage manually",
                    "Settings > Battery > App launch > Lagoon > Manage manually > allow Auto-launch, Secondary launch and Run in background.",
                    listOf(
                        ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
                        ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity"),
                    ),
                ),
                OemStep(
                    "Protected apps",
                    "On older EMUI: Phone Manager > Protected apps > Lagoon on.",
                    listOf(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")),
                ),
            ),
        )
        Oem.Oppo -> OemGuide(
            oem,
            "ColorOS and OxygenOS share one battery manager that stops apps unless they may auto-launch.",
            listOf(
                OemStep(
                    "Allow auto-launch",
                    "Settings > Apps > Lagoon > Allow auto-launch (also \"Allow other apps to launch\").",
                    listOf(
                        ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
                        ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity"),
                        ComponentName("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"),
                    ),
                ),
                OemStep(
                    "Battery usage: no limits",
                    "Settings > Battery > More settings > App battery management > Lagoon > Don't optimise, and turn off \"Sleep standby optimisation\".",
                ),
            ),
        )
        Oem.Vivo -> OemGuide(
            oem,
            "Funtouch and OriginOS need both autostart and high background power consumption allowed.",
            listOf(
                OemStep(
                    "Allow autostart",
                    "i Manager > App manager > Autostart manager > Lagoon on.",
                    listOf(
                        ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
                        ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"),
                    ),
                ),
                OemStep(
                    "High background power",
                    "Settings > Battery > Background power consumption management > Lagoon > Allow high background power consumption.",
                ),
            ),
        )
        Oem.Other -> OemGuide(
            oem,
            "Stock Android keeps notification listeners alive on its own. If the island still disappears, " +
                "check the app's battery setting.",
            listOf(
                OemStep(
                    "Battery: unrestricted",
                    "Settings > Apps > Lagoon > Battery > Unrestricted.",
                ),
            ),
        )
    }

    /** First deep link that resolves on this phone, as a launchable intent, or null. */
    fun resolve(context: Context, step: OemStep): Intent? {
        val pm = context.packageManager
        for (target in step.targets) {
            val intent = Intent().setComponent(target).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (intent.resolveActivity(pm) != null) return intent
        }
        return null
    }
}
