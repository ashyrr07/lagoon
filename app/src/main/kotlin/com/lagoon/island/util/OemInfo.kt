package com.lagoon.island.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import com.lagoon.island.R
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

/**
 * One thing the user should do on this brand, with an optional deep link to the right screen.
 * Texts are string resources, read in composition through [title] and [detail] so they follow
 * the app's language.
 */
data class OemStep(
    @StringRes val titleRes: Int,
    @StringRes val detailRes: Int,
    /** Candidate settings activities, tried in order; null = no known deep link. */
    val targets: List<ComponentName> = emptyList(),
    val action: String? = null,
) {
    val title: String
        @Composable @ReadOnlyComposable get() = stringResource(titleRes)

    val detail: String
        @Composable @ReadOnlyComposable get() = stringResource(detailRes)
}

data class OemGuide(
    val oem: Oem,
    @StringRes val summaryRes: Int,
    val steps: List<OemStep>,
) {
    val summary: String
        @Composable @ReadOnlyComposable get() = stringResource(summaryRes)
}

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
            R.string.island_oem_xiaomi_summary,
            listOf(
                OemStep(
                    R.string.island_oem_xiaomi_autostart_title,
                    R.string.island_oem_xiaomi_autostart_detail,
                    listOf(
                        ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
                    ),
                ),
                OemStep(
                    R.string.island_oem_xiaomi_battery_title,
                    R.string.island_oem_xiaomi_battery_detail,
                    listOf(ComponentName("com.miui.powerkeeper", "com.miui.powerkeeper.ui.HiddenAppsConfigActivity")),
                ),
                OemStep(
                    R.string.island_oem_xiaomi_lock_title,
                    R.string.island_oem_xiaomi_lock_detail,
                ),
            ),
        )
        Oem.Samsung -> OemGuide(
            oem,
            R.string.island_oem_samsung_summary,
            listOf(
                OemStep(
                    R.string.island_oem_samsung_never_sleeping_title,
                    R.string.island_oem_samsung_never_sleeping_detail,
                    listOf(
                        ComponentName("com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity"),
                        ComponentName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity"),
                    ),
                ),
                OemStep(
                    R.string.island_oem_samsung_adaptive_title,
                    R.string.island_oem_samsung_adaptive_detail,
                ),
            ),
        )
        Oem.Huawei -> OemGuide(
            oem,
            R.string.island_oem_huawei_summary,
            listOf(
                OemStep(
                    R.string.island_oem_huawei_manual_title,
                    R.string.island_oem_huawei_manual_detail,
                    listOf(
                        ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
                        ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity"),
                    ),
                ),
                OemStep(
                    R.string.island_oem_huawei_protected_title,
                    R.string.island_oem_huawei_protected_detail,
                    listOf(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")),
                ),
            ),
        )
        Oem.Oppo -> OemGuide(
            oem,
            R.string.island_oem_oppo_summary,
            listOf(
                OemStep(
                    R.string.island_oem_oppo_autolaunch_title,
                    R.string.island_oem_oppo_autolaunch_detail,
                    listOf(
                        ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
                        ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity"),
                        ComponentName("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"),
                    ),
                ),
                OemStep(
                    R.string.island_oem_oppo_battery_title,
                    R.string.island_oem_oppo_battery_detail,
                ),
            ),
        )
        Oem.Vivo -> OemGuide(
            oem,
            R.string.island_oem_vivo_summary,
            listOf(
                OemStep(
                    R.string.island_oem_vivo_autostart_title,
                    R.string.island_oem_vivo_autostart_detail,
                    listOf(
                        ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
                        ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"),
                    ),
                ),
                OemStep(
                    R.string.island_oem_vivo_power_title,
                    R.string.island_oem_vivo_power_detail,
                ),
            ),
        )
        Oem.Other -> OemGuide(
            oem,
            R.string.island_oem_other_summary,
            listOf(
                OemStep(
                    R.string.island_oem_other_battery_title,
                    R.string.island_oem_other_battery_detail,
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
