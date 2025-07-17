package com.mobilebudhdhi.controller.nlp

import android.content.Context
import android.util.Log
import com.mobilebudhdhi.controller.actions.*
import com.mobilebudhdhi.controller.services.ContextAwareService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class CommandProcessor(private val context: Context) {
    
    private val appController = AppController(context)
    private val messageController = MessageController(context)
    private val phoneController = PhoneController(context)
    private val emailController = EmailController(context)
    private val musicController = MusicController(context)
    private val systemController = SystemController(context)
    private val advancedSystemController = AdvancedSystemController(context)
    private val advancedNLPProcessor = AdvancedNLPProcessor(context)
    
    private var isActivated = false
    private var lastActivationTime = 0L
    private val activationTimeout = 30000L // 30 seconds
    
    fun isActivated(): Boolean {
        return isActivated && (System.currentTimeMillis() - lastActivationTime) < activationTimeout
    }
    
    suspend fun processCommand(command: VoiceCommand): String = withContext(Dispatchers.IO) {
        val text = command.text.lowercase().trim()
        
        // Use advanced NLP processing for better understanding
        val enhancedIntent = advancedNLPProcessor.processAdvancedCommand(command)
        
        // Check for wake word with more natural responses
        if (command.containsWakeWord()) {
            isActivated = true
            lastActivationTime = System.currentTimeMillis()
            
            // If it's just the wake word, provide a contextual greeting
            if (text.split(" ").size <= 2) {
                // Get contextual response based on device state
                val contextService = ContextAwareService.getInstance()
                val deviceContext = ContextAwareService.getCurrentContext()
                
                val contextualGreeting = when {
                    deviceContext?.batteryLevel ?: 100 < 20 -> "Hello! I notice your battery is low. How can I help you quickly?"
                    deviceContext?.callState == CallState.RINGING -> "Hello! You have an incoming call. Would you like me to answer or decline it?"
                    deviceContext?.timeOfDay == TimeOfDay.NIGHT -> "Good evening! How can I help you tonight?"
                    deviceContext?.timeOfDay == TimeOfDay.MORNING -> "Good morning! How can I help you start your day?"
                    deviceContext?.timeOfDay == TimeOfDay.AFTERNOON -> "Good afternoon! I'm here to assist you."
                    else -> {
                        val calendar = Calendar.getInstance()
                        val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)
                        when {
                            hourOfDay < 5 -> "You're up late! How can I help you?"
                            hourOfDay < 12 -> "Good morning! How can I help you today?"
                            hourOfDay < 17 -> "Good afternoon! I'm here to assist you."
                            hourOfDay < 22 -> "Good evening! What can I do for you?"
                            else -> "It's getting late! How can I help you tonight?"
                        }
                    }
                }
                
                return@withContext contextualGreeting
            }
        }
        
        // If not activated, ignore command
        if (!isActivated()) {
            return@withContext ""
        }
        
        // Update activation time
        lastActivationTime = System.currentTimeMillis()
        
        // Log the command for debugging
        Log.d("MobileBudhdhi", "Processing command: $text")
        Log.d("MobileBudhdhi", "Enhanced intent: ${enhancedIntent.intent}, confidence: ${enhancedIntent.confidence}")
        
        // Check for contextual responses first
        val contextService = ContextAwareService.getInstance()
        val contextualResponse = contextService?.getContextualResponse(text)
        if (!contextualResponse.isNullOrEmpty()) {
            return@withContext contextualResponse
        }
        
        // Check for common phrases and greetings first
        if (text.matches(Regex(".*(how are you|how're you|how you doing).*"))) {
            return@withContext "I'm doing well, thank you for asking. How can I help you today?"
        }
        
        if (text.matches(Regex(".*(what can you do|what are you capable of|what are your abilities).*"))) {
            return@withContext "I can help you control your phone with voice commands. " +
                   "I can open apps, send messages, make calls, read notifications, " +
                   "control music, adjust system settings, and more. Just tell me what you need!"
        }
        
        if (text.matches(Regex(".*(thank you|thanks).*"))) {
            return@withContext "You're welcome! Is there anything else you need?"
        }
        
        // Extract intent from command
        val intent = command.extractIntent()
        
        // Process based on command type
        return@withContext when (intent.type) {
            CommandType.EMERGENCY -> {
                val emergencyController = EmergencyController(context)
                emergencyController.handleEmergencyCommand(text)
            }
            
            CommandType.HELP -> {
                "I can help you with many tasks like opening apps, sending messages, making calls, " +
                "reading notifications, controlling music, adjusting system settings, taking photos, " +
                "emergency assistance, and much more. Just tell me what you need!"
            }
            
            CommandType.APP_CONTROL -> {
                when (intent.action) {
                    "open" -> appController.openApp(intent.target)
                    "close" -> appController.closeApp(intent.target)
                    else -> "Please specify which app you want to open or close."
                }
            }
            
            CommandType.PHONE_CALL -> {
                when (intent.action) {
                    "call" -> phoneController.makeCall(intent.target)
                    "end" -> phoneController.endCall()
                    "answer" -> phoneController.answerCall()
                    "reject" -> phoneController.rejectCall()
                    else -> "Would you like to make, answer, reject, or end a call?"
                }
            }
            
            CommandType.MESSAGING -> {
                when (intent.action) {
                    "read" -> messageController.readMessages()
                    "send" -> messageController.sendSMS(intent.target, intent.content)
                    else -> "Would you like to read or send a message?"
                }
            }
            
            CommandType.WHATSAPP -> {
                when (intent.action) {
                    "read" -> messageController.readWhatsAppMessages()
                    "send" -> messageController.sendWhatsAppMessage(intent.target, intent.content)
                    else -> {
                        if (text.contains("open")) {
                            appController.openApp("WhatsApp")
                        } else {
                            "Would you like to read or send a WhatsApp message?"
                        }
                    }
                }
            }
            
            CommandType.EMAIL -> {
                when (intent.action) {
                    "read" -> emailController.readEmails()
                    "send" -> {
                        val subject = intent.parameters["subject"] ?: "Voice Message"
                        emailController.sendEmail(intent.target, subject, intent.content)
                    }
                    else -> "Would you like to read or send an email?"
                }
            }
            
            CommandType.MUSIC -> {
                when (intent.action) {
                    "play" -> musicController.playMusic()
                    "pause" -> musicController.pauseMusic()
                    "next" -> musicController.nextTrack()
                    "previous" -> musicController.previousTrack()
                    else -> {
                        if (intent.parameters.containsKey("volume")) {
                            if (intent.parameters["volume"] == "up") {
                                musicController.volumeUp()
                            } else {
                                musicController.volumeDown()
                            }
                        } else {
                            "What would you like to do with your music?"
                        }
                    }
                }
            }
            
            CommandType.CAMERA -> {
                when {
                    text.contains("photo") || text.contains("picture") || text.contains("selfie") -> {
                        appController.openApp("Camera")
                        "Opening camera to take a photo"
                    }
                    text.contains("video") -> {
                        appController.openApp("Camera")
                        "Opening camera to record video"
                    }
                    text.contains("flashlight") || text.contains("torch") -> {
                        if (text.contains("on")) {
                            advancedSystemController.performAdvancedSystemAction("flashlight_on")
                        } else {
                            advancedSystemController.performAdvancedSystemAction("flashlight_off")
                        }
                    }
                    else -> "Would you like to take a photo, record video, or control the flashlight?"
                }
            }
            
            CommandType.SYSTEM_CONTROL -> {
                when {
                    intent.parameters.containsKey("volume") -> {
                        if (intent.parameters["volume"] == "up") {
                            systemController.volumeUp()
                        } else {
                            systemController.volumeDown()
                        }
                    }
                    intent.parameters.containsKey("brightness") -> {
                        if (intent.parameters["brightness"] == "up") {
                            systemController.increaseBrightness()
                        } else {
                            systemController.decreaseBrightness()
                        }
                    }
                    text.contains("screen on") || text.contains("wake up") -> {
                        systemController.wakeScreen()
                        "Waking up your device."
                    }
                    text.contains("screen off") || text.contains("lock") -> {
                        systemController.lockScreen()
                        "Locking your device."
                    }
                    text.contains("do not disturb") -> {
                        if (text.contains("on")) {
                            advancedSystemController.performAdvancedSystemAction("do_not_disturb_on")
                        } else {
                            advancedSystemController.performAdvancedSystemAction("do_not_disturb_off")
                        }
                    }
                    text.contains("auto rotate") -> {
                        if (text.contains("on")) {
                            advancedSystemController.performAdvancedSystemAction("auto_rotate_on")
                        } else {
                            advancedSystemController.performAdvancedSystemAction("auto_rotate_off")
                        }
                    }
                    text.contains("power save") -> {
                        if (text.contains("on")) {
                            advancedSystemController.performAdvancedSystemAction("power_save_on")
                        } else {
                            advancedSystemController.performAdvancedSystemAction("power_save_off")
                        }
                    }
                    else -> "What system setting would you like to adjust?"
                }
            }
            
            CommandType.DEVICE_ADMIN -> {
                when {
                    text.contains("restart") || text.contains("reboot") -> {
                        advancedSystemController.performAdvancedSystemAction("restart_device")
                    }
                    text.contains("device info") -> {
                        advancedSystemController.performAdvancedSystemAction("device_info")
                    }
                    text.contains("battery info") -> {
                        advancedSystemController.performAdvancedSystemAction("battery_info")
                    }
                    text.contains("memory info") -> {
                        advancedSystemController.performAdvancedSystemAction("memory_info")
                    }
                    text.contains("storage info") -> {
                        advancedSystemController.performAdvancedSystemAction("storage_info")
                    }
                    text.contains("network info") -> {
                        advancedSystemController.performAdvancedSystemAction("network_info")
                    }
                    text.contains("clear cache") -> {
                        advancedSystemController.performAdvancedSystemAction("clear_cache")
                    }
                    else -> "What device administration task would you like to perform?"
                }
            }
            
            CommandType.CONNECTIVITY -> {
                when {
                    text.contains("wifi on") -> systemController.enableWifi()
                    text.contains("wifi off") -> systemController.disableWifi()
                    text.contains("bluetooth on") -> systemController.enableBluetooth()
                    text.contains("bluetooth off") -> systemController.disableBluetooth()
                    text.contains("airplane mode on") -> systemController.enableAirplaneMode()
                    text.contains("airplane mode off") -> systemController.disableAirplaneMode()
                    text.contains("mobile data on") -> advancedSystemController.performAdvancedSystemAction("mobile_data_on")
                    text.contains("mobile data off") -> advancedSystemController.performAdvancedSystemAction("mobile_data_off")
                    text.contains("hotspot on") -> advancedSystemController.performAdvancedSystemAction("hotspot_on")
                    text.contains("hotspot off") -> advancedSystemController.performAdvancedSystemAction("hotspot_off")
                    else -> "Would you like to change WiFi, Bluetooth, Mobile Data, Hotspot, or Airplane mode settings?"
                }
            }
            
            CommandType.ACCESSIBILITY -> {
                val accessibilityService = com.mobilebudhdhi.controller.services.MobileBuddhiAccessibilityService.getInstance()
                if (accessibilityService != null) {
                    when {
                        text.contains("read screen") || text.contains("what's on screen") -> {
                            val screenContent = accessibilityService.getAllTextFromScreen()
                            if (screenContent.isNotEmpty()) {
                                "Here's what's on your screen: ${screenContent.take(200)}..."
                            } else {
                                "I couldn't read any text from the current screen."
                            }
                        }
                        text.contains("describe screen") -> {
                            val structuredContent = accessibilityService.getStructuredScreenContent()
                            val description = StringBuilder()
                            structuredContent.forEach { (key, value) ->
                                if (value.isNotEmpty()) {
                                    description.append("$key: $value. ")
                                }
                            }
                            if (description.isNotEmpty()) {
                                description.toString()
                            } else {
                                "I couldn't analyze the current screen content."
                            }
                        }
                        else -> "I can read the screen or describe what's currently displayed. What would you like me to do?"
                    }
                } else {
                    "Accessibility service is not running. Please enable it in settings."
                }
            }
            
            CommandType.LOCATION -> {
                when {
                    text.contains("where am i") || text.contains("my location") -> {
                        advancedSystemController.performAdvancedSystemAction("location_on")
                        "Opening location settings to show your current location"
                    }
                    text.contains("find nearest") || text.contains("find closest") -> {
                        val query = text.substringAfter("find").substringAfter("nearest").substringAfter("closest").trim()
                        if (query.isNotEmpty()) {
                            appController.openApp("Maps")
                            "Opening Maps to find nearest $query"
                        } else {
                            "What would you like me to find nearby?"
                        }
                    }
                    else -> "I can help you find your location or search for nearby places. What do you need?"
                }
            }
            
            CommandType.MULTI_ACTION -> {
                // Handle compound commands
                val commands = text.split(" and then ", " after that ")
                if (commands.size >= 2) {
                    val results = mutableListOf<String>()
                    commands.forEach { cmd ->
                        val subCommand = VoiceCommand(cmd.trim(), System.currentTimeMillis())
                        val result = processCommand(subCommand)
                        results.add(result)
                    }
                    "Executed multiple actions: ${results.joinToString("; ")}"
                } else {
                    "I detected a multi-action command but couldn't parse it properly. Please try again."
                }
            }
            
            CommandType.TIME_DATE -> {
                val calendar = Calendar.getInstance()
                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                val minute = calendar.get(Calendar.MINUTE)
                val day = calendar.get(Calendar.DAY_OF_MONTH)
                val month = calendar.get(Calendar.MONTH) + 1
                val year = calendar.get(Calendar.YEAR)
                val dayOfWeek = calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault())
                
                when {
                    text.contains("time") -> "The current time is $hour:${minute.toString().padStart(2, '0')}"
                    text.contains("date") -> "Today's date is $dayOfWeek, $day/$month/$year"
                    text.contains("day") -> "Today is $dayOfWeek"
                    else -> "Current time: $hour:${minute.toString().padStart(2, '0')}, Date: $dayOfWeek, $day/$month/$year"
                }
            }
            
            CommandType.WEATHER -> {
                "I don't have direct access to weather information, but I can open a weather app for you."
                appController.openApp("Weather")
            }
            
            CommandType.SEARCH -> {
                val query = when {
                    text.contains("search for") -> text.substringAfter("search for").trim()
                    text.contains("google") -> text.substringAfter("google").trim()
                    text.contains("find") -> text.substringAfter("find").trim()
                    else -> text.substringAfter("search").trim()
                }
                if (query.isNotEmpty()) {
                    appController.searchWeb(query)
                } else {
                    "What would you like me to search for?"
                }
            }
            
            CommandType.NAVIGATION -> {
                val destination = when {
                    text.contains("navigate to") -> text.substringAfter("navigate to").trim()
                    text.contains("directions to") -> text.substringAfter("directions to").trim()
                    text.contains("take me to") -> text.substringAfter("take me to").trim()
                    else -> text.substringAfter("to").trim()
                }
                if (destination.isNotEmpty()) {
                    appController.openApp("Maps")
                    "Opening Maps for navigation to $destination"
                } else {
                    "Where would you like me to navigate to?"
                }
            }
            
            CommandType.REMINDER -> {
                when {
                    text.contains("alarm") -> {
                        appController.openApp("Clock")
                        "Opening Clock app to set an alarm"
                    }
                    text.contains("reminder") -> {
                        appController.openApp("Calendar")
                        "Opening Calendar to set a reminder"
                    }
                    text.contains("timer") -> {
                        appController.openApp("Clock")
                        "Opening Clock app to set a timer"
                    }
                    else -> "Would you like to set an alarm, reminder, or timer?"
                }
            }
            
            CommandType.CALENDAR -> {
                when {
                    text.contains("schedule") || text.contains("appointment") || text.contains("meeting") -> {
                        appController.openApp("Calendar")
                        "Opening Calendar to manage your schedule"
                    }
                    text.contains("what's my schedule") -> {
                        appController.openApp("Calendar")
                        "Opening Calendar to show your schedule"
                    }
                    else -> "Opening Calendar app for you"
                }
            }
            
            CommandType.CONTACTS -> {
                when {
                    text.contains("add contact") || text.contains("save contact") -> {
                        appController.openApp("Contacts")
                        "Opening Contacts to add a new contact"
                    }
                    text.contains("find contact") || text.contains("search contact") -> {
                        appController.openApp("Contacts")
                        "Opening Contacts to search for a contact"
                    }
                    else -> "Opening Contacts app for you"
                }
            }
            
            CommandType.READ_CONTENT -> {
                when {
                    text.contains("message") -> messageController.readMessages()
                    text.contains("email") -> emailController.readEmails()
                    text.contains("notification") -> {
                        advancedSystemController.performQuickAction("notifications")
                    }
                    text.contains("screen") -> {
                        val accessibilityService = com.mobilebudhdhi.controller.services.MobileBuddhiAccessibilityService.getInstance()
                        if (accessibilityService != null) {
                            val screenContent = accessibilityService.getAllTextFromScreen()
                            if (screenContent.isNotEmpty()) {
                                "Here's what's on your screen: ${screenContent.take(300)}..."
                            } else {
                                "I couldn't read any text from the current screen."
                            }
                        } else {
                            "Accessibility service is not running."
                        }
                    }
                    else -> "What would you like me to read - messages, emails, notifications, or screen content?"
                }
            }
            
            CommandType.UNKNOWN -> {
                when {
                    text.contains("stop listening") || text.contains("goodbye") || text.contains("bye") -> {
                        isActivated = false
                        "Goodbye! Say 'Budhdhi' to activate me again."
                    }
                    text.contains("screenshot") -> {
                        advancedSystemController.performQuickAction("screenshot")
                    }
                    text.contains("recent apps") -> {
                        advancedSystemController.performQuickAction("recent_apps")
                    }
                    text.contains("quick settings") -> {
                        advancedSystemController.performQuickAction("quick_settings")
                    }
                    text.contains("split screen") -> {
                        advancedSystemController.performQuickAction("split_screen")
                    }
                    else -> {
                        "I didn't understand that command. Please try again or say 'help' for assistance. " +
                        "You can ask me to open apps, send messages, make calls, control system settings, " +
                        "take photos, handle emergencies, and much more!"
                    }
                }
            }
            
            else -> "I'm not sure how to help with that yet. Is there something else I can do for you?"
        }
    }
    
    private fun isAppName(text: String): Boolean {
        val commonApps = listOf(
            "whatsapp", "instagram", "facebook", "twitter", "youtube", "gmail", 
            "chrome", "maps", "camera", "gallery", "settings", "calculator",
            "calendar", "contacts", "messages", "phone", "music", "spotify"
        )
        return commonApps.any { text.contains(it) }
    }
    
    private fun extractAppName(text: String): String {
        val words = text.split(" ")
        val openIndex = words.indexOfFirst { it.contains("open") }
        
        return if (openIndex != -1 && openIndex < words.size - 1) {
            words[openIndex + 1]
        } else {
            // Try to find known app names
            val commonApps = mapOf(
                "whatsapp" to "WhatsApp",
                "instagram" to "Instagram", 
                "facebook" to "Facebook",
                "youtube" to "YouTube",
                "gmail" to "Gmail",
                "chrome" to "Chrome",
                "maps" to "Maps",
                "camera" to "Camera",
                "gallery" to "Gallery",
                "settings" to "Settings",
                "calculator" to "Calculator",
                "calendar" to "Calendar",
                "contacts" to "Contacts",
                "messages" to "Messages",
                "phone" to "Phone",
                "music" to "Music",
                "spotify" to "Spotify"
            )
            
            commonApps.entries.find { text.contains(it.key) }?.value ?: "unknown"
        }
    }
    
    private fun extractContact(text: String): String {
        val words = text.split(" ")
        val toIndex = words.indexOfFirst { it == "to" }
        
        return if (toIndex != -1 && toIndex < words.size - 1) {
            words[toIndex + 1]
        } else {
            // Try to extract names after common patterns
            val patterns = listOf("call", "text", "message", "send")
            for (pattern in patterns) {
                val patternIndex = words.indexOfFirst { it.contains(pattern) }
                if (patternIndex != -1 && patternIndex < words.size - 1) {
                    return words[patternIndex + 1]
                }
            }
            "unknown"
        }
    }
    
    private fun extractMessage(text: String): String {
        val sayingIndex = text.indexOf("saying")
        val thatIndex = text.indexOf("that")
        
        return when {
            sayingIndex != -1 -> text.substring(sayingIndex + 6).trim()
            thatIndex != -1 -> text.substring(thatIndex + 4).trim()
            else -> {
                val words = text.split(" ")
                val messageStart = words.indexOfFirst { 
                    it in listOf("message", "text", "saying", "that") 
                }
                if (messageStart != -1 && messageStart < words.size - 1) {
                    words.drop(messageStart + 1).joinToString(" ")
                } else {
                    "Hello"
                }
            }
        }
    }
    
    private fun extractEmail(text: String): String {
        val toIndex = text.indexOf("to")
        return if (toIndex != -1) {
            val afterTo = text.substring(toIndex + 2).trim()
            val words = afterTo.split(" ")
            words.firstOrNull { it.contains("@") } ?: words.firstOrNull() ?: "unknown"
        } else {
            "unknown"
        }
    }
    
    private fun extractSubject(text: String): String {
        val aboutIndex = text.indexOf("about")
        val subjectIndex = text.indexOf("subject")
        
        return when {
            aboutIndex != -1 -> {
                val afterAbout = text.substring(aboutIndex + 5).trim()
                afterAbout.split(" ").take(5).joinToString(" ")
            }
            subjectIndex != -1 -> {
                val afterSubject = text.substring(subjectIndex + 7).trim()
                afterSubject.split(" ").take(5).joinToString(" ")
            }
            else -> "Voice Message"
        }
    }
    
    private fun extractEmailBody(text: String): String {
        val bodyKeywords = listOf("saying", "message", "body", "content")
        
        for (keyword in bodyKeywords) {
            val index = text.indexOf(keyword)
            if (index != -1) {
                return text.substring(index + keyword.length).trim()
            }
        }
        
        return "This is a voice-generated email from Mobile Budhdhi."
    }
}