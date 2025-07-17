package com.mobilebudhdhi.controller.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.*

class MobileBuddhiAccessibilityService : AccessibilityService() {
    
    private var serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    companion object {
        private var instance: MobileBuddhiAccessibilityService? = null
        
        fun getInstance(): MobileBuddhiAccessibilityService? = instance
        
        fun isServiceRunning(): Boolean = instance != null
    }
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.let { accessibilityEvent ->
            when (accessibilityEvent.eventType) {
                AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> {
                    handleNotification(accessibilityEvent)
                }
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                    handleWindowChange(accessibilityEvent)
                }
                AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                    handleViewClick(accessibilityEvent)
                }
            }
        }
    }
    
    override fun onInterrupt() {
        // Handle service interruption
    }
    
    private fun handleNotification(event: AccessibilityEvent) {
        serviceScope.launch {
            try {
                val packageName = event.packageName?.toString()
                val text = event.text?.joinToString(" ") ?: ""
                
                // Handle WhatsApp notifications
                if (packageName == "com.whatsapp" && text.isNotEmpty()) {
                    // Could announce new WhatsApp messages
                    announceNotification("New WhatsApp message: $text")
                }
                
                // Handle SMS notifications
                if (packageName == "com.google.android.apps.messaging" && text.isNotEmpty()) {
                    announceNotification("New text message: $text")
                }
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }
    
    private fun handleWindowChange(event: AccessibilityEvent) {
        // Track app changes for better context awareness
        val packageName = event.packageName?.toString()
        // Could be used to provide context-aware commands
    }
    
    private fun handleViewClick(event: AccessibilityEvent) {
        // Track user interactions for learning patterns
    }
    
    private fun announceNotification(message: String) {
        // This could integrate with the voice service to announce notifications
        val intent = Intent("com.mobilebudhdhi.ANNOUNCE_NOTIFICATION")
        intent.putExtra("message", message)
        sendBroadcast(intent)
    }
    
    // Public methods for external control
    fun performClick(x: Float, y: Float): Boolean {
        val path = Path()
        path.moveTo(x, y)
        
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()
        
        return dispatchGesture(gesture, null, null)
    }
    
    fun performSwipe(startX: Float, startY: Float, endX: Float, endY: Float): Boolean {
        val path = Path()
        path.moveTo(startX, startY)
        path.lineTo(endX, endY)
        
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 500))
            .build()
        
        return dispatchGesture(gesture, null, null)
    }
    
    fun performBack(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_BACK)
    }
    
    fun performHome(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_HOME)
    }
    
    fun performRecents(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_RECENTS)
    }
    
    fun performNotifications(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
    }
    
    fun performQuickSettings(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
    }
    
    fun findAndClickText(text: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        return findAndClickNodeWithText(rootNode, text)
    }
    
    private fun findAndClickNodeWithText(node: AccessibilityNodeInfo, text: String): Boolean {
        // Check current node
        if (node.text?.toString()?.contains(text, ignoreCase = true) == true ||
            node.contentDescription?.toString()?.contains(text, ignoreCase = true) == true) {
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
        
        // Check child nodes
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null && findAndClickNodeWithText(child, text)) {
                child.recycle()
                return true
            }
            child?.recycle()
        }
        
        return false
    }
    
    fun findEditTextAndType(text: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        return findEditTextAndSetText(rootNode, text)
    }
    
    private fun findEditTextAndSetText(node: AccessibilityNodeInfo, text: String): Boolean {
        if (node.className == "android.widget.EditText" && node.isEditable) {
            val arguments = android.os.Bundle()
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null && findEditTextAndSetText(child, text)) {
                child.recycle()
                return true
            }
            child?.recycle()
        }
        
        return false
    }
    
    fun getAllTextFromScreen(): String {
        val rootNode = rootInActiveWindow ?: return ""
        val textBuilder = StringBuilder()
        extractTextFromNode(rootNode, textBuilder)
        return textBuilder.toString()
    }
    
    fun getStructuredScreenContent(): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val rootNode = rootInActiveWindow ?: return result
        
        // Get package name of current app
        val packageName = rootNode.packageName?.toString() ?: "unknown"
        result["current_app"] = packageName
        
        // Get window title if available
        rootNode.findAccessibilityNodeInfosByViewId("android:id/title")?.firstOrNull()?.let {
            result["window_title"] = it.text?.toString() ?: ""
        }
        
        // Extract main content
        val textBuilder = StringBuilder()
        extractTextFromNode(rootNode, textBuilder)
        result["screen_text"] = textBuilder.toString()
        
        // Extract specific elements based on app
        when (packageName) {
            "com.whatsapp" -> {
                extractWhatsAppContent(rootNode, result)
            }
            "com.google.android.gm" -> {
                extractGmailContent(rootNode, result)
            }
            "com.google.android.apps.messaging" -> {
                extractMessagingContent(rootNode, result)
            }
        }
        
        return result
    }
    
    private fun extractWhatsAppContent(rootNode: AccessibilityNodeInfo, result: MutableMap<String, String>) {
        // Try to find message content
        val messages = StringBuilder()
        val messageNodes = findNodesWithText(rootNode, "message")
        messageNodes.forEach { node ->
            node.text?.toString()?.let { text ->
                messages.append(text).append("\n")
            }
        }
        if (messages.isNotEmpty()) {
            result["whatsapp_messages"] = messages.toString()
        }
        
        // Try to find contact name
        rootNode.findAccessibilityNodeInfosByViewId("com.whatsapp:id/conversation_contact_name")?.firstOrNull()?.let {
            result["contact_name"] = it.text?.toString() ?: ""
        }
    }
    
    private fun extractGmailContent(rootNode: AccessibilityNodeInfo, result: MutableMap<String, String>) {
        // Try to find email subject
        rootNode.findAccessibilityNodeInfosByViewId("com.google.android.gm:id/subject")?.firstOrNull()?.let {
            result["email_subject"] = it.text?.toString() ?: ""
        }
        
        // Try to find email body
        rootNode.findAccessibilityNodeInfosByViewId("com.google.android.gm:id/body")?.firstOrNull()?.let {
            result["email_body"] = it.text?.toString() ?: ""
        }
    }
    
    private fun extractMessagingContent(rootNode: AccessibilityNodeInfo, result: MutableMap<String, String>) {
        // Try to find message content
        val messages = StringBuilder()
        val messageNodes = findNodesWithText(rootNode, "message")
        messageNodes.forEach { node ->
            node.text?.toString()?.let { text ->
                messages.append(text).append("\n")
            }
        }
        if (messages.isNotEmpty()) {
            result["sms_messages"] = messages.toString()
        }
    }
    
    private fun findNodesWithText(node: AccessibilityNodeInfo, textToFind: String): List<AccessibilityNodeInfo> {
        val results = mutableListOf<AccessibilityNodeInfo>()
        
        // Check current node
        node.text?.toString()?.let { text ->
            if (text.contains(textToFind, ignoreCase = true)) {
                results.add(node)
            }
        }
        
        // Check child nodes
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            results.addAll(findNodesWithText(child, textToFind))
        }
        
        return results
    }
    
    private fun extractTextFromNode(node: AccessibilityNodeInfo, textBuilder: StringBuilder) {
        node.text?.let { text ->
            if (text.isNotEmpty()) {
                textBuilder.append(text).append(" ")
            }
        }
        
        node.contentDescription?.let { desc ->
            if (desc.isNotEmpty()) {
                textBuilder.append(desc).append(" ")
            }
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            child?.let {
                extractTextFromNode(it, textBuilder)
                it.recycle()
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        instance = null
        serviceScope.cancel()
    }
}