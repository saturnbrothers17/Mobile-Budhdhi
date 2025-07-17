package com.mobilebudhdhi.controller.actions

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.location.LocationManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.telephony.SmsManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.*

/**
 * Enhanced Emergency Controller with advanced emergency features
 * including medical information, emergency alerts, and more sophisticated
 * emergency response capabilities.
 */
class EnhancedEmergencyController(private val context: Context) {
    
    private val emergencyNumbers = mapOf(
        "police" to "911",
        "fire" to "911", 
        "ambulance" to "911",
        "emergency" to "911",
        "poison" to "1-800-222-1222" // Poison control
    )
    
    private val emergencyContacts = mutableMapOf<String, EmergencyContact>()
    private val medicalInfo = MedicalInformation()
    private var panicModeActive = false
    private var emergencySirenPlayer: MediaPlayer? = null
    
    companion object {
        private const val TAG = "EmergencyController"
        private const val VIBRATION_PATTERN_DURATION = 1000L
        private const val EMERGENCY_PREFS = "MobileBudhdhi_Emergency"
    }
    
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
            lowerCommand.contains("call poison control") -> {
                callSpecificEmergencyService("poison")
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
            lowerCommand.contains("stop panic") || lowerCommand.contains("cancel emergency") -> {
                deactivatePanicMode()
            }
            lowerCommand.contains("medical info") || lowerCommand.contains("health info") -> {
                displayMedicalInfo()
            }
            lowerCommand.contains("emergency siren") || lowerCommand.contains("alarm") -> {
                activateEmergencySiren()
            }
            lowerCommand.contains("emergency contacts") -> {
                listEmergencyContacts()
            }
            else -> "Emergency command not recognized. Say 'call emergency', 'send emergency message', 'panic mode', or 'medical info'"
        }
    }
    
    private suspend fun callEmergencyServices(): String = withContext(Dispatchers.IO) {
        try {
            val intent = Intent(Intent.ACTION_CALL)
            intent.data = Uri.parse("tel:911")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            
            // Send medical info via SMS to emergency services if available
            if (medicalInfo.isConfigured()) {
                val smsManager = SmsManager.getDefault()
                val medicalSummary = medicalInfo.getEmergencySummary()
                try {
                    // This would ideally go to emergency services, but for demo we'll send to emergency contacts
                    emergencyContacts.values.forEach { contact ->
                        if (contact.sendMedicalInfo) {
                            smsManager.sendTextMessage(contact.phoneNumber, null, medicalSummary, null, null)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error sending medical info", e)
                }
            }
            
            "Calling emergency services now! Medical information will be shared if available."
        } catch (e: Exception) {
            Log.e(TAG, "Error calling emergency services", e)
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
            Log.e(TAG, "Error calling $service", e)
            "Error calling $service: ${e.message}"
        }
    }
    
    private suspend fun sendEmergencyMessage(): String = withContext(Dispatchers.IO) {
        try {
            val location = getCurrentLocation()
            val locationStr = if (location != null) {
                "My location: https://maps.google.com/?q=${location.latitude},${location.longitude}"
            } else {
                "Location unavailable"
            }
            
            val emergencyMessage = "EMERGENCY: I need help! This is an automated message from Mobile Budhdhi. $locationStr"
            val smsManager = SmsManager.getDefault()
            
            // Send to predefined emergency contacts
            if (emergencyContacts.isNotEmpty()) {
                var sentCount = 0
                emergencyContacts.values.forEach { contact ->
                    try {
                        smsManager.sendTextMessage(contact.phoneNumber, null, emergencyMessage, null, null)
                        sentCount++
                    } catch (e: Exception) {
                        Log.e(TAG, "Error sending emergency message to ${contact.name}", e)
                    }
                }
                "Emergency messages sent to $sentCount contacts with your current location"
            } else {
                "No emergency contacts configured. Please set up emergency contacts first."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending emergency messages", e)
            "Error sending emergency messages: ${e.message}"
        }
    }
    
    private suspend fun shareEmergencyLocation(): String = withContext(Dispatchers.IO) {
        try {
            val location = getCurrentLocation()
            
            if (location != null) {
                val lat = location.latitude
                val lng = location.longitude
                val locationMessage = "EMERGENCY LOCATION: I am at https://maps.google.com/?q=$lat,$lng - Please send help!"
                
                val smsManager = SmsManager.getDefault()
                
                if (emergencyContacts.isNotEmpty()) {
                    var sentCount = 0
                    emergencyContacts.values.forEach { contact ->
                        try {
                            smsManager.sendTextMessage(contact.phoneNumber, null, locationMessage, null, null)
                            sentCount++
                        } catch (e: Exception) {
                            Log.e(TAG, "Error sending location to ${contact.name}", e)
                        }
                    }
                    "Emergency location shared with $sentCount contacts"
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
            Log.e(TAG, "Error sharing location", e)
            "Error sharing location: ${e.message}"
        }
    }
    
    private suspend fun activatePanicMode(): String = withContext(Dispatchers.IO) {
        if (panicModeActive) {
            return@withContext "Panic mode is already active!"
        }
        
        try {
            panicModeActive = true
            val results = mutableListOf<String>()
            
            // 1. Send emergency messages with location
            try {
                val result = sendEmergencyMessage()
                results.add(result)
            } catch (e: Exception) {
                results.add("Emergency messages failed")
            }
            
            // 2. Turn on flashlight for visibility (strobe effect)
            try {
                startStrobeFlashlight()
                results.add("Flashlight strobe activated")
            } catch (e: Exception) {
                results.add("Flashlight activation failed")
            }
            
            // 3. Set volume to maximum
            try {
                val systemController = SystemController(context)
                systemController.setVolume(100)
                results.add("Volume maximized")
            } catch (e: Exception) {
                results.add("Volume adjustment failed")
            }
            
            // 4. Start emergency siren
            try {
                activateEmergencySiren()
                results.add("Emergency siren activated")
            } catch (e: Exception) {
                results.add("Siren activation failed")
            }
            
            // 5. Start vibration pattern
            try {
                startEmergencyVibration()
                results.add("Emergency vibration activated")
            } catch (e: Exception) {
                results.add("Vibration failed")
            }
            
            // 6. Display medical info on screen
            try {
                displayMedicalInfo()
                results.add("Medical info displayed")
            } catch (e: Exception) {
                results.add("Medical info display failed")
            }
            
            // Start a coroutine to maintain panic mode
            maintainPanicMode()
            
            "PANIC MODE ACTIVATED! ${results.joinToString(", ")}"
        } catch (e: Exception) {
            panicModeActive = false
            Log.e(TAG, "Error activating panic mode", e)
            "Error activating panic mode: ${e.message}"
        }
    }
    
    private suspend fun deactivatePanicMode(): String = withContext(Dispatchers.IO) {
        if (!panicModeActive) {
            return@withContext "Panic mode is not active."
        }
        
        try {
            panicModeActive = false
            
            // Stop flashlight
            stopStrobeFlashlight()
            
            // Stop siren
            stopEmergencySiren()
            
            // Stop vibration
            stopEmergencyVibration()
            
            "Panic mode deactivated."
        } catch (e: Exception) {
            Log.e(TAG, "Error deactivating panic mode", e)
            "Error deactivating panic mode: ${e.message}"
        }
    }
    
    private suspend fun maintainPanicMode() = withContext(Dispatchers.IO) {
        try {
            while (panicModeActive) {
                // Periodically send location updates
                if (emergencyContacts.isNotEmpty()) {
                    val location = getCurrentLocation()
                    if (location != null) {
                        val locationMessage = "EMERGENCY UPDATE: I am at https://maps.google.com/?q=${location.latitude},${location.longitude}"
                        val smsManager = SmsManager.getDefault()
                        
                        // Send to primary emergency contact only to avoid spamming
                        emergencyContacts.values.firstOrNull { it.isPrimary }?.let { contact ->
                            try {
                                smsManager.sendTextMessage(contact.phoneNumber, null, locationMessage, null, null)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error sending location update", e)
                            }
                        }
                    }
                }
                
                // Wait 5 minutes before next update
                delay(5 * 60 * 1000)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in panic mode maintenance", e)
        }
    }
    
    private fun startStrobeFlashlight() {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList[0]
            
            // Start a thread to blink the flashlight
            Thread {
                try {
                    var flashOn = false
                    while (panicModeActive) {
                        flashOn = !flashOn
                        cameraManager.setTorchMode(cameraId, flashOn)
                        Thread.sleep(500) // Blink every half second
                    }
                    // Ensure flashlight is off when done
                    cameraManager.setTorchMode(cameraId, false)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in flashlight strobe", e)
                }
            }.start()
        } catch (e: Exception) {
            Log.e(TAG, "Error starting flashlight", e)
        }
    }
    
    private fun stopStrobeFlashlight() {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList[0]
            cameraManager.setTorchMode(cameraId, false)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping flashlight", e)
        }
    }
    
    private fun activateEmergencySiren() {
        try {
            // Set volume to maximum
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                0
            )
            
            // Play emergency siren sound
            if (emergencySirenPlayer == null) {
                emergencySirenPlayer = MediaPlayer.create(context, android.R.raw.alarm_clock_beep)
                emergencySirenPlayer?.isLooping = true
            }
            
            emergencySirenPlayer?.start()
        } catch (e: Exception) {
            Log.e(TAG, "Error activating emergency siren", e)
        }
    }
    
    private fun stopEmergencySiren() {
        try {
            emergencySirenPlayer?.stop()
            emergencySirenPlayer?.release()
            emergencySirenPlayer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping emergency siren", e)
        }
    }
    
    private fun startEmergencyVibration() {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            
            // SOS pattern: ... --- ...
            val pattern = longArrayOf(
                0, 200, 200, 200, 200, 200, 200,     // ...
                500,                                  // pause
                0, 500, 200, 500, 200, 500, 200,     // ---
                500,                                  // pause
                0, 200, 200, 200, 200, 200, 200      // ...
            )
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting emergency vibration", e)
        }
    }
    
    private fun stopEmergencyVibration() {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping emergency vibration", e)
        }
    }
    
    private fun displayMedicalInfo(): String {
        return if (medicalInfo.isConfigured()) {
            val info = medicalInfo.getEmergencySummary()
            
            // Create an intent to display medical info
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setClassName("com.mobilebudhdhi.controller", "com.mobilebudhdhi.controller.EmergencyInfoActivity")
            intent.putExtra("medical_info", info)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            
            "Displaying medical information: $info"
        } else {
            "No medical information configured. Please set up your medical information."
        }
    }
    
    private suspend fun getCurrentLocation(): android.location.Location? = withContext(Dispatchers.IO) {
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
                    Log.e(TAG, "Location permission not granted", e)
                }
            }
            
            location
        } catch (e: Exception) {
            Log.e(TAG, "Error getting location", e)
            null
        }
    }
    
    fun addEmergencyContact(name: String, phoneNumber: String, isPrimary: Boolean = false, sendMedicalInfo: Boolean = true): String {
        return try {
            val contact = EmergencyContact(name, phoneNumber, isPrimary, sendMedicalInfo)
            emergencyContacts[name.lowercase()] = contact
            
            // If this is marked as primary, update other contacts
            if (isPrimary) {
                emergencyContacts.values.forEach { 
                    if (it.name != name) {
                        it.isPrimary = false
                    }
                }
            }
            
            saveEmergencyContacts()
            "Emergency contact $name added successfully"
        } catch (e: Exception) {
            Log.e(TAG, "Error adding emergency contact", e)
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
            Log.e(TAG, "Error removing emergency contact", e)
            "Error removing emergency contact: ${e.message}"
        }
    }
    
    fun listEmergencyContacts(): String {
        return if (emergencyContacts.isNotEmpty()) {
            val contactList = emergencyContacts.values.joinToString("\n") { 
                "${it.name} (${it.phoneNumber})${if (it.isPrimary) " - Primary" else ""}"
            }
            "Emergency contacts:\n$contactList"
        } else {
            "No emergency contacts configured"
        }
    }
    
    fun updateMedicalInfo(
        bloodType: String? = null,
        allergies: List<String>? = null,
        medications: List<String>? = null,
        conditions: List<String>? = null,
        doctorName: String? = null,
        doctorPhone: String? = null,
        notes: String? = null
    ): String {
        return try {
            bloodType?.let { medicalInfo.bloodType = it }
            allergies?.let { medicalInfo.allergies = it }
            medications?.let { medicalInfo.medications = it }
            conditions?.let { medicalInfo.conditions = it }
            doctorName?.let { medicalInfo.doctorName = it }
            doctorPhone?.let { medicalInfo.doctorPhone = it }
            notes?.let { medicalInfo.notes = it }
            
            saveMedicalInfo()
            "Medical information updated successfully"
        } catch (e: Exception) {
            Log.e(TAG, "Error updating medical info", e)
            "Error updating medical information: ${e.message}"
        }
    }
    
    fun getMedicalInfo(): MedicalInformation {
        return medicalInfo
    }
    
    private fun saveEmergencyContacts() {
        try {
            val prefs = context.getSharedPreferences(EMERGENCY_PREFS, Context.MODE_PRIVATE)
            val editor = prefs.edit()
            
            // Clear existing contacts first
            editor.clear()
            
            // Save contacts
            emergencyContacts.values.forEachIndexed { index, contact ->
                val prefix = "contact_$index"
                editor.putString("${prefix}_name", contact.name)
                editor.putString("${prefix}_number", contact.phoneNumber)
                editor.putBoolean("${prefix}_primary", contact.isPrimary)
                editor.putBoolean("${prefix}_medical", contact.sendMedicalInfo)
            }
            
            editor.apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving emergency contacts", e)
        }
    }
    
    private fun loadEmergencyContacts() {
        try {
            val prefs = context.getSharedPreferences(EMERGENCY_PREFS, Context.MODE_PRIVATE)
            var index = 0
            
            while (true) {
                val prefix = "contact_$index"
                val name = prefs.getString("${prefix}_name", null) ?: break
                val number = prefs.getString("${prefix}_number", null) ?: break
                val isPrimary = prefs.getBoolean("${prefix}_primary", false)
                val sendMedical = prefs.getBoolean("${prefix}_medical", true)
                
                emergencyContacts[name.lowercase()] = EmergencyContact(name, number, isPrimary, sendMedical)
                index++
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading emergency contacts", e)
        }
    }
    
    private fun saveMedicalInfo() {
        try {
            val prefs = context.getSharedPreferences(EMERGENCY_PREFS, Context.MODE_PRIVATE)
            val editor = prefs.edit()
            
            editor.putString("medical_blood_type", medicalInfo.bloodType)
            editor.putString("medical_allergies", medicalInfo.allergies.joinToString(","))
            editor.putString("medical_medications", medicalInfo.medications.joinToString(","))
            editor.putString("medical_conditions", medicalInfo.conditions.joinToString(","))
            editor.putString("medical_doctor_name", medicalInfo.doctorName)
            editor.putString("medical_doctor_phone", medicalInfo.doctorPhone)
            editor.putString("medical_notes", medicalInfo.notes)
            
            editor.apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving medical info", e)
        }
    }
    
    private fun loadMedicalInfo() {
        try {
            val prefs = context.getSharedPreferences(EMERGENCY_PREFS, Context.MODE_PRIVATE)
            
            medicalInfo.bloodType = prefs.getString("medical_blood_type", "") ?: ""
            
            prefs.getString("medical_allergies", "")?.let { 
                if (it.isNotEmpty()) {
                    medicalInfo.allergies = it.split(",")
                }
            }
            
            prefs.getString("medical_medications", "")?.let { 
                if (it.isNotEmpty()) {
                    medicalInfo.medications = it.split(",")
                }
            }
            
            prefs.getString("medical_conditions", "")?.let { 
                if (it.isNotEmpty()) {
                    medicalInfo.conditions = it.split(",")
                }
            }
            
            medicalInfo.doctorName = prefs.getString("medical_doctor_name", "") ?: ""
            medicalInfo.doctorPhone = prefs.getString("medical_doctor_phone", "") ?: ""
            medicalInfo.notes = prefs.getString("medical_notes", "") ?: ""
        } catch (e: Exception) {
            Log.e(TAG, "Error loading medical info", e)
        }
    }
    
    init {
        loadEmergencyContacts()
        loadMedicalInfo()
    }
    
    data class EmergencyContact(
        val name: String,
        val phoneNumber: String,
        var isPrimary: Boolean = false,
        var sendMedicalInfo: Boolean = true
    )
    
    data class MedicalInformation(
        var bloodType: String = "",
        var allergies: List<String> = emptyList(),
        var medications: List<String> = emptyList(),
        var conditions: List<String> = emptyList(),
        var doctorName: String = "",
        var doctorPhone: String = "",
        var notes: String = ""
    ) {
        fun isConfigured(): Boolean {
            return bloodType.isNotEmpty() || 
                   allergies.isNotEmpty() || 
                   medications.isNotEmpty() || 
                   conditions.isNotEmpty()
        }
        
        fun getEmergencySummary(): String {
            val summary = StringBuilder("MEDICAL INFO: ")
            
            if (bloodType.isNotEmpty()) {
                summary.append("Blood Type: $bloodType. ")
            }
            
            if (allergies.isNotEmpty()) {
                summary.append("Allergies: ${allergies.joinToString(", ")}. ")
            }
            
            if (medications.isNotEmpty()) {
                summary.append("Medications: ${medications.joinToString(", ")}. ")
            }
            
            if (conditions.isNotEmpty()) {
                summary.append("Conditions: ${conditions.joinToString(", ")}. ")
            }
            
            if (doctorName.isNotEmpty() && doctorPhone.isNotEmpty()) {
                summary.append("Doctor: $doctorName ($doctorPhone). ")
            }
            
            if (notes.isNotEmpty()) {
                summary.append("Notes: $notes")
            }
            
            return summary.toString()
        }
    }
}