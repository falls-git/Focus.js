package com.example.focusjs

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

class AppManager(private val context: Context) {

    // 1. Data model for App Information
    data class AppInfo(
        val label: String,
        val packageName: String
    )

    // 2. Fetch installed launchable apps
    fun getInstalledApps(): List<AppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        return pm.queryIntentActivities(intent, 0).mapNotNull { resolveInfo ->
            val label = resolveInfo.loadLabel(pm).toString()
            val packageName = resolveInfo.activityInfo.packageName
            if (label.isNotEmpty() && packageName.isNotEmpty()) {
                AppInfo(label, packageName)
            } else null
        }.sortedBy { it.label.lowercase() }
    }

    fun launchApp(packageName: String) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            context.startActivity(launchIntent)
        }
    }
}