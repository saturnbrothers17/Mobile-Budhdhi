package com.mobilebudhdhi.controller.actions

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EmailController(private val context: Context) {
    
    suspend fun sendEmail(recipient: String, subject: String, body: String): String = withContext(Dispatchers.IO) {
        try {
            // First try Gmail directly for better integration
            val gmailResult = sendGmailEmail(recipient, subject, body)
            
            // If Gmail isn't available, try generic email intent
            if (gmailResult.contains("No email app found")) {
                val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:")
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
                    putExtra(Intent.EXTRA_SUBJECT, subject)
                    putExtra(Intent.EXTRA_TEXT, body)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                
                if (emailIntent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(emailIntent)
                    
                    // Use accessibility service to help complete the action
                    kotlinx.coroutines.delay(2000) // Wait for email app to open
                    
                    val accessibilityService = com.mobilebudhdhi.controller.services.MobileBuddhiAccessibilityService.getInstance()
                    if (accessibilityService != null) {
                        // Try to find and click the send button
                        val sendButtonClicked = accessibilityService.findAndClickText("Send")
                        
                        if (sendButtonClicked) {
                            return@withContext "Email sent to $recipient with subject: $subject"
                        }
                    }
                    
                    "Opening email app to send message to $recipient"
                } else {
                    "No email app found. Please install Gmail or another email client."
                }
            } else {
                gmailResult
            }
        } catch (e: Exception) {
            "Error sending email: ${e.message}"
        }
    }
    
    private suspend fun sendGmailEmail(recipient: String, subject: String, body: String): String = withContext(Dispatchers.IO) {
        try {
            val gmailIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                setPackage("com.google.android.gm")
                putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            if (gmailIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(gmailIntent)
                "Opening Gmail to send message to $recipient"
            } else {
                "No email app found. Please install Gmail or another email client."
            }
        } catch (e: Exception) {
            "Error opening Gmail: ${e.message}"
        }
    }
    
    suspend fun readEmails(): String = withContext(Dispatchers.IO) {
        try {
            // First try to get emails from notification service
            val notificationService = com.mobilebudhdhi.controller.services.NotificationListenerService.getInstance()
            val emailNotifications = notificationService?.getEmailMessages()
            
            if (emailNotifications != null && emailNotifications.isNotEmpty()) {
                val emailText = emailNotifications.take(5).joinToString("\n") { 
                    "From ${it.title}: ${it.text}" 
                }
                return@withContext "Here are your recent emails:\n$emailText"
            }
            
            // If no emails found in notifications, open Gmail
            val gmailIntent = context.packageManager.getLaunchIntentForPackage("com.google.android.gm")
            if (gmailIntent != null) {
                gmailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(gmailIntent)
                
                // Use accessibility service to read screen content
                val accessibilityService = com.mobilebudhdhi.controller.services.MobileBuddhiAccessibilityService.getInstance()
                if (accessibilityService != null) {
                    // Give Gmail time to open
                    kotlinx.coroutines.delay(2000)
                    val screenText = accessibilityService.getAllTextFromScreen()
                    
                    if (screenText.isNotEmpty()) {
                        // Extract email information from screen text
                        val emailPattern = "(.*?)\\s*-\\s*(.*?)".toRegex()
                        val matches = emailPattern.findAll(screenText)
                        
                        if (matches.any()) {
                            val emailSummary = matches.take(5).joinToString("\n") { match ->
                                val (sender, subject) = match.destructured
                                "From $sender: $subject"
                            }
                            return@withContext "Here are your recent emails:\n$emailSummary"
                        }
                    }
                }
                
                "Opening Gmail to check your emails"
            } else {
                "Gmail not found. Please install Gmail to read emails."
            }
        } catch (e: Exception) {
            "Error reading emails: ${e.message}"
        }
    }
    
    suspend fun composeEmail(): String = withContext(Dispatchers.IO) {
        try {
            val composeIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            val chooser = Intent.createChooser(composeIntent, "Choose Email App")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            
            "Opening email composer"
        } catch (e: Exception) {
            "Error opening email composer: ${e.message}"
        }
    }
    
    suspend fun searchEmails(query: String): String = withContext(Dispatchers.IO) {
        try {
            val searchIntent = Intent(Intent.ACTION_SEARCH).apply {
                setPackage("com.google.android.gm")
                putExtra("query", query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            if (searchIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(searchIntent)
                "Searching emails for: $query"
            } else {
                "Gmail not available for search"
            }
        } catch (e: Exception) {
            "Error searching emails: ${e.message}"
        }
    }
    
    suspend fun replyToLastEmail(message: String): String = withContext(Dispatchers.IO) {
        try {
            // This would require more complex integration with email providers
            // For now, we'll open the email app
            val gmailIntent = context.packageManager.getLaunchIntentForPackage("com.google.android.gm")
            if (gmailIntent != null) {
                gmailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(gmailIntent)
                "Opening Gmail. Please manually reply to your last email with: $message"
            } else {
                "Gmail not found"
            }
        } catch (e: Exception) {
            "Error opening Gmail for reply: ${e.message}"
        }
    }
}