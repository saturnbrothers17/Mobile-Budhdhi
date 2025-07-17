package com.mobilebudhdhi.controller.actions

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppController(private val context: Context) {
    
    private val packageManager = context.packageManager
    
    suspend fun openApp(appName: String): String = withContext(Dispatchers.IO) {
        try {
            // First try to wake the device if it's locked
            val systemController = SystemController(context)
            systemController.wakeScreen()
            
            // Then try to open the app
            val packageName = getPackageNameForApp(appName)
            if (packageName != null) {
                val intent = packageManager.getLaunchIntentForPackage(packageName)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    context.startActivity(intent)
                    
                    // Use accessibility service to verify app opened
                    kotlinx.coroutines.delay(1500)
                    val accessibilityService = com.mobilebudhdhi.controller.services.MobileBuddhiAccessibilityService.getInstance()
                    if (accessibilityService != null) {
                        val screenText = accessibilityService.getAllTextFromScreen()
                        if (screenText.isNotEmpty()) {
                            return@withContext "Opened $appName successfully"
                        }
                    }
                    
                    "Opening $appName"
                } else {
                    "Cannot open $appName. App not found."
                }
            } else {
                // Try to search for the app in the app drawer
                val launcherIntent = Intent(Intent.ACTION_MAIN)
                launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER)
                launcherIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launcherIntent)
                
                // Use accessibility service to search for the app
                kotlinx.coroutines.delay(1000)
                val accessibilityService = com.mobilebudhdhi.controller.services.MobileBuddhiAccessibilityService.getInstance()
                if (accessibilityService != null) {
                    val appFound = accessibilityService.findAndClickText(appName)
                    if (appFound) {
                        return@withContext "Found and opened $appName"
                    }
                }
                
                "App $appName not found on your device"
            }
        } catch (e: Exception) {
            "Error opening $appName: ${e.message}"
        }
    }
    
    suspend fun closeApp(appName: String): String = withContext(Dispatchers.IO) {
        try {
            // Try to use accessibility service to close the app
            val accessibilityService = com.mobilebudhdhi.controller.services.MobileBuddhiAccessibilityService.getInstance()
            if (accessibilityService != null) {
                accessibilityService.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_RECENTS)
                // Wait a moment for recents to appear
                kotlinx.coroutines.delay(500)
                // Swipe up to close the app
                val displayMetrics = context.resources.displayMetrics
                val screenWidth = displayMetrics.widthPixels
                val screenHeight = displayMetrics.heightPixels
                accessibilityService.performSwipe(
                    screenWidth / 2f,
                    screenHeight / 2f,
                    screenWidth / 2f,
                    screenHeight * 0.1f
                )
                "Closing $appName"
            } else {
                "Accessibility service not running, cannot close app"
            }
        } catch (e: Exception) {
            "Error closing $appName: ${e.message}"
        }
    }
    
    suspend fun searchWeb(query: String): String = withContext(Dispatchers.IO) {
        try {
            val searchIntent = Intent(Intent.ACTION_WEB_SEARCH)
            searchIntent.putExtra("query", query)
            searchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(searchIntent)
            "Searching for '$query'"
        } catch (e: Exception) {
            try {
                // Fallback to browser with Google search
                val uri = android.net.Uri.parse("https://www.google.com/search?q=${android.net.Uri.encode(query)}")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                "Searching for '$query' in browser"
            } catch (e2: Exception) {
                "Error searching for '$query': ${e2.message}"
            }
        }
    }
    
    private fun getPackageNameForApp(appName: String): String? {
        val commonApps = mapOf(
            "whatsapp" to "com.whatsapp",
            "instagram" to "com.instagram.android",
            "facebook" to "com.facebook.katana",
            "twitter" to "com.twitter.android",
            "youtube" to "com.google.android.youtube",
            "gmail" to "com.google.android.gm",
            "chrome" to "com.android.chrome",
            "maps" to "com.google.android.apps.maps",
            "camera" to "com.android.camera2",
            "gallery" to "com.google.android.apps.photos",
            "settings" to "com.android.settings",
            "calculator" to "com.google.android.calculator",
            "calendar" to "com.google.android.calendar",
            "contacts" to "com.android.contacts",
            "messages" to "com.google.android.apps.messaging",
            "phone" to "com.google.android.dialer",
            "music" to "com.google.android.music",
            "spotify" to "com.spotify.music"
        )
        
        return commonApps[appName.lowercase()] ?: findAppByName(appName)
    }
    
    private fun findAppByName(appName: String): String? {
        val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        
        return installedApps.find { app ->
            val label = packageManager.getApplicationLabel(app).toString()
            label.lowercase().contains(appName.lowercase())
        }?.packageName
    }
}