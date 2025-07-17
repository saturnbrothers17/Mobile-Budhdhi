package com.mobilebudhdhi.controller.actions

import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.Uri
import android.telephony.SmsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EmergencyController(private val context: Context) {
    
    private val emergencyNumbers = mapOf(
        "police" to "911",
        "fire" to "911", 
        "ambulance" to "911",
        "emergency" to "911"
    )
    
    private val emergencyContacts = mutableMapOf<String, String>() // User-defined emergency contacts
    
    suspend fun handleEmergencyCommand(command: String): String = withContext(Dispatchers.IO) {
        val lowerCommand = command.lowercase()
        
        when {
            lowerCommand.contains("call emergency") || lowerCommand.contains("emergency call") -> {
                callEmergencyServices()
            }
            lowerCommand.contains("call police") -> {
                callSpecificEmergencyService("police")
            }
            lowerCommand.contains("call fire") || lowerCommand.contains("call fire department") -> {
                callSpecificEmergencyService("fire")
            }
            lowerCommand.contains("call ambulance") -> {
                callSpecificEmergencyService("ambulance")
            }
            lowerCommand.contains("send emergency message") || lowerCommand.contains("emergency text") -> {
                sendEmergencyMessage()
            }
            lowerCommand.contains("emergency location") || lowerCommand.contains("share location") -> {
                shareEmergencyLocation()
            }
            lowerCommand.contains("panic mode") || lowerCommand.contains("emergency mode") -> {
                activatePanicMode()
            }
            else -> "Emergency command not recognized. Say 'call emergency', 'send emergency message', or 'panic mode'"
        }
    }
    
    private suspend fun callEmergencyServices(): String = withContext(Dispatchers.IO) {
        try {
            val intent = Intent(Intent.ACTION_CALL)
            intent.data = Uri.parse("tel:911")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            
            "Calling emergency services now!"
        } catch (e: Exception) {
            "Error calling emergency services: ${e.message}"
        }
    }
    
    private suspend fun callSpecificEmergencyService(service: String): String = withContext(Dispatchers.IO) {
        try {
            val number = emergencyNumbers[service] ?: "911"
            val intent = Intent(Intent.ACTION_CALL)
            intent.data = Uri.parse("tel:$number")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            
            "Calling $service emergency services now!"
        } catch (e: Exception) {
            "Error calling $service: ${e.message}"
        }
    }
    
    private suspend fun sendEmergencyMessage(): String = withContext(Dispatchers.IO) {
        try {
            val emergencyMessage = "EMERGENCY: I need help! This is an automated message from Mobile Budhdhi. Please check on me."
            val smsManager = SmsManager.getDefault()
            
            // Send to predefined emergency contacts
            if (emergencyContacts.isNotEmpty()) {
                emergencyContacts.forEach { (name, number) ->
                    try {
                        smsManager.sendTextMessage(number, null, emergencyMessage, null, null)
                    } catch (e: Exception) {
                        // Continue with other contacts even if one fails
                    }
                }
                "Emergency messages sent to ${emergencyContacts.size} contacts"
            } else {
                "No emergency contacts configured. Please set up emergency contacts first."
            }
        } catch (e: Exception) {
            "Error sending emergency messages: ${e.message}"
        }
    }
    
    private suspend fun shareEmergencyLocation(): String = withContext(Dispatchers.IO) {
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            
            // Try to get last known location
            val providers = locationManager.getProviders(true)
            var location: android.location.Location? = null
            
            for (provider in providers) {
                try {
                    val lastKnown = locationManager.getLastKnownLocation(provider)
                    if (lastKnown != null && (location == null || lastKnown.accuracy < location.accuracy)) {
                        location = lastKnown
                    }
                } catch (e: SecurityException) {
                    // Permission not granted
                }
            }
            
            if (location != null) {
                val lat = location.latitude
                val lng = location.longitude
                val locationMessage = "EMERGENCY LOCATION: I am at https://maps.google.com/?q=$lat,$lng - Please send help!"
                
                val smsManager = SmsManager.getDefault()
                
                if (emergencyContacts.isNotEmpty()) {
                    emergencyContacts.forEach { (name, number) ->
                        try {
                            smsManager.sendTextMessage(number, null, locationMessage, null, null)
                        } catch (e: Exception) {
                            // Continue with other contacts
                        }
                    }
                    "Emergency location shared with ${emergencyContacts.size} contacts"
                } else {
                    // Open maps to show current location
                    val mapsIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lng"))
                    mapsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(mapsIntent)
                    "Opening your location in maps"
                }
            } else {
                "Unable to determine current location. Please ensure location services are enabled."
            }
        } catch (e: Exception) {
            "Error sharing location: ${e.message}"
        }
    }
    
    private suspend fun activatePanicMode(): String = withContext(Dispatchers.IO) {
        try {
            // Activate multiple emergency features at once
            val results = mutableListOf<String>()
            
            // 1. Call emergency services
            try {
                callEmergencyServices()
                results.add("Emergency call initiated")
            } catch (e: Exception) {
                results.add("Emergency call failed")
            }
            
            // 2. Send emergency messages
            try {
                sendEmergencyMessage()
                results.add("Emergency messages sent")
            } catch (e: Exception) {
                results.add("Emergency messages failed")
            }
            
            // 3. Share location
            try {
                shareEmergencyLocation()
                results.add("Location shared")
            } catch (e: Exception) {
                results.add("Location sharing failed")
            }
            
            // 4. Turn on flashlight for visibility
            try {
                val advancedSystemController = AdvancedSystemController(context)
                advancedSystemController.performAdvancedSystemAction("flashlight_on")
                results.add("Flashlight activated")
            } catch (e: Exception) {
                results.add("Flashlight activation failed")
            }
            
            // 5. Set volume to maximum
            try {
                val systemController = SystemController(context)
                systemController.setVolume(100)
                results.add("Volume maximized")
            } catch (e: Exception) {
                results.add("Volume adjustment failed")
            }
            
            "PANIC MODE ACTIVATED! ${results.joinToString(", ")}"
        } catch (e: Exception) {
            "Error activating panic mode: ${e.message}"
        }
    }
    
    fun addEmergencyContact(name: String, phoneNumber: String): String {
        return try {
            emergencyContacts[name.lowercase()] = phoneNumber
            saveEmergencyContacts()
            "Emergency contact $name added successfully"
        } catch (e: Exception) {
            "Error adding emergency contact: ${e.message}"
        }
    }
    
    fun removeEmergencyContact(name: String): String {
        return try {
            if (emergencyContacts.remove(name.lowercase()) != null) {
                saveEmergencyContacts()
                "Emergency contact $name removed successfully"
            } else {
                "Emergency contact $name not found"
            }
        } catch (e: Exception) {
            "Error removing emergency contact: ${e.message}"
        }
    }
    
    fun listEmergencyContacts(): String {
        return if (emergencyContacts.isNotEmpty()) {
            "Emergency contacts: ${emergencyContacts.keys.joinToString(", ")}"
        } else {
            "No emergency contacts configured"
        }
    }
    
    private fun saveEmergencyContacts() {
        try {
            val prefs = context.getSharedPreferences("MobileBudhdhi_Emergency", Context.MODE_PRIVATE)
            val editor = prefs.edit()
            
            emergencyContacts.forEach { (name, number) ->
                editor.putString("contact_$name", number)
            }
            
            editor.apply()
        } catch (e: Exception) {
            // Handle error silently
        }
    }
    
    private fun loadEmergencyContacts() {
        try {
            val prefs = context.getSharedPreferences("MobileBudhdhi_Emergency", Context.MODE_PRIVATE)
            val allPrefs = prefs.all
            
            allPrefs.forEach { (key, value) ->
                if (key.startsWith("contact_") && value is String) {
                    val contactName = key.removePrefix("contact_")
                    emergencyContacts[contactName] = value
                }
            }
        } catch (e: Exception) {
            // Handle error silently
        }
    }
    
    init {
        loadEmergencyContacts()
    }
}