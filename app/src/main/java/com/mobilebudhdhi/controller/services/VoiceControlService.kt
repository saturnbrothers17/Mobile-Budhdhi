package com.mobilebudhdhi.controller.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.core.app.NotificationCompat
import com.mobilebudhdhi.controller.MainActivity
import com.mobilebudhdhi.controller.R
import com.mobilebudhdhi.controller.nlp.CommandProcessor
import com.mobilebudhdhi.controller.nlp.VoiceCommand
import kotlinx.coroutines.*
import java.util.*

class VoiceControlService : Service(), RecognitionListener, TextToSpeech.OnInitListener {
    
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var commandProcessor: CommandProcessor
    private lateinit var audioManager: AudioManager
    private lateinit var wakeLock: PowerManager.WakeLock
    private lateinit var handler: Handler
    
    private var isListening = false
    private var isSpeaking = false
    private var serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var notificationReceiver: BroadcastReceiver? = null
    private var restartRecognizerRunnable: Runnable? = null
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "MobileBudhdhi_Channel"
        private const val WAKE_WORD = "budhdhi"
        private const val WAKE_LOCK_TAG = "MobileBudhdhi:VoiceWakeLock"
        private const val RESTART_RECOGNIZER_DELAY = 1000L
        private const val ACTIVATION_TIMEOUT = 30000L // 30 seconds
        
        private var instance: VoiceControlService? = null
        
        fun getInstance(): VoiceControlService? = instance
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        initializeComponents()
        createNotificationChannel()
        registerNotificationReceiver()
        acquireWakeLock()
    }
    
    private fun initializeComponents() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(this)
        
        textToSpeech = TextToSpeech(this, this)
        textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                isSpeaking = true
            }
            
            override fun onDone(utteranceId: String?) {
                isSpeaking = false
                // Resume listening after speaking
                if (!isListening) {
                    startContinuousListening()
                }
            }
            
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                isSpeaking = false
            }
        })
        
        commandProcessor = CommandProcessor(this)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        handler = Handler(Looper.getMainLooper())
    }
    
    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or 
            PowerManager.ACQUIRE_CAUSES_WAKEUP or
            PowerManager.ON_AFTER_RELEASE,
            WAKE_LOCK_TAG
        )
        // Acquire without timeout to ensure continuous operation
        wakeLock.acquire()
        
        // Schedule periodic wake lock renewal to ensure service stays alive
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (::wakeLock.isInitialized) {
                    if (wakeLock.isHeld) {
                        wakeLock.release()
                    }
                    wakeLock.acquire()
                }
                // Schedule next renewal
                handler.postDelayed(this, 5 * 60 * 1000) // Every 5 minutes
            }
        }, 5 * 60 * 1000) // First renewal after 5 minutes
    }
    
    private fun registerNotificationReceiver() {
        notificationReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "com.mobilebudhdhi.ANNOUNCE_NOTIFICATION") {
                    val message = intent.getStringExtra("message") ?: return
                    speak(message)
                }
            }
        }
        
        val filter = IntentFilter("com.mobilebudhdhi.ANNOUNCE_NOTIFICATION")
        registerReceiver(notificationReceiver, filter)
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        
        // Register for screen on/off events to ensure we keep listening
        val screenFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        
        registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_SCREEN_OFF -> {
                        // Ensure we keep listening even when screen is off
                        if (!isListening && !isSpeaking) {
                            startContinuousListening()
                        }
                    }
                    Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> {
                        // Restart listening when screen turns on
                        if (!isListening && !isSpeaking) {
                            startContinuousListening()
                        }
                    }
                }
            }
        }, screenFilter)
        
        startContinuousListening()
        return START_STICKY
    }
    
    private fun startContinuousListening() {
        if (!isListening) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            
            speechRecognizer.startListening(intent)
            isListening = true
        }
    }
    
    override fun onResults(results: Bundle?) {
        results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let { matches ->
            if (matches.isNotEmpty()) {
                val spokenText = matches[0].lowercase()
                processVoiceInput(spokenText)
            }
        }
        
        // Restart listening for continuous operation
        serviceScope.launch {
            delay(500) // Brief pause before restarting
            isListening = false
            startContinuousListening()
        }
    }
    
    private fun processVoiceInput(spokenText: String) {
        val command = VoiceCommand(spokenText, System.currentTimeMillis())
        
        // Log the spoken text for debugging
        Log.d("MobileBudhdhi", "Heard: $spokenText")
        
        // Check for wake word or if already activated
        if (command.containsWakeWord() || commandProcessor.isActivated()) {
            // Acquire wake lock to ensure processing continues even if screen is off
            if (::wakeLock.isInitialized && !wakeLock.isHeld) {
                wakeLock.acquire()
            }
            
            // Play a subtle sound to indicate recognition
            audioManager.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD)
            
            serviceScope.launch {
                try {
                    // If it's just the wake word, provide a greeting
                    if (command.containsWakeWord() && spokenText.trim().split(" ").size <= 2) {
                        speak("Yes, I'm listening. How can I help you?")
                        return@launch
                    }
                    
                    // Process the command
                    val response = commandProcessor.processCommand(command)
                    if (response.isNotEmpty()) {
                        speak(response)
                    }
                } catch (e: Exception) {
                    Log.e("MobileBudhdhi", "Error processing command: ${e.message}", e)
                    speak("Sorry, I couldn't process that command. Please try again.")
                }
            }
        }
    }
    
    private fun speak(text: String) {
        if (::textToSpeech.isInitialized && textToSpeech.isSpeaking.not()) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }
    
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech.language = Locale.getDefault()
            textToSpeech.setSpeechRate(1.0f)
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Mobile Budhdhi Voice Control",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Continuous voice control service"
                setSound(null, null)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        // Create a pending intent for the main activity
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Mobile Budhdhi Active")
            .setContentText("Listening for voice commands...")
            .setSmallIcon(R.drawable.ic_mic)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(pendingIntent)
            .build()
    }
    
    override fun onError(error: Int) {
        // Handle speech recognition errors and restart
        isListening = false
        
        // Create a runnable for restarting recognition
        restartRecognizerRunnable = Runnable {
            if (!isListening && !isSpeaking) {
                startContinuousListening()
            } else {
                // If still speaking, try again later
                handler.postDelayed(restartRecognizerRunnable!!, RESTART_RECOGNIZER_DELAY)
            }
        }
        
        // Post with delay to avoid rapid restarts
        handler.postDelayed(restartRecognizerRunnable!!, RESTART_RECOGNIZER_DELAY)
    }
    
    override fun onPartialResults(partialResults: Bundle?) {
        // Handle partial results for better responsiveness
    }
    
    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {}
    override fun onEvent(eventType: Int, params: Bundle?) {}
    
    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
        textToSpeech.shutdown()
        serviceScope.cancel()
        
        // Release wake lock if held
        if (::wakeLock.isInitialized && wakeLock.isHeld) {
            wakeLock.release()
        }
        
        // Unregister notification receiver
        notificationReceiver?.let {
            unregisterReceiver(it)
            notificationReceiver = null
        }
        
        // Remove any pending callbacks
        if (::handler.isInitialized && restartRecognizerRunnable != null) {
            handler.removeCallbacks(restartRecognizerRunnable!!)
        }
        
        instance = null
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
}