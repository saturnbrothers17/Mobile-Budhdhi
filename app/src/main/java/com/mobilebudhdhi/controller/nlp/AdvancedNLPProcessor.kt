package com.mobilebudhdhi.controller.nlp

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

/**
 * Advanced NLP processor that provides more sophisticated natural language understanding
 * capabilities for better command recognition and intent extraction.
 */
class AdvancedNLPProcessor(private val context: Context) {
    
    private val commandPatterns = mutableMapOf<String, List<String>>()
    private val entityRecognizers = mutableMapOf<String, (String) -> String?>()
    private val intentClassifiers = mutableListOf<IntentClassifier>()
    
    init {
        initializePatterns()
        initializeEntityRecognizers()
        initializeIntentClassifiers()
    }
    
    private fun initializePatterns() {
        // App control patterns
        commandPatterns["app_open"] = listOf(
            "open (.*)",
            "launch (.*)",
            "start (.*)",
            "run (.*)",
            "go to (.*)"
        )
        
        // Call patterns
        commandPatterns["call_make"] = listOf(
            "call (.*)",
            "dial (.*)",
            "phone (.*)",
            "ring (.*)"
        )
        
        // Message patterns
        commandPatterns["message_send"] = listOf(
            "send (.*) message to (.*)",
            "text (.*) to (.*)",
            "message (.*) saying (.*)",
            "send message to (.*) saying (.*)"
        )
        
        // More patterns can be added here
    }
    
    private fun initializeEntityRecognizers() {
        // Contact recognizer
        entityRecognizers["contact"] = { text ->
            val contactPatterns = listOf(
                "to ([a-zA-Z0-9 ]+)",
                "call ([a-zA-Z0-9 ]+)",
                "message ([a-zA-Z0-9 ]+)"
            )
            
            for (pattern in contactPatterns) {
                val regex = Regex(pattern)
                val match = regex.find(text)
                if (match != null && match.groupValues.size > 1) {
                    return@entityRecognizers match.groupValues[1].trim()
                }
            }
            
            null
        }
        
        // App name recognizer
        entityRecognizers["app"] = { text ->
            val appNames = listOf(
                "whatsapp", "facebook", "instagram", "youtube", "chrome", 
                "gmail", "maps", "camera", "settings", "calculator", "calendar"
            )
            
            for (app in appNames) {
                if (text.contains(app, ignoreCase = true)) {
                    return@entityRecognizers app
                }
            }
            
            null
        }
        
        // Message content recognizer
        entityRecognizers["message_content"] = { text ->
            val contentPatterns = listOf(
                "saying (.*)",
                "that says (.*)",
                "with message (.*)",
                "with text (.*)"
            )
            
            for (pattern in contentPatterns) {
                val regex = Regex(pattern)
                val match = regex.find(text)
                if (match != null && match.groupValues.size > 1) {
                    return@entityRecognizers match.groupValues[1].trim()
                }
            }
            
            null
        }
    }
    
    private fun initializeIntentClassifiers() {
        // Add basic intent classifiers
        intentClassifiers.add(IntentClassifier(
            intent = "open_app",
            patterns = listOf(
                "open (.*)",
                "launch (.*)",
                "start (.*)",
                "run (.*)"
            ),
            threshold = 0.7f
        ))
        
        intentClassifiers.add(IntentClassifier(
            intent = "make_call",
            patterns = listOf(
                "call (.*)",
                "dial (.*)",
                "phone (.*)"
            ),
            threshold = 0.7f
        ))
        
        intentClassifiers.add(IntentClassifier(
            intent = "send_message",
            patterns = listOf(
                "send message",
                "text (.*)",
                "message (.*)"
            ),
            threshold = 0.7f
        ))
        
        // Add more classifiers for other intents
    }
    
    suspend fun processAdvancedCommand(command: VoiceCommand): EnhancedIntent = withContext(Dispatchers.Default) {
        val text = command.text.lowercase().trim()
        
        // First try pattern matching for common commands
        for ((intentName, patterns) in commandPatterns) {
            for (pattern in patterns) {
                val regex = Regex(pattern)
                if (regex.matches(text)) {
                    val match = regex.find(text)
                    if (match != null) {
                        val entities = extractEntities(text)
                        return@withContext EnhancedIntent(
                            intent = intentName,
                            confidence = 0.9f,
                            entities = entities
                        )
                    }
                }
            }
        }
        
        // If no direct pattern match, try intent classification
        val classifiedIntent = classifyIntent(text)
        if (classifiedIntent.confidence > 0.5f) {
            val entities = extractEntities(text)
            return@withContext EnhancedIntent(
                intent = classifiedIntent.intent,
                confidence = classifiedIntent.confidence,
                entities = entities
            )
        }
        
        // Fallback to basic command type
        val commandType = command.getCommandType()
        val entities = extractEntities(text)
        
        EnhancedIntent(
            intent = commandType.toString().lowercase(),
            confidence = 0.5f,
            entities = entities
        )
    }
    
    private fun extractEntities(text: String): Map<String, String> {
        val entities = mutableMapOf<String, String>()
        
        for ((entityType, recognizer) in entityRecognizers) {
            val value = recognizer(text)
            if (value != null) {
                entities[entityType] = value
            }
        }
        
        return entities
    }
    
    private fun classifyIntent(text: String): ClassifiedIntent {
        var bestIntent = "unknown"
        var bestScore = 0.0f
        
        for (classifier in intentClassifiers) {
            val score = classifier.classify(text)
            if (score > bestScore && score >= classifier.threshold) {
                bestScore = score
                bestIntent = classifier.intent
            }
        }
        
        return ClassifiedIntent(bestIntent, bestScore)
    }
    
    fun learnFromInteraction(command: String, actualIntent: String) {
        // In a real implementation, this would update the model based on user interactions
        // For now, we'll just log it
        Log.d("AdvancedNLP", "Learning: '$command' -> $actualIntent")
    }
    
    data class ClassifiedIntent(val intent: String, val confidence: Float)
    
    data class IntentClassifier(
        val intent: String,
        val patterns: List<String>,
        val threshold: Float
    ) {
        fun classify(text: String): Float {
            var maxScore = 0.0f
            
            for (pattern in patterns) {
                val regex = Regex(pattern)
                if (regex.containsMatchIn(text)) {
                    val match = regex.find(text)
                    if (match != null) {
                        val matchLength = match.value.length
                        val score = matchLength.toFloat() / text.length
                        if (score > maxScore) {
                            maxScore = score
                        }
                    }
                }
            }
            
            return maxScore
        }
    }
}

data class EnhancedIntent(
    val intent: String,
    val confidence: Float,
    val entities: Map<String, String> = emptyMap()
)