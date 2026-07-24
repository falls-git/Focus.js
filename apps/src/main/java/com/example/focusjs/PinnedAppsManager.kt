package com.example.focusjs

import android.content.Context
import android.content.SharedPreferences

class PinnedAppsManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("focusjs_pinned_apps", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PINNED_PACKAGES = "pinned_packages"
    }

    // Get list of pinned package names
    fun getPinnedPackageNames(): List<String> {
        val saved = prefs.getStringSet(KEY_PINNED_PACKAGES, null)
        return saved?.toList() ?: emptyList()
    }

    // Pin an app
    fun pinApp(packageName: String) {
        val current = getPinnedPackageNames().toMutableSet()
        current.add(packageName)
        prefs.edit().putStringSet(KEY_PINNED_PACKAGES, current).apply()
    }

    // Unpin an app
    fun unpinApp(packageName: String) {
        val current = getPinnedPackageNames().toMutableSet()
        current.remove(packageName)
        prefs.edit().putStringSet(KEY_PINNED_PACKAGES, current).apply()
    }

    // Check if initialized
    fun isInitialized(): Boolean {
        return prefs.contains(KEY_PINNED_PACKAGES)
    }

    // Save initial defaults
    fun saveInitialDefaults(packageNames: List<String>) {
        prefs.edit().putStringSet(KEY_PINNED_PACKAGES, packageNames.toSet()).apply()
    }
}