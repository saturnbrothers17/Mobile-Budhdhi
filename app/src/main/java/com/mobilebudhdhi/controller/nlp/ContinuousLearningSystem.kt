package com.mobilebudhdhi.controller.nlp

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.util.*

/**
 * A system that continuously learns from user interactions to improve
 * command recognition and response accuracy over time.
 */
class ContinuousLearningSystem(private val context: Context) {
    
    private val userPreferences = UserPreferences()
    private val commandHistory = mutableListOf<CommandHistoryEntry>()
    private val patternDatabase = mutableMapOf<String, MutableList<String>>()
    private val frequentCommands = mutableMapOf<String, Int>()
    private val commandCorrections = mutableMapOf<String, String>()
    
    companion object {
        private const val TAG = "ContinuousLearning"
        private const val MAX_HISTORY_SIZE = 100
        private const val PREFERENCES_FILE = "command_preferences"
        private const val PATTERNS_FILE = "command_patterns"
        private const val HISTORY_FILE = "command_history"
    }
    
    init {
        loadData()
    }
    
    suspend fun processCommand(command: VoiceCommand): VoiceCommand = withContext(Dispatchers.Default) {
        // Record command in history
        addToHistory(command)
        
        // Check for corrections
        val correctedText = checkForCorrections(command.text)
        if (correctedText != command.text) {
            return@withContext command.copy(text = correctedText)
        }
        
        // Update frequency counter
        updateCommandFrequency(command.text)
        
        // Return possibly enhanced command
        command
    }
    
    fun recordCommandSuccess(command: String, intent: String, successful: Boolean) {
        val lastEntry = commandHistory.lastOrNull()
        if (lastEntry?.command == command) {
            lastEntry.successful = successful
            lastEntry.recognizedIntent = intent
        }
        
        // If successful, add to pattern database
        if (successful) {
            addToPatternDatabase(command, intent)
        }
        
        // Save data periodically
        if (commandHistory.size % 10 == 0) {
            saveData()
        }
    }
    
    fun addCorrection(originalCommand: String, correctedCommand: String) {
        commandCorrections[originalCommand.lowercase()] = correctedCommand
        saveData()
    }
    
    fun setPreference(key: String, value: Any) {
        when (value) {
            is String -> userPreferences.stringPreferences[key] = value
            is Boolean -> userPreferences.booleanPreferences[key] = value
            is Int -> userPreferences.intPreferences[key] = value
            is Float -> userPreferences.floatPreferences[key] = value
        }
        saveData()
    }
    
    fun getPreference(key: String): Any? {
        return userPreferences.stringPreferences[key] ?:
               userPreferences.booleanPreferences[key] ?:
               userPreferences.intPreferences[key] ?:
               userPreferences.floatPreferences[key]
    }
    
