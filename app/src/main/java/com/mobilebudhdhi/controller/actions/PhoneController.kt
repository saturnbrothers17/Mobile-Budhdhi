package com.mobilebudhdhi.controller.actions

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telecom.TelecomManager
import android.database.Cursor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PhoneController(private val context: Context) {
    
    suspend fun makeCall(contact: String): String = withContext(Dispatchers.IO) {
        try {
            // First try to wake the device if it's locked
            val systemController = SystemController(context)
            systemController.wakeScreen()
            
            val phoneNumber = resolveContact(contact)
            if (phoneNumber != null) {
                // Try to make the call directly
                val intent = Intent(Intent.ACTION_CALL).apply {
                    data = Uri.parse("tel:$phoneNumber")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                
                // Wait a moment to see if call starts
                kotlinx.coroutines.delay(2000)
                
                // Use accessibility service to check if call is active
                val accessibilityService = com.mobilebudhdhi.controller.services.MobileBuddhiAccessibilityService.getInstance()
                if (accessibilityService != null) {
                    val screenText = accessibilityService.getAllTextFromScreen()
                    if (screenText.contains("calling") || screenText.contains("dialing") || 
                        screenText.contains("connected") || screenText.contains("call")) {
                        return@withContext "Call connected to $contact"
                    }
                }
                
                "Calling $contact"
            } else {
                // If contact not found, try to open dialer with the name
                val dialerIntent = Intent(Intent.ACTION_DIAL)
                dialerIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(dialerIntent)
                
                // Use accessibility service to try to find and call the contact
                kotlinx.coroutines.delay(1500)
                val accessibilityService = com.mobilebudhdhi.controller.services.MobileBuddhiAccessibilityService.getInstance()
                if (accessibilityService != null) {
                    // Try to find search box and type contact name
                    val searchSuccess = accessibilityService.findEditTextAndType(contact)
                    if (searchSuccess) {
                        kotlinx.coroutines.delay(1000)
                        // Try to find and click the contact
                        val contactFound = accessibilityService.findAndClickText(contact)
                        if (contactFound) {
                            kotlinx.coroutines.delay(1000)
                            // Try to find and click call button
                            val callButtonClicked = accessibilityService.findAndClickText("Call") ||
                                                  accessibilityService.findAndClickText("CALL") ||
                                                  accessibilityService.findAndClickText("Dial")
                            
                            if (callButtonClicked) {
                                return@withContext "Found and calling $contact"
                            }
                        }
                    }
                }
                
                "Contact $contact not found. Opening dialer for manual search."
            }
        } catch (e: Exception) {
            "Error making call: ${e.message}"
        }
    }
    
    suspend fun endCall(): String = withContext(Dispatchers.IO) {
        try {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            if (telecomManager.isInCall) {
                telecomManager.endCall()
                "Call ended"
            } else {
                "No active call to end"
            }
        } catch (e: Exception) {
            "Error ending call: ${e.message}"
        }
    }
    
    suspend fun answerCall(): String = withContext(Dispatchers.IO) {
        try {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            if (telecomManager.isRinging) {
                telecomManager.acceptRingingCall()
                "Call answered"
            } else {
                "No incoming call to answer"
            }
        } catch (e: Exception) {
            "Error answering call: ${e.message}"
        }
    }
    
    suspend fun rejectCall(): String = withContext(Dispatchers.IO) {
        try {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            if (telecomManager.isRinging) {
                telecomManager.endCall()
                "Call rejected"
            } else {
                "No incoming call to reject"
            }
        } catch (e: Exception) {
            "Error rejecting call: ${e.message}"
        }
    }
    
    suspend fun getCallHistory(): String = withContext(Dispatchers.IO) {
        try {
            val recentCalls = getRecentCalls(5)
            if (recentCalls.isNotEmpty()) {
                val callText = recentCalls.joinToString("\n") { call ->
                    val type = when (call.type) {
                        1 -> "Incoming"
                        2 -> "Outgoing" 
                        3 -> "Missed"
                        else -> "Unknown"
                    }
                    "$type call ${if (call.type == 2) "to" else "from"} ${call.name}: ${formatDuration(call.duration)}"
                }
                "Here are your recent calls:\n$callText"
            } else {
                "No recent calls found"
            }
        } catch (e: Exception) {
            "Error reading call history: ${e.message}"
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
    
    private fun getRecentCalls(limit: Int): List<CallRecord> {
        val calls = mutableListOf<CallRecord>()
        
        val cursor: Cursor? = context.contentResolver.query(
            android.provider.CallLog.Calls.CONTENT_URI,
            arrayOf(
                android.provider.CallLog.Calls.NUMBER,
                android.provider.CallLog.Calls.CACHED_NAME,
                android.provider.CallLog.Calls.TYPE,
                android.provider.CallLog.Calls.DATE,
                android.provider.CallLog.Calls.DURATION
            ),
            null,
            null,
            "${android.provider.CallLog.Calls.DATE} DESC LIMIT $limit"
        )
        
        cursor?.use {
            while (it.moveToNext()) {
                val number = it.getString(0) ?: "Unknown"
                val name = it.getString(1) ?: number
                val type = it.getInt(2)
                val date = it.getLong(3)
                val duration = it.getLong(4)
                
                calls.add(CallRecord(name, number, type, date, duration))
            }
        }
        
        return calls
    }
    
    private fun formatDuration(seconds: Long): String {
        return if (seconds == 0L) {
            "Not answered"
        } else {
            val minutes = seconds / 60
            val remainingSeconds = seconds % 60
            "${minutes}m ${remainingSeconds}s"
        }
    }
    
    data class CallRecord(
        val name: String,
        val number: String,
        val type: Int,
        val timestamp: Long,
        val duration: Long
    )
}