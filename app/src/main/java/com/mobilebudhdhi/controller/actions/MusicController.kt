package com.mobilebudhdhi.controller.actions

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.view.KeyEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MusicController(private val context: Context) {
    
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    
    suspend fun playMusic(): String = withContext(Dispatchers.IO) {
        try {
            // First try to wake the device if it's locked
            val systemController = SystemController(context)
            systemController.wakeScreen()
            
            // Try to resume current music first
            sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY)
            
            // Wait a moment to see if music starts playing
            kotlinx.coroutines.delay(1000)
            
            // Check if music is playing by checking for notifications
            val notificationService = com.mobilebudhdhi.controller.services.NotificationListenerService.getInstance()
            if (notificationService != null) {
                val musicApps = listOf(
                    "com.spotify.music",
                    "com.google.android.music",
                    "com.google.android.apps.youtube.music",
                    "com.amazon.mp3",
                    "com.apple.android.music"
                )
                
                val musicNotifications = notificationService.getActiveNotifications().filter { 
                    musicApps.contains(it.packageName) 
                }
                
                if (musicNotifications.isNotEmpty()) {
                    val notification = musicNotifications.first()
                    return@withContext "Playing music: ${notification.title} - ${notification.text}"
                }
            }
            
            // If no music is playing, try to open a music app
            val musicApps = listOf(
                "com.spotify.music",
                "com.google.android.music",
                "com.google.android.apps.youtube.music",
                "com.amazon.mp3",
                "com.apple.android.music",
                "com.gaana",
                "com.jio.media.jiobeats"
            )
            
            for (packageName in musicApps) {
                val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    
                    // Wait for app to open
                    kotlinx.coroutines.delay(2000)
                    
                    // Try to click play button using accessibility service
                    val accessibilityService = com.mobilebudhdhi.controller.services.MobileBuddhiAccessibilityService.getInstance()
                    if (accessibilityService != null) {
                        val playButtonClicked = accessibilityService.findAndClickText("Play") ||
                                               accessibilityService.findAndClickText("PLAY") ||
                                               accessibilityService.findAndClickText("▶")
                        
                        if (playButtonClicked) {
                            return@withContext "Started playing music in your music app"
                        }
                    }
                    
                    return@withContext "Opening music app and starting playback"
                }
            }
            
            "Music playback started. If no music was playing, please open your preferred music app first."
        } catch (e: Exception) {
            "Error starting music: ${e.message}"
        }
    }
    
    suspend fun pauseMusic(): String = withContext(Dispatchers.IO) {
        try {
            sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PAUSE)
            "Music paused"
        } catch (e: Exception) {
            "Error pausing music: ${e.message}"
        }
    }
    
    suspend fun stopMusic(): String = withContext(Dispatchers.IO) {
        try {
            sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_STOP)
            "Music stopped"
        } catch (e: Exception) {
            "Error stopping music: ${e.message}"
        }
    }
    
    suspend fun nextTrack(): String = withContext(Dispatchers.IO) {
        try {
            sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_NEXT)
            "Playing next track"
        } catch (e: Exception) {
            "Error skipping to next track: ${e.message}"
        }
    }
    
    suspend fun previousTrack(): String = withContext(Dispatchers.IO) {
        try {
            sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            "Playing previous track"
        } catch (e: Exception) {
            "Error going to previous track: ${e.message}"
        }
    }
    
    suspend fun playPauseToggle(): String = withContext(Dispatchers.IO) {
        try {
            sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            "Music play/pause toggled"
        } catch (e: Exception) {
            "Error toggling music: ${e.message}"
        }
    }
    
    suspend fun openSpotify(): String = withContext(Dispatchers.IO) {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage("com.spotify.music")
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                "Opening Spotify"
            } else {
                "Spotify not installed"
            }
        } catch (e: Exception) {
            "Error opening Spotify: ${e.message}"
        }
    }
    
    suspend fun openYouTubeMusic(): String = withContext(Dispatchers.IO) {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage("com.google.android.apps.youtube.music")
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                "Opening YouTube Music"
            } else {
                "YouTube Music not installed"
            }
        } catch (e: Exception) {
            "Error opening YouTube Music: ${e.message}"
        }
    }
    
    suspend fun playPlaylist(playlistName: String): String = withContext(Dispatchers.IO) {
        try {
            // This would require specific integration with music services
            // For now, we'll open the music app and let user manually select
            playMusic()
            "Opening music app. Please manually select playlist: $playlistName"
        } catch (e: Exception) {
            "Error playing playlist: ${e.message}"
        }
    }
    
    suspend fun searchAndPlay(songName: String): String = withContext(Dispatchers.IO) {
        try {
            // Try YouTube Music first
            val ytMusicIntent = Intent(Intent.ACTION_SEARCH).apply {
                setPackage("com.google.android.apps.youtube.music")
                putExtra("query", songName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            if (ytMusicIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(ytMusicIntent)
                return@withContext "Searching for '$songName' in YouTube Music"
            }
            
            // Try Spotify
            val spotifyIntent = Intent(Intent.ACTION_SEARCH).apply {
                setPackage("com.spotify.music")
                putExtra("query", songName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            if (spotifyIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(spotifyIntent)
                return@withContext "Searching for '$songName' in Spotify"
            }
            
            "No compatible music app found for search"
        } catch (e: Exception) {
            "Error searching for song: ${e.message}"
        }
    }
    
    suspend fun volumeUp(): String = withContext(Dispatchers.IO) {
        try {
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_RAISE,
                AudioManager.FLAG_SHOW_UI
            )
            "Music volume increased"
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
            "Music volume decreased"
        } catch (e: Exception) {
            "Error decreasing volume: ${e.message}"
        }
    }
    
    suspend fun getCurrentTrackInfo(): String = withContext(Dispatchers.IO) {
        try {
            // Try to get track info from notification service
            val notificationService = com.mobilebudhdhi.controller.services.NotificationListenerService.getInstance()
            
            if (notificationService != null) {
                val musicApps = listOf(
                    "com.spotify.music",
                    "com.google.android.music",
                    "com.google.android.apps.youtube.music",
                    "com.amazon.mp3",
                    "com.apple.android.music"
                )
                
                val musicNotifications = notificationService.getActiveNotifications().filter { 
                    musicApps.contains(it.packageName) 
                }
                
                if (musicNotifications.isNotEmpty()) {
                    val notification = musicNotifications.first()
                    return@withContext "Currently playing: ${notification.title} - ${notification.text}"
                }
            }
            
            // If no track info found, use accessibility service
            val accessibilityService = com.mobilebudhdhi.controller.services.MobileBuddhiAccessibilityService.getInstance()
            if (accessibilityService != null) {
                val screenText = accessibilityService.getAllTextFromScreen()
                
                if (screenText.contains("playing") || screenText.contains("song") || 
                    screenText.contains("track") || screenText.contains("artist")) {
                    return@withContext "I found music information: ${screenText.take(100)}..."
                }
            }
            
            "Unable to get current track information"
        } catch (e: Exception) {
            "Error getting track info: ${e.message}"
        }
    }
    
    private fun sendMediaKeyEvent(keyCode: Int) {
        val keyEvent = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
        audioManager.dispatchMediaKeyEvent(keyEvent)
        
        val keyEventUp = KeyEvent(KeyEvent.ACTION_UP, keyCode)
        audioManager.dispatchMediaKeyEvent(keyEventUp)
    }
}