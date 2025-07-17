package com.mobilebudhdhi.controller.nlp

import java.util.regex.Pattern

data class VoiceCommand(
    val text: String,
    val timestamp: Long,
    val confidence: Float = 1.0f,
    val isPartial: Boolean = false
) {
    
    fun getCleanText(): String {
        return text.lowercase()
            .replace(Regex("[^a-zA-Z0-9\\s]"), "")
            .trim()
    }
    
    fun containsWakeWord(): Boolean {
        val cleanText = getCleanText()
        return cleanText.contains("budhdhi") || 
               cleanText.contains("buddy") || // Common mispronunciation
               cleanText.contains("buddha") || // Common mispronunciation
               cleanText.contains("buddhist") || // Common mispronunciation
               cleanText.contains("mobile") && (cleanText.contains("buddy") || cleanText.contains("budhi")) ||
               cleanText.contains("hey") && (cleanText.contains("buddy") || cleanText.contains("budhi")) ||
               cleanText.contains("ok") && (cleanText.contains("buddy") || cleanText.contains("budhi"))
    }
    
    fun getCommandType(): CommandType {
        val cleanText = getCleanText()
        
        return when {
            // Emergency patterns - highest priority
            cleanText.contains("emergency") || cleanText.contains("help me") || cleanText.contains("urgent") || 
            cleanText.contains("sos") || cleanText.contains("panic") || cleanText.contains("danger") ||
            cleanText.contains("call 911") || cleanText.contains("call police") || cleanText.contains("call ambulance") -> CommandType.EMERGENCY
            
            // Camera patterns
            matchesPattern(cleanText, "take|capture|shoot", "photo|picture|selfie|image") -> CommandType.CAMERA
            matchesPattern(cleanText, "record|start|capture", "video|movie|clip") -> CommandType.CAMERA
            cleanText.contains("flashlight") || cleanText.contains("torch") || cleanText.contains("flash") -> CommandType.CAMERA
            
            // Multi-action patterns
            cleanText.contains(" and then ") || cleanText.contains(" after that ") -> CommandType.MULTI_ACTION
            
            // Conditional patterns
            cleanText.contains("if ") && cleanText.contains(" then ") -> CommandType.CONDITIONAL
            
            // Device admin patterns
            cleanText.contains("lock screen") || cleanText.contains("unlock screen") ||
            cleanText.contains("restart") || cleanText.contains("reboot") || cleanText.contains("shutdown") -> CommandType.DEVICE_ADMIN
            
            // Location patterns
            cleanText.contains("where am i") || cleanText.contains("my location") || cleanText.contains("gps") ||
            matchesPattern(cleanText, "find|locate|where is", "nearest|closest") -> CommandType.LOCATION
            
            // Calendar patterns
            matchesPattern(cleanText, "schedule|add|create|set", "meeting|appointment|event|calendar") -> CommandType.CALENDAR
            cleanText.contains("what's my schedule") || cleanText.contains("calendar") -> CommandType.CALENDAR
            
            // Contacts patterns
            matchesPattern(cleanText, "add|create|save", "contact|number|person") -> CommandType.CONTACTS
            matchesPattern(cleanText, "find|search|look for", "contact|number|person") -> CommandType.CONTACTS
            
            // App control patterns
            matchesPattern(cleanText, "open|launch|start|run|go to", "whatsapp|instagram|facebook|youtube|gmail|chrome|maps|camera|gallery|settings|calculator|calendar|contacts|messages|phone|music|spotify") -> CommandType.APP_CONTROL
            cleanText.contains("switch to") || cleanText.contains("go to") -> CommandType.APP_CONTROL
            
            // Phone call patterns
            matchesPattern(cleanText, "call|dial|phone|ring", "mom|dad|brother|sister|friend|contact|boss|wife|husband") -> CommandType.PHONE_CALL
            cleanText.contains("make a call") || cleanText.contains("call someone") || cleanText.contains("redial") -> CommandType.PHONE_CALL
            cleanText.contains("answer call") || cleanText.contains("reject call") || cleanText.contains("end call") -> CommandType.PHONE_CALL
            
            // Messaging patterns
            matchesPattern(cleanText, "send|write|compose", "message|text|sms") -> CommandType.MESSAGING
            cleanText.contains("message to") || cleanText.contains("text to") || cleanText.contains("reply to") -> CommandType.MESSAGING
            
            // WhatsApp specific patterns
            cleanText.contains("whatsapp") || cleanText.contains("whats app") || cleanText.contains("wa") -> CommandType.WHATSAPP
            
            // Email patterns
            matchesPattern(cleanText, "send|write|compose|draft", "email|mail|gmail") -> CommandType.EMAIL
            cleanText.contains("email to") || cleanText.contains("mail to") || cleanText.contains("check email") -> CommandType.EMAIL
            
            // Music control patterns
            matchesPattern(cleanText, "play|start|resume|pause|stop|next|previous|skip", "music|song|track|playlist|album|audio") -> CommandType.MUSIC
            cleanText.contains("volume") && (cleanText.contains("music") || cleanText.contains("song") || cleanText.contains("audio")) -> CommandType.MUSIC
            
            // System control patterns
            cleanText.contains("volume") || cleanText.contains("brightness") || cleanText.contains("screen") -> CommandType.SYSTEM_CONTROL
            cleanText.contains("turn on") || cleanText.contains("turn off") || cleanText.contains("enable") || cleanText.contains("disable") -> CommandType.SYSTEM_CONTROL
            cleanText.contains("silent mode") || cleanText.contains("do not disturb") || cleanText.contains("vibrate") -> CommandType.SYSTEM_CONTROL
            
            // Connectivity patterns
            cleanText.contains("wifi") || cleanText.contains("bluetooth") || cleanText.contains("data") || 
            cleanText.contains("airplane") || cleanText.contains("hotspot") || cleanText.contains("mobile data") -> CommandType.CONNECTIVITY
            
            // Accessibility patterns
            cleanText.contains("read screen") || cleanText.contains("describe screen") || 
            cleanText.contains("what's on screen") || cleanText.contains("accessibility") -> CommandType.ACCESSIBILITY
            
            // Reading content patterns
            matchesPattern(cleanText, "read|tell me|what's in|show", "message|email|notification|text|screen") -> CommandType.READ_CONTENT
            
            // Weather patterns
            cleanText.contains("weather") || cleanText.contains("temperature") || cleanText.contains("forecast") ||
            cleanText.contains("rain") || cleanText.contains("sunny") || cleanText.contains("cloudy") -> CommandType.WEATHER
            
            // Time and date patterns
            cleanText.contains("time") || cleanText.contains("date") || cleanText.contains("day") || 
            cleanText.contains("schedule") || cleanText.contains("what day") || cleanText.contains("what time") -> CommandType.TIME_DATE
            
            // Search patterns
            matchesPattern(cleanText, "search|find|look for|google", "web|internet|online") -> CommandType.SEARCH
            cleanText.contains("search for") || cleanText.contains("google") -> CommandType.SEARCH
            
            // Navigation patterns
            matchesPattern(cleanText, "navigate|directions|route|map", "to|from|between") -> CommandType.NAVIGATION
            cleanText.contains("take me to") || cleanText.contains("how to get to") -> CommandType.NAVIGATION
            
            // Alarm and reminder patterns
            matchesPattern(cleanText, "set|create|add|remind", "alarm|reminder|timer|alert") -> CommandType.REMINDER
            cleanText.contains("wake me up") || cleanText.contains("remind me") -> CommandType.REMINDER
            
            // Help patterns
            cleanText.contains("help") || cleanText.contains("what can you do") || cleanText.contains("commands") ||
            cleanText.contains("how to") || cleanText.contains("assist") -> CommandType.HELP
            
            else -> CommandType.UNKNOWN
        }
    }
    
    private fun matchesPattern(text: String, actionPattern: String, objectPattern: String): Boolean {
        val actionRegex = "\\b(${actionPattern})\\b"
        val objectRegex = "\\b(${objectPattern})\\b"
        
        return Pattern.compile(actionRegex).matcher(text).find() && 
               Pattern.compile(objectRegex).matcher(text).find()
    }
    
    fun extractIntent(): CommandIntent {
        val commandType = getCommandType()
        val cleanText = getCleanText()
        
        return CommandIntent(
            type = commandType,
            action = extractAction(cleanText, commandType),
            target = extractTarget(cleanText, commandType),
            content = extractContent(cleanText, commandType),
            parameters = extractParameters(cleanText, commandType)
        )
    }
    
    private fun extractAction(text: String, type: CommandType): String {
        return when (type) {
            CommandType.APP_CONTROL -> {
                if (text.contains("open") || text.contains("launch") || text.contains("start")) "open"
                else if (text.contains("close") || text.contains("exit")) "close"
                else "open"
            }
            CommandType.PHONE_CALL -> {
                if (text.contains("end") || text.contains("hang up")) "end"
                else "call"
            }
            CommandType.MESSAGING, CommandType.WHATSAPP -> {
                if (text.contains("read")) "read"
                else "send"
            }
            CommandType.EMAIL -> {
                if (text.contains("read")) "read"
                else "send"
            }
            CommandType.MUSIC -> {
                when {
                    text.contains("play") || text.contains("start") -> "play"
                    text.contains("pause") || text.contains("stop") -> "pause"
                    text.contains("next") -> "next"
                    text.contains("previous") -> "previous"
                    else -> "play"
                }
            }
            else -> ""
        }
    }
    
    private fun extractTarget(text: String, type: CommandType): String {
        return when (type) {
            CommandType.APP_CONTROL -> {
                val appNames = listOf("whatsapp", "instagram", "facebook", "youtube", "gmail", 
                                     "chrome", "maps", "camera", "gallery", "settings")
                appNames.firstOrNull { text.contains(it) } ?: ""
            }
            CommandType.PHONE_CALL -> {
                val afterCall = text.substringAfter("call").trim()
                val words = afterCall.split(" ")
                if (words.isNotEmpty()) words[0] else ""
            }
            CommandType.MESSAGING, CommandType.WHATSAPP, CommandType.EMAIL -> {
                val toIndex = text.indexOf(" to ")
                if (toIndex != -1) {
                    val afterTo = text.substring(toIndex + 4).trim()
                    afterTo.split(" ")[0]
                } else ""
            }
            else -> ""
        }
    }
    
    private fun extractContent(text: String, type: CommandType): String {
        return when (type) {
            CommandType.MESSAGING, CommandType.WHATSAPP -> {
                val sayingIndex = text.indexOf("saying")
                val messageIndex = text.indexOf("message")
                
                when {
                    sayingIndex != -1 -> text.substring(sayingIndex + 6).trim()
                    messageIndex != -1 && text.length > messageIndex + 8 -> text.substring(messageIndex + 8).trim()
                    else -> ""
                }
            }
            CommandType.EMAIL -> {
                val bodyIndex = text.indexOf("body")
                val contentIndex = text.indexOf("content")
                
                when {
                    bodyIndex != -1 -> text.substring(bodyIndex + 4).trim()
                    contentIndex != -1 -> text.substring(contentIndex + 7).trim()
                    else -> ""
                }
            }
            else -> ""
        }
    }
    
    private fun extractParameters(text: String, type: CommandType): Map<String, String> {
        val params = mutableMapOf<String, String>()
        
        when (type) {
            CommandType.EMAIL -> {
                val subjectIndex = text.indexOf("subject")
                if (subjectIndex != -1) {
                    val afterSubject = text.substring(subjectIndex + 7).trim()
                    val bodyIndex = afterSubject.indexOf("body")
                    val subject = if (bodyIndex != -1) {
                        afterSubject.substring(0, bodyIndex).trim()
                    } else {
                        afterSubject.split(" ").take(5).joinToString(" ")
                    }
                    params["subject"] = subject
                }
            }
            CommandType.MUSIC -> {
                if (text.contains("volume")) {
                    if (text.contains("up")) params["volume"] = "up"
                    else if (text.contains("down")) params["volume"] = "down"
                }
            }
            CommandType.SYSTEM_CONTROL -> {
                if (text.contains("volume")) {
                    if (text.contains("up")) params["volume"] = "up"
                    else if (text.contains("down")) params["volume"] = "down"
                }
                if (text.contains("brightness")) {
                    if (text.contains("up") || text.contains("increase")) params["brightness"] = "up"
                    else if (text.contains("down") || text.contains("decrease")) params["brightness"] = "down"
                }
            }
            else -> {}
        }
        
        return params
    }
}

enum class CommandType {
    APP_CONTROL,
    PHONE_CALL,
    MESSAGING,
    WHATSAPP,
    EMAIL,
    MUSIC,
    SYSTEM_CONTROL,
    CONNECTIVITY,
    READ_CONTENT,
    WEATHER,
    TIME_DATE,
    SEARCH,
    NAVIGATION,
    REMINDER,
    HELP,
    EMERGENCY,
    CAMERA,
    MULTI_ACTION,
    CONDITIONAL,
    DEVICE_ADMIN,
    ACCESSIBILITY,
    LOCATION,
    CALENDAR,
    CONTACTS,
    UNKNOWN
}

data class CommandIntent(
    val type: CommandType,
    val action: String = "",
    val target: String = "",
    val content: String = "",
    val parameters: Map<String, String> = emptyMap()
)

