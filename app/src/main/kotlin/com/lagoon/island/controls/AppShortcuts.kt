package com.lagoon.island.controls

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import com.lagoon.island.util.IntentLauncher
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** A launchable app, as the picker and the shortcuts row see it. */
data class AppInfo(
    val packageName: String,
    val label: String,
)

/**
 * App shortcuts for the deck. Only apps with a MAIN/LAUNCHER activity are visible, through the
 * manifest `<queries>` block (no QUERY_ALL_PACKAGES).
 */
@Singleton
class AppShortcuts @Inject constructor(
    @ApplicationContext private val context: Context,
    private val launcher: IntentLauncher,
) {
    private val pm: PackageManager = context.packageManager

    /** Every launchable app except ourselves, sorted by label. Do the call off the main thread. */
    fun launchableApps(): List<AppInfo> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = runCatching { pm.queryIntentActivities(intent, PackageManager.MATCH_ALL) }.getOrDefault(emptyList())
        return resolved
            .asSequence()
            .map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
            .filter { (pkg, _) -> pkg != context.packageName }
            .distinctBy { (pkg, _) -> pkg }
            .map { (pkg, label) -> AppInfo(pkg, label) }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    fun label(packageName: String): String? =
        runCatching { pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString() }.getOrNull()

    fun icon(packageName: String): Drawable? =
        runCatching { pm.getApplicationIcon(packageName) }.getOrNull()

    fun isInstalled(packageName: String): Boolean =
        runCatching { pm.getApplicationInfo(packageName, 0) }.isSuccess

    fun launch(packageName: String): Boolean {
        val intent = pm.getLaunchIntentForPackage(packageName) ?: return false
        return launcher.launch(intent)
    }
}
