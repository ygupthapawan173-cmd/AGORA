package com.agora.assistant

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

 data class LaunchableApp(val label: String, val packageName: String)

object AccessibilityAppRepository {
    private const val PREFS = "jarvis_accessibility"
    private const val KEY_SELECTED = "selected_packages"
    private const val KEY_PAUSED = "paused"

    fun isPaused(context: Context): Boolean = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_PAUSED, false)

    fun setPaused(context: Context, paused: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_PAUSED, paused).apply()
        JarvisAccessibilityService.instance?.refreshPackageScope()
    }

    fun selected(context: Context): Set<String> = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getStringSet(KEY_SELECTED, emptySet())?.toSet().orEmpty()

    fun save(context: Context, packages: Set<String>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putStringSet(KEY_SELECTED, packages.toSet()).apply()
        JarvisAccessibilityService.instance?.refreshPackageScope()
    }

    fun isAllowed(context: Context, packageName: String?): Boolean {
        if (isPaused(context) || packageName.isNullOrBlank() || isSensitivePackage(packageName)) return false
        return selected(context).contains(packageName)
    }

    fun isSensitivePackage(packageName: String): Boolean {
        val p = packageName.lowercase()
        return listOf(
            "bank", "wallet", "upi", "phonepe", "paytm", "gpay", "googlepay", "cred", "finance", "trading", "broker",
            "bhim", "mobikwik", "amazonpay", "payzapp", "yono", "sbi", "hdfc", "icici", "axisbank", "kotak", "canara", "pnb", "idfc", "indusind", "razorpay"
        ).any { p.contains(it) }
    }

    fun launcherApps(context: Context): List<LaunchableApp> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            .mapNotNull { resolve ->
                val pkg = resolve.activityInfo?.packageName ?: return@mapNotNull null
                if (pkg == context.packageName || isSensitivePackage(pkg)) return@mapNotNull null
                LaunchableApp(resolve.loadLabel(pm).toString(), pkg)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }
}
