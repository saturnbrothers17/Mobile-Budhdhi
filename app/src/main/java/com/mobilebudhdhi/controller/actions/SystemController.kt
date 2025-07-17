package com.mobilebudhdhi.controller.actions

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SystemController(private val context: Context) {
    
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    
    suspend fun volumeUp(): String = withContext(Dispatchers.IO) {
        try {
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_RAISE,
                AudioManager.FLAG_SHOW_UI
            )
            "Volume increased"
        } catch (e: Exception) {
            "Error increasing volume: ${e.message}"
        }
    }
    
    suspend fun volumeDown(): String = withContext(Dispatchers.IO) {
        try {
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_LOWER,
                AudioManager.FLAG_SHOW_UI
            )
            "Volume decreased"
        } catch (e: Exception) {
            "Error decreasing volume: ${e.message}"
        }
    }
    
    suspend fun setVolume(level: Int): String = withContext(Dispatchers.IO) {
        try {
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val targetVolume = (level * maxVolume / 100).coerceIn(0, maxVolume)
            
            audioManager.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                targetVolume,
                AudioManager.FLAG_SHOW_UI
            )
            "Volume set to $level%"
        } catch (e: Exception) {
            "Error setting volume: ${e.message}"
        }
    }
    
    suspend fun muteVolume(): String = withContext(Dispatchers.IO) {
        try {
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_MUTE,
                AudioManager.FLAG_SHOW_UI
            )
            "Volume muted"
        } catch (e: Exception) {
            "Error muting volume: ${e.message}"
        }
    }
    
    suspend fun unmuteVolume(): String = withContext(Dispatchers.IO) {
        try {
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_UNMUTE,
                AudioManager.FLAG_SHOW_UI
            )
            "Volume unmuted"
        } catch (e: Exception) {
            "Error unmuting volume: ${e.message}"
        }
    }
    
    suspend fun increaseBrightness(): String = withContext(Dispatchers.IO) {
        try {
            val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            "Opening display settings to increase brightness"
        } catch (e: Exception) {
            "Error accessing brightness settings: ${e.message}"
        }
    }
    
    suspend fun decreaseBrightness(): String = withContext(Dispatchers.IO) {
        try {
            val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            "Opening display settings to decrease brightness"
        } catch (e: Exception) {
            "Error accessing brightness settings: ${e.message}"
        }
    }
    
    suspend fun enableWifi(): String = withContext(Dispatchers.IO) {
        try {
            if (!wifiManager.isWifiEnabled) {
                // Note: Direct WiFi control is deprecated in newer Android versions
                val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                "Opening WiFi settings to enable WiFi"
            } else {
                "WiFi is already enabled"
            }
        } catch (e: Exception) {
            "Error enabling WiFi: ${e.message}"
        }
    }
    
    suspend fun disableWifi(): String = withContext(Dispatchers.IO) {
        try {
            if (wifiManager.isWifiEnabled) {
                val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                "Opening WiFi settings to disable WiFi"
            } else {
                "WiFi is already disabled"
            }
        } catch (e: Exception) {
            "Error disabling WiFi: ${e.message}"
        }
    }
    
    suspend fun enableBluetooth(): String = withContext(Dispatchers.IO) {
        try {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter != null) {
                if (!bluetoothAdapter.isEnabled) {
                    val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    "Requesting to enable Bluetooth"
                } else {
                    "Bluetooth is already enabled"
                }
            } else {
                "Bluetooth not supported on this device"
            }
        } catch (e: Exception) {
            "Error enabling Bluetooth: ${e.message}"
        }
    }
    
    suspend fun disableBluetooth(): String = withContext(Dispatchers.IO) {
        try {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter != null) {
                if (bluetoothAdapter.isEnabled) {
                    val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    "Opening Bluetooth settings to disable Bluetooth"
                } else {
                    "Bluetooth is already disabled"
                }
            } else {
                "Bluetooth not supported on this device"
            }
        } catch (e: Exception) {
            "Error disabling Bluetooth: ${e.message}"
        }
    }
    
    suspend fun openSettings(): String = withContext(Dispatchers.IO) {
        try {
            val intent = Intent(Settings.ACTION_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            "Opening system settings"
        } catch (e: Exception) {
            "Error opening settings: ${e.message}"
        }
    }
    
    suspend fun enableAirplaneMode(): String = withContext(Dispatchers.IO) {
        try {
            val intent = Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            "Opening airplane mode settings"
        } catch (e: Exception) {
            "Error accessing airplane mode: ${e.message}"
        }
    }
    
    suspend fun disableAirplaneMode(): String = withContext(Dispatchers.IO) {
        try {
            val intent = Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            "Opening airplane mode settings to disable airplane mode"
        } catch (e: Exception) {
            "Error accessing airplane mode: ${e.message}"
        }
    }
    
    suspend fun wakeScreen(): String = withContext(Dispatchers.IO) {
        try {
            // Use multiple approaches to ensure screen wakes up
            
            // 1. Use PowerManager to wake up the screen
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            val wakeLock = powerManager.newWakeLock(
                android.os.PowerManager.FULL_WAKE_LOCK or
                android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP or
                android.os.PowerManager.ON_AFTER_RELEASE,
                "MobileBudhdhi:WakeScreenLock"
            )
            wakeLock.acquire(10000) // 10 seconds
            
            // 2. Use accessibility service as backup
            val accessibilityService = com.mobilebudhdhi.controller.services.MobileBuddhiAccessibilityService.getInstance()
            if (accessibilityService != null) {
                accessibilityService.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
                accessibilityService.performBack()
            }
            
            // 3. Try to show a system dialog which will wake the screen
            val intent = Intent(android.provider.Settings.ACTION_VOICE_INPUT_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            context.startActivity(intent)
            
            // Release wake lock after a delay
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (wakeLock.isHeld) {
                    wakeLock.release()
                }
            }, 5000)
            
            "Screen woken up"
        } catch (e: Exception) {
            "Error waking screen: ${e.message}"
        }
    }
    
    suspend fun lockScreen(): String = withContext(Dispatchers.IO) {
        try {
            // Use accessibility service to lock screen
            val accessibilityService = com.mobilebudhdhi.controller.services.MobileBuddhiAccessibilityService.getInstance()
            if (accessibilityService != null) {
                accessibilityService.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
                "Screen locked"
            } else {
                "Accessibility service not running, cannot lock screen"
            }
        } catch (e: Exception) {
            "Error locking screen: ${e.message}"
        }
    }
    
    suspend fun openQuickSettings(): String = withContext(Dispatchers.IO) {
        try {
            val intent = Intent("android.settings.panel.action.INTERNET_CONNECTIVITY")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            "Opening quick settings panel"
        } catch (e: Exception) {
            "Error opening quick settings: ${e.message}"
        }
    }
}