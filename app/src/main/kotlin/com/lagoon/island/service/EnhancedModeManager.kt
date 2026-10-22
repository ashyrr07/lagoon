package com.lagoon.island.service

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.lagoon.island.overlay.AccessibilityOverlayHost
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Bridge between [IslandAccessibilityService] (bound by the system while the user has Enhanced
 * mode on) and the rest of the app, which must keep working when the service is absent.
 *
 * - [host] is the accessibility overlay host while the service is connected, else null. The
 *   controller prefers it over the app overlay and re-hosts the live window when it changes.
 * - [foregroundPackage] is the last package reported by a window-state change (never our own
 *   package, SystemUI or a keyboard). Null until the service has seen one.
 * - [keyguardLocked] mirrors [KeyguardManager.isKeyguardLocked], refreshed on screen on/off and
 *   user-present broadcasts, so the controller can hide the island under the lock screen in
 *   Standard mode.
 */
@Singleton
class EnhancedModeManager @Inject constructor() {
    private val _host = MutableStateFlow<AccessibilityOverlayHost?>(null)
    val host: StateFlow<AccessibilityOverlayHost?> = _host.asStateFlow()

    private val _foregroundPackage = MutableStateFlow<String?>(null)
    val foregroundPackage: StateFlow<String?> = _foregroundPackage.asStateFlow()

    private val _keyguardLocked = MutableStateFlow(false)
    val keyguardLocked: StateFlow<Boolean> = _keyguardLocked.asStateFlow()

    /** True while the accessibility service is bound and has handed us a host. */
    val isConnected: Boolean get() = _host.value != null

    // ---- Called by the service --------------------------------------------------------------

    fun onServiceConnected(host: AccessibilityOverlayHost) {
        Log.i(TAG, "Enhanced mode connected")
        _host.value = host
    }

    /** Only clears the host the caller registered: a stale service instance must not evict a new one. */
    fun onServiceDisconnected(host: AccessibilityOverlayHost) {
        if (_host.value === host) {
            Log.i(TAG, "Enhanced mode disconnected")
            _host.value = null
            _foregroundPackage.value = null
        }
    }

    fun onForegroundPackage(packageName: String) {
        _foregroundPackage.value = packageName
    }

    // ---- Keyguard tracking (used in both modes) ----------------------------------------------

    private var keyguardReceiver: BroadcastReceiver? = null

    fun startKeyguardWatch(context: Context) {
        if (keyguardReceiver != null) return
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) = readKeyguard(ctx)
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        context.registerReceiver(receiver, filter)
        keyguardReceiver = receiver
        readKeyguard(context)
    }

    fun stopKeyguardWatch(context: Context) {
        keyguardReceiver?.let { runCatching { context.unregisterReceiver(it) } }
        keyguardReceiver = null
    }

    private fun readKeyguard(context: Context) {
        val km = context.getSystemService(KeyguardManager::class.java)
        _keyguardLocked.value = km?.isKeyguardLocked == true
    }

    // ---- System settings helpers -------------------------------------------------------------

    fun serviceComponent(context: Context) = ComponentName(context, IslandAccessibilityService::class.java)

    /** Whether the user has switched our service on in Accessibility settings (bound or not yet). */
    fun isServiceEnabled(context: Context): Boolean {
        val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        val component = serviceComponent(context)
        return isServiceListed(enabled, component.packageName, component.className)
    }

    /** Straight to Lagoon's own switch in Accessibility settings, with fallbacks. */
    fun openAccessibilitySettings(context: Context) {
        com.lagoon.island.util.SystemScreens.accessibility(context)
    }

    /** Opens our App info page, where "Allow restricted settings" lives in the overflow menu. */
    fun openAppInfo(context: Context) {
        com.lagoon.island.util.SystemScreens.appInfo(context)
    }

    /**
     * Android 13+ blocks accessibility for apps that were sideloaded (installed from a local or
     * downloaded file, or by a non-session installer) until the user taps "Allow restricted
     * settings" on the App info page. Store installs are exempt. Errs on the side of true.
     */
    fun isSideloaded(context: Context): Boolean {
        val info = runCatching { context.packageManager.getInstallSourceInfo(context.packageName) }
            .getOrElse { return true }
        val installer = info.installingPackageName
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when (info.packageSource) {
                PackageInstaller.PACKAGE_SOURCE_STORE -> return false
                PackageInstaller.PACKAGE_SOURCE_LOCAL_FILE,
                PackageInstaller.PACKAGE_SOURCE_DOWNLOADED_FILE,
                -> return true
            }
        }
        return installer == null || installer !in STORE_INSTALLERS
    }

    companion object {
        private const val TAG = "EnhancedModeManager"

        /** Installers whose session-based installs are exempt from restricted settings. */
        private val STORE_INSTALLERS = setOf(
            "com.android.vending",
            "com.google.android.packageinstaller",
            "org.fdroid.fdroid",
            "org.fdroid.basic",
            "com.aurora.store",
        )

        /**
         * Parses the colon-separated ENABLED_ACCESSIBILITY_SERVICES value. Entries may be written
         * in full ("pkg/pkg.Class") or short ("pkg/.Class") form and are matched case-insensitively.
         */
        fun isServiceListed(enabledServices: String?, packageName: String, className: String): Boolean {
            if (enabledServices.isNullOrBlank()) return false
            val full = "$packageName/$className"
            val short = if (className.startsWith(packageName)) {
                "$packageName/${className.removePrefix(packageName)}"
            } else {
                null
            }
            return enabledServices.split(':').any { raw ->
                val entry = raw.trim()
                entry.equals(full, ignoreCase = true) || (short != null && entry.equals(short, ignoreCase = true))
            }
        }
    }
}
