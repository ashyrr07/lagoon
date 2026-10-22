package com.lagoon.island.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Launchable apps (the manifest `<queries>` for MAIN/LAUNCHER is all we can see without
 * QUERY_ALL_PACKAGES) with label and icon, loaded once off the main thread and cached.
 * Also a small label cache for sources that only need a package's display name.
 */
@Singleton
class AppsRepository @Inject constructor(@ApplicationContext private val context: Context) {

    data class AppInfo(val packageName: String, val label: String, val icon: Drawable)

    private val pm: PackageManager get() = context.packageManager
    private val mutex = Mutex()
    private var cache: List<AppInfo>? = null
    private val labels = ConcurrentHashMap<String, String>()

    /** Launchable apps sorted by label; [refresh] forces a re-query (e.g. after a package change). */
    suspend fun launchableApps(refresh: Boolean = false): List<AppInfo> = mutex.withLock {
        cache?.takeUnless { refresh } ?: withContext(Dispatchers.IO) { load() }.also { cache = it }
    }

    /** Display name of a package, or the package name itself when it is not visible to us. Cached. */
    fun labelOf(packageName: String): String = labels.getOrPut(packageName) {
        runCatching { pm.getApplicationLabel(applicationInfo(packageName)).toString() }
            .getOrNull()?.takeIf { it.isNotBlank() } ?: packageName
    }

    fun launchIntent(packageName: String): Intent? = runCatching { pm.getLaunchIntentForPackage(packageName) }.getOrNull()

    private fun load(): List<AppInfo> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved: List<ResolveInfo> = if (Build.VERSION.SDK_INT >= 33) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }
        return resolved
            .asSequence()
            .map { it.activityInfo }
            .filter { it.packageName != context.packageName }
            .distinctBy { it.packageName }
            .map { info ->
                val label = runCatching { info.applicationInfo.loadLabel(pm).toString() }.getOrNull()
                    ?.takeIf { it.isNotBlank() } ?: info.packageName
                labels[info.packageName] = label
                AppInfo(info.packageName, label, info.applicationInfo.loadIcon(pm))
            }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
            .toList()
    }

    private fun applicationInfo(packageName: String): ApplicationInfo =
        if (Build.VERSION.SDK_INT >= 33) {
            pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getApplicationInfo(packageName, 0)
        }
}
