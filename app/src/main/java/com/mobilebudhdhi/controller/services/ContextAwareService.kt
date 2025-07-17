package com.mobilebudhdhi.controller.services

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.os.BatteryManager
import android.os.IBinder
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import kotlinx.coroutines.*
import java.util.*

class ContextAwareService : Service() {
    
    private var serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var contextReceiver: BroadcastReceiver? = null
    private var phoneStateListener: PhoneStateListener? = null
    
    private var currentContext = DeviceContext()
    
    companion object {
        private var instance: ContextAwareService? = null
        
        fun getInstance(): ContextAwareService? = instance
        
        fun getCurrentContext(): DeviceContext? = instance?.currentContext
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        registerContextListeners()
        startContextMonitoring()
    }
    
    private fun registerContextListeners() {
        // Register for various system events
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_HEADSET_PLUG)
            addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
            addAction("android.net.conn.CONNECTIVITY_CHANGE")
            addAction("android.location.PROVIDERS_CHANGED")
        }
        
        contextReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                intent?.let { handleContextChange(it) }
            }
        }
        
        registerReceiver(contextReceiver, filter)
        
        // Register phone state listener
        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        phoneStateListener = object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                super.onCallStateChanged(state, phoneNumber)
                updateCallState(state, phoneNumber)
            }
        }
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
    }
    
    private fun handleContextChange(intent: Intent) {
        serviceScope.launch {
            when (intent.action) {
                Intent.ACTION_BATTERY_CHANGED -> updateBatteryContext(intent)
                Intent.ACTION_POWER_CONNECTED -> {
                    currentContext = currentContext.copy(isCharging = true)
                    notifyContextChange("power_connected")
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    currentContext = currentContext.copy(isCharging = false)
                    notifyContextChange("power_disconnected")
                }
                Intent.ACTION_SCREEN_ON -> {
                    currentContext = currentContext.copy(isScreenOn = true)
                    notifyContextChange("screen_on")
                }
                Intent.ACTION_SCREEN_OFF -> {
                    currentContext = currentContext.copy(isScreenOn = false)
                    notifyContextChange("screen_off")
                }
                Intent.ACTION_USER_PRESENT -> {
                    currentContext = currentContext.copy(isUnlocked = true)
                    notifyContextChange("device_unlocked")
                }
                Intent.ACTION_HEADSET_PLUG -> updateHeadsetContext(intent)
                Intent.ACTION_AIRPLANE_MODE_CHANGED -> updateAirplaneModeContext(intent)
                "android.net.conn.CONNECTIVITY_CHANGE" -> updateConnectivityContext()
                "android.location.PROVIDERS_CHANGED" -> updateLocationContext()
            }
        }
    }
    
    private fun updateBatteryContext(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val batteryPct = if (level != -1 && scale != -1) {
            (level * 100 / scale.toFloat()).toInt()
        } else -1
        
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
        
        val wasLowBattery = currentContext.batteryLevel < 20
        val isLowBattery = batteryPct < 20
        
        currentContext = currentContext.copy(
            batteryLevel = batteryPct,
            isCharging = isCharging
        )
        
        // Notify if battery became low
        if (!wasLowBattery && isLowBattery) {
            notifyContextChange("battery_low")
            announceToUser("Battery is low at $batteryPct percent")
        }
        
        // Notify if battery became critical
        if (batteryPct < 10 && batteryPct > 0) {
            notifyContextChange("battery_critical")
            announceToUser("Critical battery warning: $batteryPct percent remaining")
        }
    }
    
    private fun updateHeadsetContext(intent: Intent) {
        val state = intent.getIntExtra("state", -1)
        val isHeadsetConnected = state == 1
        
        currentContext = currentContext.copy(isHeadsetConnected = isHeadsetConnected)
        
        if (isHeadsetConnected) {
            notifyContextChange("headset_connected")
            announceToUser("Headset connected")
        } else {
            notifyContextChange("headset_disconnected")
            announceToUser("Headset disconnected")
        }
    }
    
    private fun updateAirplaneModeContext(intent: Intent) {
        val isAirplaneModeOn = intent.getBooleanExtra("state", false)
        currentContext = currentContext.copy(isAirplaneModeOn = isAirplaneModeOn)
        
        if (isAirplaneModeOn) {
            notifyContextChange("airplane_mode_on")
            announceToUser("Airplane mode enabled")
        } else {
            notifyContextChange("airplane_mode_off")
            announceToUser("Airplane mode disabled")
        }
    }
    
    private fun updateConnectivityContext() {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        val isConnected = networkInfo?.isConnected == true
        val networkType = networkInfo?.typeName ?: "None"
        
        val wasConnected = currentContext.isNetworkConnected
        currentContext = currentContext.copy(
            isNetworkConnected = isConnected,
            networkType = networkType
        )
        
        if (!wasConnected && isConnected) {
            notifyContextChange("network_connected")
            announceToUser("Network connected via $networkType")
        } else if (wasConnected && !isConnected) {
            notifyContextChange("network_disconnected")
            announceToUser("Network disconnected")
        }
    }
    
    private fun updateLocationContext() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkLocationEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        
        currentContext = currentContext.copy(
            isLocationEnabled = isGpsEnabled || isNetworkLocationEnabled
        )
        
        notifyContextChange("location_settings_changed")
    }
    
    private fun updateCallState(state: Int, phoneNumber: String?) {
        val callState = when (state) {
            TelephonyManager.CALL_STATE_IDLE -> CallState.IDLE
            TelephonyManager.CALL_STATE_RINGING -> CallState.RINGING
            TelephonyManager.CALL_STATE_OFFHOOK -> CallState.IN_CALL
            else -> CallState.IDLE
        }
        
        val wasInCall = currentContext.callState == CallState.IN_CALL
        currentContext = currentContext.copy(
            callState = callState,
            incomingNumber = phoneNumber
        )
        
        when (callState) {
            CallState.RINGING -> {
                notifyContextChange("incoming_call")
                announceToUser("Incoming call from ${phoneNumber ?: "unknown number"}")
            }
            CallState.IN_CALL -> {
                if (!wasInCall) {
                    notifyContextChange("call_started")
                    announceToUser("Call started")
                }
            }
            CallState.IDLE -> {
                if (wasInCall) {
                    notifyContextChange("call_ended")
                    announceToUser("Call ended")
                }
            }
        }
    }
    
    private fun startContextMonitoring() {
        serviceScope.launch {
            while (isActive) {
                updateTimeContext()
                updateUsageContext()
                delay(60000) // Update every minute
            }
        }
    }
    
    private fun updateTimeContext() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        
        val timeOfDay = when (hour) {
            in 5..11 -> TimeOfDay.MORNING
            in 12..17 -> TimeOfDay.AFTERNOON
            in 18..21 -> TimeOfDay.EVENING
            else -> TimeOfDay.NIGHT
        }
        
        val isWeekend = dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY
        
        currentContext = currentContext.copy(
            timeOfDay = timeOfDay,
            isWeekend = isWeekend
        )
    }
    
    private fun updateUsageContext() {
        // Update app usage patterns, location patterns, etc.
        // This could be expanded to learn user behavior
    }
    
    private fun notifyContextChange(event: String) {
        val intent = Intent("com.mobilebudhdhi.CONTEXT_CHANGE")
        intent.putExtra("event", event)
        intent.putExtra("context", currentContext.toString())
        sendBroadcast(intent)
    }
    
    private fun announceToUser(message: String) {
        val intent = Intent("com.mobilebudhdhi.ANNOUNCE_NOTIFICATION")
        intent.putExtra("message", message)
        sendBroadcast(intent)
    }
    
    fun getContextualResponse(command: String): String {
        return when {
            currentContext.batteryLevel < 20 && command.contains("battery") -> 
                "Your battery is low at ${currentContext.batteryLevel}%. Consider charging your device."
            
            currentContext.callState == CallState.RINGING && command.contains("call") ->
                "You have an incoming call from ${currentContext.incomingNumber ?: "unknown number"}. Would you like me to answer or decline it?"
            
            !currentContext.isNetworkConnected && (command.contains("internet") || command.contains("online")) ->
                "You're not connected to the internet. Please check your network connection."
            
            currentContext.timeOfDay == TimeOfDay.NIGHT && command.contains("volume") ->
                "It's nighttime. Would you like me to keep the volume low?"
            
            currentContext.isHeadsetConnected && command.contains("music") ->
                "I notice you have headphones connected. Perfect for listening to music!"
            
            else -> ""
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        contextReceiver?.let { unregisterReceiver(it) }
        phoneStateListener?.let {
            val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE)
        }
        serviceScope.cancel()
        instance = null
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
}

data class DeviceContext(
    val batteryLevel: Int = 100,
    val isCharging: Boolean = false,
    val isScreenOn: Boolean = true,
    val isUnlocked: Boolean = true,
    val isHeadsetConnected: Boolean = false,
    val isAirplaneModeOn: Boolean = false,
    val isNetworkConnected: Boolean = true,
    val networkType: String = "WiFi",
    val isLocationEnabled: Boolean = true,
    val callState: CallState = CallState.IDLE,
    val incomingNumber: String? = null,
    val timeOfDay: TimeOfDay = TimeOfDay.MORNING,
    val isWeekend: Boolean = false,
    val currentApp: String = "",
    val lastUsedApps: List<String> = emptyList()
)

enum class CallState { IDLE, RINGING, IN_CALL }
enum class TimeOfDay { MORNING, AFTERNOON, EVENING, NIGHT }