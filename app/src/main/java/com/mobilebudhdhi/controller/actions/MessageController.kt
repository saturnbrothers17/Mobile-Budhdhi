package com.mobilebudhdhi.controller.actions

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Telephony
import android.telephony.SmsManager
import android.database.Cursor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MessageController(private val context: Context) {
    
    private val smsManager = SmsManager.getDefault()
    
    suspend fun sendSMS(contact: String, message: String): String = withContext(Dispatchers.IO) {
        try {
            val phoneNumber = resolveContact(contact)
            if (phoneNumber != null) {
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
                "Message sent to $contact: $message"
            } else {
                "Contact $contact not found"
            }
        } catch (e: Exception) {
            "Error sending message: ${e.message}"
        }
    }
    
    suspend fun readMessages(): String = withContext(Dispatchers.IO) {
        try {
            // First try to get messages from SMS database
            val messages = getRecentMessages(5)
            
            // Then try to get messages from notification service
            val notificationService = com.mobilebudhdhi.controller.services.NotificationListenerService.getInstance()
            val notificationMessages = notificationService?.getSMSMessages() ?: emptyList()
            
            // Combine both sources
            if (messages.isNotEmpty() || notificationMessages.isNotEmpty()) {
                val messageText = StringBuilder("Here are your recent messages:\n")
                
                // Add database messages
                if (messages.isNotEmpty()) {
                    messages.forEach { message ->
                        messageText.append("From ${message.sender}: ${message.body}\n")
                    }
                }
                
                // Add notification messages that aren't duplicates
                if (notificationMessages.isNotEmpty()) {
                    notificationMessages.forEach { notification ->
                        // Check if this notification is already included in database messages
                        val isDuplicate = messages.any { 
                            it.body.contains(notification.text) || notification.text.contains(it.body) 
                        }
                        
                        if (!isDuplicate) {
                            messageText.append("From ${notification.title}: ${notification.text}\n")
                        }
                    }
                }
                
                return@withContext messageText.toString().trim()
            } else {
                "No recent messages found"
            }
        } catch (e: Exception) {
            "Error reading messages: ${e.message}"
        }
    }
    
    suspend fun sendWhatsAppMessage(contact: String, message: String): String = withContext(Dispatchers.IO) {
        try {
            val phoneNumber = resolveContact(contact)
            if (phoneNumber != null) {
                // First try direct WhatsApp API
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://api.whatsapp.com/send?phone=$phoneNumber&text=${Uri.encode(message)}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                
                // Use accessibility service to complete the action
                kotlinx.coroutines.delay(2000) // Wait for WhatsApp to open
                
                val accessibilityService = com.mobilebudhdhi.controller.services.MobileBuddhiAccessibilityService.getInstance()
                if (accessibilityService != null) {
                    // Try to find and click the send button
                    val sendButtonClicked = accessibilityService.findAndClickText("Send")
                    
                    if (sendButtonClicked) {
                        return@withContext "Message sent to $contact via WhatsApp: $message"
                    }
                }
                
                "Opening WhatsApp to send message to $contact"
            } else {
                "Contact $contact not found"
            }
        } catch (e: Exception) {
            "Error sending WhatsApp message: ${e.message}"
        }
    }
    
    suspend fun readWhatsAppMessages(): String = withContext(Dispatchers.IO) {
        try {
            // Try to get WhatsApp messages from notification service
            val notificationService = com.mobilebudhdhi.controller.services.NotificationListenerService.getInstance()
            
            if (notificationService != null) {
                val whatsAppMessages = notificationService.getWhatsAppMessages()
                
                if (whatsAppMessages.isNotEmpty()) {
                    val messageText = whatsAppMessages.take(5).joinToString("\n") { 
                        "From ${it.title}: ${it.text}" 
                    }
                    return@withContext "Here are your recent WhatsApp messages:\n$messageText"
                }
            }
            
            // If no messages found or service not running, try to open WhatsApp
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setPackage("com.whatsapp")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            
            // Use accessibility service to read screen content
            val accessibilityService = com.mobilebudhdhi.controller.services.MobileBuddhiAccessibilityService.getInstance()
            if (accessibilityService != null) {
                // Give WhatsApp time to open
                kotlinx.coroutines.delay(1500)
                val screenText = accessibilityService.getAllTextFromScreen()
                
                if (screenText.isNotEmpty()) {
                    return@withContext "I found these messages in WhatsApp: ${screenText.take(200)}..."
                }
            }
            
            "Opening WhatsApp to check your messages"
        } catch (e: Exception) {
            "Error accessing WhatsApp messages: ${e.message}"
        }
    }
    
    private fun resolveContact(contactName: String): String? {
        val cursor: Cursor? = context.contentResolver.query(
            android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER,
                android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
            ),
            "${android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
            arrayOf("%$contactName%"),
            null
        )
        
        cursor?.use {
            if (it.moveToFirst()) {
                val phoneNumber = it.getString(0)
                return phoneNumber?.replace(Regex("[^+\\d]"), "")
            }
        }
        
        // If not found by name, check if it's already a phone number
        return if (contactName.matches(Regex("[+]?[0-9\\s-()]+")) && contactName.length >= 10) {
            contactName.replace(Regex("[^+\\d]"), "")
        } else null
    }
    
    private fun getRecentMessages(limit: Int): List<SMSMessage> {
        val messages = mutableListOf<SMSMessage>()
        
        val cursor: Cursor? = context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.TYPE
            ),
            null,
            null,
            "${Telephony.Sms.DATE} DESC LIMIT $limit"
        )
        
        cursor?.use {
            while (it.moveToNext()) {
                val address = it.getString(0) ?: "Unknown"
                val body = it.getString(1) ?: ""
                val date = it.getLong(2)
                val type = it.getInt(3)
                
                val sender = if (type == Telephony.Sms.MESSAGE_TYPE_INBOX) {
                    getContactName(address) ?: address
                } else {
                    "You"
                }
                
                messages.add(SMSMessage(sender, body, date))
            }
        }
        
        return messages
    }
    
    private fun getContactName(phoneNumber: String): String? {
        val cursor: Cursor? = context.contentResolver.query(
            android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME),
            "${android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER} = ?",
            arrayOf(phoneNumber),
            null
        )
        
        cursor?.use {
            if (it.moveToFirst()) {
                return it.getString(0)
            }
        }
        
        return null
    }
    
    data class SMSMessage(
        val sender: String,
        val body: String,
        val timestamp: Long
    )
}