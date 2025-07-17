package com.mobilebudhdhi.controller.actions

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.telephony.TelephonyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AdvancedSystemController(private val context: Context) {
    
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    
    suspend fun performAdvancedSystemAction(action: String, parameters: Map<String, String> = emptyMap()): String = withContext(Dispatchers.IO) {
        when (action.lowercase()) {
            "flashlight_on" -> toggleFlashlight(true)
            "flashlight_off" -> toggleFlashlight(false)
            "do_not_disturb_on" -> enableDoNotDisturb()
            "do_not_disturb_off" -> disableDoNotDisturb()
            "auto_rotate_on" -> enableAutoRotate()
            "auto_rotate_off" -> disableAutoRotate()
            "power_save_on" -> enablePowerSaveMode()
            "power_save_off" -> disablePowerSaveMode()
            "mobile_data_on" -> enableMobileData()
            "mobile_data_off" -> disableMobileData()
            "hotspot_on" -> enableHotspot()
            "hotspot_off" -> disableHotspot()
            "location_on" -> enableLocation()
            "location_off" -> disableLocation()
            "sync_on" -> enableSync()
            "sync_off" -> disableSync()
            "developer_options" -> openDeveloperOptions()
            "device_info" -> getDeviceInfo()
            "battery_info" -> getBatteryInfo()
            "storage_info" -> getStorageInfo()
            "memory_info" -> getMemoryInfo()
            "network_info" -> getNetworkInfo()
            "clear_cache" -> clearSystemCache()
            "restart_device" -> restartDevice()
            "emergency_mode" -> enableEmergencyMode()
            else -> "Unknown system action: $action"
        }
    }
    
    private suspend fun toggleFlashlight(enable: Boolean): String = withContext(Dispatchers.IO) {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList[0]
            cameraManager.setTorchMode(cameraId, enable)
            if (enable) "Flashlight turned on" else "Flashlight turned off"
        } catch (e: Exception) {
            "Error controlling flashlight: ${e.message}"
        }
    }
    
    private suspend fun enableDoNotDisturb(): String = withContext(Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                "Opening Do Not Disturb settings"
            } else {
                audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                "Do Not Disturb enabled"
            }
        } catch (e: Exception) {
            "Error enabling Do Not Disturb: ${e.message}"
        }
    }
    
    private suspend fun disableDoNotDisturb(): String = withContext(Dispatchers.IO) {
        try {
            audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
            "Do Not Disturb disabled"
        } catch (e: Exception) {
            "Error disabling Do Not Disturb: ${e.message}"
        }
    }
    
    private suspend fun enableAutoRotate(): String = withContext(Dispatchers.IO) {
        try {
            val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            "Opening display settings to enable auto-rotate"
        } catch (e: Exception) {
            "Error accessing auto-rotate settings: ${e.message}"
        }
    }
    
    private suspend fun disableAutoRotate(): String = withContext(Dispatchers.IO) {
        try {
            val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            "Opening display settings to disable auto-rotate"
        } catch (e: Exception) {
            "Error accessing auto-rotate settings: ${e.message}"
        }
    }
    
    private suspend fun enablePowerSaveMode(): String = withContext(Dispatchers.IO) {
        try {
            val intent = Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            "Opening battery saver settings"
        } catch (e: Exception) {
            "Error accessing power save mode: ${e.message}"
        }
    }
    
    private suspend fun disablePowerSaveMode(): String = withContext(Dispatchers.IO) {
        try {
            val intent = Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            "Opening battery saver settings to disable power save mode"
        } catch (e: Exception) {
            "Error accessing power save mode: ${e.message}"
        }
    }
    
    private suspend fun enableMobileData(): String = withContext(Dispatchers.IO) {
        try {
            val intent = Intent(Settings.ACTION_DATA_USAGE_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            "Opening mobile data settings"
        } catch (e: Exception) {
            "Error accessing mobile data settings: ${e.message}"
        }
    }
    
    private suspend fun disableMobileData(): String = withContext(Dispatchers.IO) {
        try {
            val intent = Intent(Settings.ACTION_DATA_USAGE_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            "Opening mobile data settings to disable mobile data"
        } catch (e: Exception) {
            "Error accessing mobile data settings: ${e.message}"
        }
    }
    
    private suspend fun enableHotspot(): String = withContext(Dispatchers.IO) {
        try {
            val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            "Opening wireless settings to enable hotspot"
        } catch (e: Exception) {
            "Error accessing hotspot settings: ${e.message}"
        }
    }
    
    private suspend fun disableHotspot(): String = withContext(Dispatchers.IO) {
        try {
            val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            "Opening wireless settings to disable hotspot"
        } catch (e: Exception) {
            "Error accessing hotspot settings: ${e.message}"
        }
    }
    
    private suspend fun enableLocation(): String = withContext(Dispatchers.IO) {
        try {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            "Opening location settings"
        } catch (e: Exception) {
            "Error accessing location settings: ${e.message}"
        }
    }
    
    private suspend fun disableLocation(): String = withContext(Dispatchers.IO) {
        try {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            "Opening location settings to disable location"
        } catch (e: Exception) {
            "Error accessing location settings: ${e.message}"
        }
    }
    
    private suspend fun enableSync(): String = withContext(Dispatchers.IO) {
        try {
            val intent = Intent(Settings.ACTION_SYNC_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            "Opening sync settings"
        } catch (e: Exception) {
            "Error accessing sync settings: ${e.message}"
        }
    }
    
    private suspend fun disableSync(): String = withContext(Dispatchers.IO) {
        try {
            val intent = Intent(Settings.ACTION_SYNC_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            "Opening sync settings to disable sync"
        } catch (e: Exception) {
            "Error accessing sync settings: ${e.message}"
        }
    }
    
    private suspend fun openDeveloperOptions(): String = withContext(Dispatchers.IO) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            "Opening developer options"
        } catch (e: Exception) {
            "Developer options not available or not enabled"
        }
    }
    
    private suspend fun getDeviceInfo(): String = withContext(Dispatchers.IO) {
        try {
            val deviceInfo = StringBuilder()
            deviceInfo.append("Device: ${Build.MANUFACTURER} ${Build.MODEL}\n")
            deviceInfo.append("Android Version: ${Build.VERSION.RELEASE}\n")
            deviceInfo.append("API Level: ${Build.VERSION.SDK_INT}\n")
            deviceInfo.append("Build: ${Build.DISPLAY}\n")
            
            deviceInfo.toString()
        } catch (e: Exception) {
            "Error getting device info: ${e.message}"
        }
    }
    
    private suspend fun getBatteryInfo(): String = withContext(Dispatchers.IO) {
        try {
            val batteryIntent = context.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1
            val batteryPct = if (level != -1 && scale != -1) {
                (level * 100 / scale.toFloat()).toInt()
            } else -1
            
            val status = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == android.os.BatteryManager.BATTERY_STATUS_CHARGING ||
                           status == android.os.BatteryManager.BATTERY_STATUS_FULL
            
            val chargingStatus = if (isCharging) "Charging" else "Not charging"
            
            "Battery: $batteryPct%, $chargingStatus"
        } catch (e: Exception) {
            "Error getting battery info: ${e.message}"
        }
    }
    
    private suspend fun getStorageInfo(): String = withContext(Dispatchers.IO) {
        try {
            val intent = Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            "Opening storage settings"
        } catch (e: Exception) {
            "Error accessing storage info: ${e.message}"
        }
    }
    
    private suspend fun getMemoryInfo(): String = withContext(Dispatchers.IO) {
        try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val memInfo = android.app.ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memInfo)
            
            val availMem = memInfo.availMem / (1024 * 1024) // Convert to MB
            val totalMem = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                memInfo.totalMem / (1024 * 1024)
            } else {
                -1L
            }
            
            if (totalMem > 0) {
                "Memory: ${availMem}MB available of ${totalMem}MB total"
            } else {
                "Available memory: ${availMem}MB"
            }
        } catch (e: Exception) {
            "Error getting memory info: ${e.message}"
        }
    }
    
    private suspend fun getNetworkInfo(): String = withContext(Dispatchers.IO) {
        try {
            val networkInfo = connectivityManager.activeNetworkInfo
            val isConnected = networkInfo?.isConnected == true
            val networkType = networkInfo?.typeName ?: "Unknown"
            
            val wifiEnabled = wifiManager.isWifiEnabled
            val mobileDataEnabled = telephonyManager.dataState == TelephonyManager.DATA_CONNECTED
            
            val info = StringBuilder()
            info.append("Network Status: ${if (isConnected) "Connected" else "Disconnected"}\n")
            info.append("Connection Type: $networkType\n")
            info.append("WiFi: ${if (wifiEnabled) "Enabled" else "Disabled"}\n")
            info.append("Mobile Data: ${if (mobileDataEnabled) "Connected" else "Disconnected"}")
            
            info.toString()
        } catch (e: Exception) {
            "Error getting network info: ${e.message}"
        }
    }
    
    private suspend fun clearSystemCache(): String = withContext(Dispatchers.IO) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            "Opening application settings to clear cache"
        } catch (e: Exception) {
            "Error accessing cache settings: ${e.message}"
        }
    }
    
    private suspend fun restartDevice(): String = withContext(Dispatchers.IO) {
        try {
            // This requires root access or device admin privileges
            val intent = Intent(Intent.ACTION_REBOOT)
            intent.putExtra("nowait", 1)
            intent.putExtra("interval", 1)
            intent.putExtra("window", 0)
            context.sendBroadcast(intent)
            "Attempting to restart device (requires special permissions)"
        } catch (e: Exception) {
            "Cannot restart device: ${e.message}. This requires root access or device admin privileges."
        }
    }
    
    private suspend fun enableEmergencyMode(): String = withContext(Dispatchers.IO) {
        try {
            // Enable emergency features
            val intent = Intent(Intent.ACTION_CALL)
            intent.data = android.net.Uri.parse("tel:911") // Emergency number
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            
            "Emergency mode activated. Ready to make emergency calls."
        } catch (e: Exception) {
            "Error enabling emergency mode: ${e.message}"
        }
    }
    
    suspend fun performQuickAction(action: String): String = withContext(Dispatchers.IO) {
        when (action.lowercase()) {
            "screenshot" -> takeScreenshot()
            "recent_apps" -> openRecentApps()
            "notifications" -> openNotifications()
            "quick_settings" -> openQuickSettings()
            "split_screen" -> enableSplitScreen()
            "picture_in_picture" -> enablePictureInPicture()
            else -> "Unknown quick action: $action"
        }
    }
    
    private suspend fun takeScreenshot(): String = withContext(Dispatchers.IO) {
        try {
            val accessibilityService = com.mobilebudhdhi.controller.services.MobileBuddhiAccessibilityService.getInstance()
            if (accessibilityService != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    accessibilityService.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)
                    "Screenshot taken"
                } else {
                    "Screenshot feature requires Android 11 or higher"
                }
            } else {
                "Accessibility service not running"
            }
        } catch (e: Exception) {
            "Error taking screenshot: ${e.message}"
        }
    }
    
    private suspend fun openRecentApps(): String = withContext(Dispatchers.IO) {
        try {
            val accessibilityService = com.mobilebudhdhi.controller.services.MobileBuddhiAccessibilityService.getInstance()
            if (accessibilityService != null) {
                accessibilityService.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_RECENTS)
                "Opening recent apps"
            } else {
                "Accessibility service not running"
            }
        } catch (e: Exception) {
            "Error opening recent apps: ${e.message}"
        }
    }
    
    private suspend fun openNotifications(): String = withContext(Dispatchers.IO) {
        try {
            val accessibilityService = com.mobilebudhdhi.controller.services.MobileBuddhiAccessibilityService.getInstance()
            if (accessibilityService != null) {
                accessibilityService.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
                "Opening notifications"
            } else {
                "Accessibility service not running"
            }
        } catch (e: Exception) {
            "Error opening notifications: ${e.message}"
        }
    }
    
    private suspend fun openQuickSettings(): String = withContext(Dispatchers.IO) {
        try {
            val accessibilityService = com.mobilebudhdhi.controller.services.MobileBuddhiAccessibilityService.getInstance()
            if (accessibilityService != null) {
                accessibilityService.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS)
                "Opening quick settings"
            } else {
                "Accessibility service not running"
            }
        } catch (e: Exception) {
            "Error opening quick settings: ${e.message}"
        }
    }
    
    private suspend fun enableSplitScreen(): String = withContext(Dispatchers.IO) {
        try {
            val accessibilityService = com.mobilebudhdhi.controller.services.MobileBuddhiAccessibilityService.getInstance()
            if (accessibilityService != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                accessibilityService.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN)
                "Enabling split screen mode"
            } else {
                "Split screen not supported or accessibility service not running"
            }
        } catch (e: Exception) {
            "Error enabling split screen: ${e.message}"
        }
    }
    
    private suspend fun enablePictureInPicture(): String = withContext(Dispatchers.IO) {
        try {
            // This would need to be implemented per app basis
            "Picture-in-picture mode depends on the current app's support"
        } catch (e: Exception) {
            "Error enabling picture-in-picture: ${e.message}"
        }
    }
}