    fun getFrequentCommands(limit: Int = 5): List<String> {
        return frequentCommands.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key }
    }
    
    fun getSimilarCommands(partialCommand: String, limit: Int = 3): List<String> {
        val results = mutableListOf<Pair<String, Float>>()
        
        for (command in commandHistory.map { it.command }) {
            val similarity = calculateSimilarity(partialCommand, command)
            if (similarity > 0.6f) {  // Threshold for similarity
                results.add(Pair(command, similarity))
            }
        }
        
        return results.sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }
    
    fun getPredictedIntent(command: String): String? {
        // Check pattern database first
        for ((intent, patterns) in patternDatabase) {
            for (pattern in patterns) {
                if (command.contains(pattern, ignoreCase = true)) {
                    return intent
                }
            }
        }
        
        // Check command history for similar commands
        val similarCommands = getSimilarCommands(command)
        for (similarCommand in similarCommands) {
            val historyEntry = commandHistory.firstOrNull { it.command == similarCommand && it.successful }
            if (historyEntry != null) {
                return historyEntry.recognizedIntent
            }
        }
        
        return null
    }
    
    private fun addToHistory(command: VoiceCommand) {
        val entry = CommandHistoryEntry(
            command = command.text,
            timestamp = command.timestamp,
            successful = false,
            recognizedIntent = ""
        )
        
        commandHistory.add(entry)
        
        // Keep history size manageable
        if (commandHistory.size > MAX_HISTORY_SIZE) {
            commandHistory.removeAt(0)
        }
    }
    
    private fun updateCommandFrequency(command: String) {
        val normalizedCommand = command.lowercase().trim()
        frequentCommands[normalizedCommand] = (frequentCommands[normalizedCommand] ?: 0) + 1
    }
    
    private fun addToPatternDatabase(command: String, intent: String) {
        val words = command.lowercase().split(" ")
        
        // Extract potential patterns (2-3 word sequences)
        val patterns = mutableListOf<String>()
        
        // Add single important words
        val importantWords = listOf("call", "open", "send", "play", "stop", "message", "email")
        for (word in words) {
            if (importantWords.contains(word)) {
                patterns.add(word)
            }
        }
        
        // Add 2-word patterns
        for (i in 0 until words.size - 1) {
            patterns.add("${words[i]} ${words[i+1]}")
        }
        
        // Add 3-word patterns
        for (i in 0 until words.size - 2) {
            patterns.add("${words[i]} ${words[i+1]} ${words[i+2]}")
        }
        
        // Add patterns to database
        if (!patternDatabase.containsKey(intent)) {
            patternDatabase[intent] = mutableListOf()
        }
        
        for (pattern in patterns) {
            if (!patternDatabase[intent]!!.contains(pattern)) {
                patternDatabase[intent]!!.add(pattern)
            }
        }
    }
    
    private fun checkForCorrections(command: String): String {
        val normalizedCommand = command.lowercase().trim()
        return commandCorrections[normalizedCommand] ?: command
    }
    
    private fun calculateSimilarity(s1: String, s2: String): Float {
        val longer = if (s1.length > s2.length) s1 else s2
        val shorter = if (s1.length > s2.length) s2 else s1
        
        if (longer.isEmpty()) return 1.0f
        
        val longerLength = longer.length
        val editDistance = levenshteinDistance(longer.lowercase(), shorter.lowercase())
        
        return (longerLength - editDistance) / longerLength.toFloat()
    }
    
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val costs = IntArray(s2.length + 1)
        
        for (i in 0..s2.length) {
            costs[i] = i
        }
        
        var i = 0
        while (i < s1.length) {
            var lastValue = i
            var j = 0
            while (j < s2.length) {
                val oldValue = costs[j]
                costs[j] = if (s1[i] == s2[j]) {
                    lastValue
                } else {
                    1 + minOf(lastValue, costs[j], costs[j + 1])
                }
                lastValue = oldValue
                j++
            }
            i++
            costs[s2.length] = i
        }
        
        return costs[s2.length]
    }
    
    private fun saveData() {
        try {
            // Save user preferences
            val prefsFile = File(context.filesDir, PREFERENCES_FILE)
            ObjectOutputStream(FileOutputStream(prefsFile)).use { out ->
                out.writeObject(userPreferences)
            }
            
            // Save pattern database
            val patternsFile = File(context.filesDir, PATTERNS_FILE)
            ObjectOutputStream(FileOutputStream(patternsFile)).use { out ->
                out.writeObject(patternDatabase)
            }
            
            // Save command history
            val historyFile = File(context.filesDir, HISTORY_FILE)
            ObjectOutputStream(FileOutputStream(historyFile)).use { out ->
                out.writeObject(commandHistory)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving learning data", e)
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    private fun loadData() {
        try {
            // Load user preferences
            val prefsFile = File(context.filesDir, PREFERENCES_FILE)
            if (prefsFile.exists()) {
                ObjectInputStream(FileInputStream(prefsFile)).use { input ->
                    val loadedPrefs = input.readObject() as UserPreferences
                    userPreferences.stringPreferences.putAll(loadedPrefs.stringPreferences)
                    userPreferences.booleanPreferences.putAll(loadedPrefs.booleanPreferences)
                    userPreferences.intPreferences.putAll(loadedPrefs.intPreferences)
                    userPreferences.floatPreferences.putAll(loadedPrefs.floatPreferences)
                }
            }
            
            // Load pattern database
            val patternsFile = File(context.filesDir, PATTERNS_FILE)
            if (patternsFile.exists()) {
                ObjectInputStream(FileInputStream(patternsFile)).use { input ->
                    val loadedPatterns = input.readObject() as Map<String, List<String>>
                    loadedPatterns.forEach { (intent, patterns) ->
                        patternDatabase[intent] = patterns.toMutableList()
                    }
                }
            }
            
            // Load command history
            val historyFile = File(context.filesDir, HISTORY_FILE)
            if (historyFile.exists()) {
                ObjectInputStream(FileInputStream(historyFile)).use { input ->
                    val loadedHistory = input.readObject() as List<CommandHistoryEntry>
                    commandHistory.addAll(loadedHistory)
                    
                    // Rebuild frequency map from history
                    commandHistory.forEach { entry ->
                        val normalizedCommand = entry.command.lowercase().trim()
                        frequentCommands[normalizedCommand] = (frequentCommands[normalizedCommand] ?: 0) + 1
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading learning data", e)
        }
    }
    
    data class CommandHistoryEntry(
        val command: String,
        val timestamp: Long,
        var successful: Boolean,
        var recognizedIntent: String
    ) : Serializable
    
    data class UserPreferences(
        val stringPreferences: MutableMap<String, String> = mutableMapOf(),
        val booleanPreferences: MutableMap<String, Boolean> = mutableMapOf(),
        val intPreferences: MutableMap<String, Int> = mutableMapOf(),
        val floatPreferences: MutableMap<String, Float> = mutableMapOf()
    ) : Serializable
}