package com.mobilebudhdhi.controller.services

import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.*

class NotificationListenerService : NotificationListenerService() {
    
    private var serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    companion object {
        private var instance: NotificationListenerService? = null
        
        fun getInstance(): NotificationListenerService? = instance
        
        fun isServiceRunning(): Boolean = instance != null
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
    }
    
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        
        sbn?.let { notification ->
            serviceScope.launch {
                handleNewNotification(notification)
            }
        }
    }
    
    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        // Handle notification removal if needed
    }
    
    private suspend fun handleNewNotification(sbn: StatusBarNotification) = withContext(Dispatchers.IO) {
        try {
            val packageName = sbn.packageName
            val notification = sbn.notification
            val extras = notification.extras
            
            val title = extras.getCharSequence("android.title")?.toString() ?: ""
            val text = extras.getCharSequence("android.text")?.toString() ?: ""
            val bigText = extras.getCharSequence("android.bigText")?.toString() ?: text
            
            // Skip our own notifications
            if (packageName == applicationContext.packageName) {
                return@withContext
            }
            
            // Skip empty notifications
            if (title.isEmpty() && text.isEmpty() && bigText.isEmpty()) {
                return@withContext
            }
            
            // Get app name for better announcements
            val appName = try {
                val packageManager = applicationContext.packageManager
                packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
            } catch (e: Exception) {
                packageName.substringAfterLast('.')
            }
            
            // Process based on package
            when (packageName) {
                "com.whatsapp" -> {
                    handleWhatsAppNotification(title, bigText)
                    
                    // Announce important WhatsApp messages
                    if (!title.contains("WhatsApp") && !title.contains("new messages") && title.isNotEmpty()) {
                        announceNotification("New WhatsApp message from $title")
                    }
                }
                "com.google.android.apps.messaging" -> {
                    handleSMSNotification(title, bigText)
                    
                    // Announce SMS
                    if (title.isNotEmpty()) {
                        announceNotification("New text message from $title")
                    }
                }
                "com.google.android.gm" -> {
                    handleGmailNotification(title, bigText)
                    
                    // Announce important emails
                    if (title.isNotEmpty() && !title.contains("new emails")) {
                        announceNotification("New email from $title")
                    }
                }
                "com.android.dialer", "com.google.android.dialer" -> {
                    handleCallNotification(title, bigText)
                    
                    // Announce incoming calls
                    if (text.contains("incoming") || title.contains("incoming") || 
                        text.contains("calling") || title.contains("calling")) {
                        announceNotification("Incoming call from $title")
                    }
                }
                // Handle other popular messaging apps
                "org.telegram.messenger" -> {
                    handleGenericNotification(packageName, title, bigText)
                    announceNotification("New Telegram message from $title")
                }
                "com.facebook.orca" -> {
                    handleGenericNotification(packageName, title, bigText)
                    announceNotification("New Messenger message from $title")
                }
                "com.instagram.android" -> {
                    handleGenericNotification(packageName, title, bigText)
                    if (title.isNotEmpty()) {
                        announceNotification("New Instagram notification from $title")
                    }
                }
                else -> {
                    handleGenericNotification(packageName, title, bigText)
                    
                    // Only announce important notifications from other apps
                    val importantKeywords = listOf("urgent", "important", "alert", "warning", "critical", "emergency")
                    val isImportant = importantKeywords.any { 
                        title.contains(it, ignoreCase = true) || text.contains(it, ignoreCase = true) 
                    }
                    
                    if (isImportant) {
                        announceNotification("Important notification from $appName: $title")
                    }
                }
            }
            
            // Store notification in history
            storeNotification(NotificationData(packageName, title, bigText, sbn.postTime))
            
        } catch (e: Exception) {
            // Handle error silently
        }
    }
    
    private fun announceNotification(message: String) {
        val intent = Intent("com.mobilebudhdhi.ANNOUNCE_NOTIFICATION")
        intent.putExtra("message", message)
        sendBroadcast(intent)
    }
    
    // Store recent notifications for retrieval
    private val notificationHistory = mutableListOf<NotificationData>()
    private val maxHistorySize = 50
    
    private fun storeNotification(notification: NotificationData) {
        synchronized(notificationHistory) {
            notificationHistory.add(0, notification)
            if (notificationHistory.size > maxHistorySize) {
                notificationHistory.removeAt(notificationHistory.size - 1)
            }
        }
    }
    
    fun getNotificationHistory(): List<NotificationData> {
        synchronized(notificationHistory) {
            return notificationHistory.toList()
        }
    }
    
    private fun handleWhatsAppNotification(title: String, text: String) {
        if (title.isNotEmpty() && text.isNotEmpty()) {
            val intent = Intent("com.mobilebudhdhi.WHATSAPP_MESSAGE")
            intent.putExtra("sender", title)
            intent.putExtra("message", text)
            intent.putExtra("timestamp", System.currentTimeMillis())
            sendBroadcast(intent)
        }
    }
    
    private fun handleSMSNotification(title: String, text: String) {
        if (title.isNotEmpty() && text.isNotEmpty()) {
            val intent = Intent("com.mobilebudhdhi.SMS_MESSAGE")
            intent.putExtra("sender", title)
            intent.putExtra("message", text)
            intent.putExtra("timestamp", System.currentTimeMillis())
            sendBroadcast(intent)
        }
    }
    
    private fun handleGmailNotification(title: String, text: String) {
        if (title.isNotEmpty() && text.isNotEmpty()) {
            val intent = Intent("com.mobilebudhdhi.EMAIL_MESSAGE")
            intent.putExtra("sender", title)
            intent.putExtra("message", text)
            intent.putExtra("timestamp", System.currentTimeMillis())
            sendBroadcast(intent)
        }
    }
    
    private fun handleCallNotification(title: String, text: String) {
        val intent = Intent("com.mobilebudhdhi.CALL_NOTIFICATION")
        intent.putExtra("caller", title)
        intent.putExtra("details", text)
        intent.putExtra("timestamp", System.currentTimeMillis())
        sendBroadcast(intent)
    }
    
    private fun handleGenericNotification(packageName: String, title: String, text: String) {
        // Handle other app notifications if needed
        val intent = Intent("com.mobilebudhdhi.GENERIC_NOTIFICATION")
        intent.putExtra("package", packageName)
        intent.putExtra("title", title)
        intent.putExtra("text", text)
        intent.putExtra("timestamp", System.currentTimeMillis())
        sendBroadcast(intent)
    }
    
    fun getActiveNotifications(): List<NotificationData> {
        val notifications = mutableListOf<NotificationData>()
        
        try {
            activeNotifications?.forEach { sbn ->
                val extras = sbn.notification.extras
                val title = extras.getCharSequence("android.title")?.toString() ?: ""
                val text = extras.getCharSequence("android.text")?.toString() ?: ""
                val bigText = extras.getCharSequence("android.bigText")?.toString() ?: text
                
                notifications.add(
                    NotificationData(
                        packageName = sbn.packageName,
                        title = title,
                        text = bigText,
                        timestamp = sbn.postTime
                    )
                )
            }
        } catch (e: Exception) {
            // Handle error
        }
        
        return notifications
    }
    
    fun getWhatsAppMessages(): List<NotificationData> {
        return getActiveNotifications().filter { it.packageName == "com.whatsapp" }
    }
    
    fun getSMSMessages(): List<NotificationData> {
        return getActiveNotifications().filter { it.packageName == "com.google.android.apps.messaging" }
    }
    
    fun getEmailMessages(): List<NotificationData> {
        return getActiveNotifications().filter { it.packageName == "com.google.android.gm" }
    }
    
    fun clearNotification(packageName: String) {
        try {
            activeNotifications?.forEach { sbn ->
                if (sbn.packageName == packageName) {
                    cancelNotification(sbn.key)
                }
            }
        } catch (e: Exception) {
            // Handle error
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        instance = null
        serviceScope.cancel()
    }
    
    data class NotificationData(
        val packageName: String,
        val title: String,
        val text: String,
        val timestamp: Long
    )
}