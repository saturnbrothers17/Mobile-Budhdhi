package com.mobilebudhdhi.controller.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.mobilebudhdhi.controller.services.VoiceControlService
import com.mobilebudhdhi.controller.utils.PermissionManager

class BootReceiver : BroadcastReceiver() {
    
    companion object {
        private const val SERVICE_START_DELAY = 10000L // 10 seconds delay
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED -> {
                // Delay service start to ensure system is fully booted
                Handler(Looper.getMainLooper()).postDelayed({
                    startMobileBuddhiService(context)
                }, SERVICE_START_DELAY)
            }
        }
    }
    
    private fun startMobileBuddhiService(context: Context) {
        try {
            // Check if user has previously enabled the service
            val prefs = context.getSharedPreferences("MobileBudhdhi", Context.MODE_PRIVATE)
            val serviceEnabled = prefs.getBoolean("service_enabled", true) // Default to true
            
            // Check if required permissions are granted
            val permissionManager = PermissionManager()
            val hasRequiredPermissions = permissionManager.hasRequiredPermissions(context)
            val isAccessibilityEnabled = permissionManager.isAccessibilityServiceEnabled(context)
            
            if (serviceEnabled && hasRequiredPermissions && isAccessibilityEnabled) {
                val serviceIntent = Intent(context, VoiceControlService::class.java)
                
                // Start service based on Android version
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
                
                // Log service start
                prefs.edit().putLong("last_service_start", System.currentTimeMillis()).apply()
            }
        } catch (e: Exception) {
            // Log error
            val prefs = context.getSharedPreferences("MobileBudhdhi", Context.MODE_PRIVATE)
            prefs.edit().putString("last_error", e.message ?: "Unknown error").apply()
        }
    }
}