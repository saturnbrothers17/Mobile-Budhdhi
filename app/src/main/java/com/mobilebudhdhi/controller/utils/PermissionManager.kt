package com.mobilebudhdhi.controller.utils

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.view.accessibility.AccessibilityManager
import androidx.core.content.ContextCompat

class PermissionManager {
    
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        
        return enabledServices.any { 
            it.resolveInfo.serviceInfo.packageName == context.packageName 
        }
    }
    
    fun isNotificationAccessEnabled(context: Context): Boolean {
        val enabledListeners = android.provider.Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )
        return enabledListeners?.contains(context.packageName) == true
    }
    
    fun hasRequiredPermissions(context: Context): Boolean {
        val requiredPermissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.SYSTEM_ALERT_WINDOW,
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.WAKE_LOCK
        )
        
        // Add notification permission for Android 13+
        val permissionsToCheck = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requiredPermissions + Manifest.permission.POST_NOTIFICATIONS
        } else {
            requiredPermissions
        }
        
        val permissionsGranted = permissionsToCheck.all { permission ->
            if (permission == Manifest.permission.SYSTEM_ALERT_WINDOW) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    android.provider.Settings.canDrawOverlays(context)
                } else {
                    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
                }
            } else {
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }
        }
        
        // Also check if accessibility and notification services are enabled
        return permissionsGranted && 
               isAccessibilityServiceEnabled(context) && 
               isNotificationAccessEnabled(context)
    }
    
    fun hasAllPermissions(context: Context): Boolean {
        val allPermissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.MODIFY_AUDIO_SETTINGS,
            Manifest.permission.SYSTEM_ALERT_WINDOW,
            Manifest.permission.FOREGROUND_SERVICE
        )
        
        // Add notification permission for Android 13+
        val permissionsToCheck = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            allPermissions + Manifest.permission.POST_NOTIFICATIONS
        } else {
            allPermissions
        }
        
        return permissionsToCheck.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
}