package com.mobilebudhdhi.controller

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.mobilebudhdhi.controller.services.VoiceControlService
import com.mobilebudhdhi.controller.utils.PermissionManager

class MainActivity : AppCompatActivity() {
    
    private lateinit var statusText: TextView
    private lateinit var startButton: Button
    private lateinit var permissionsButton: Button
    private lateinit var accessibilityButton: Button
    
    private val permissionManager = PermissionManager()
    
    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
        private const val ACCESSIBILITY_REQUEST_CODE = 1002
        private const val OVERLAY_PERMISSION_REQUEST_CODE = 1003
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initializeViews()
        setupClickListeners()
        updateStatus()
    }
    
    private fun initializeViews() {
        statusText = findViewById(R.id.statusText)
        startButton = findViewById(R.id.startButton)
        permissionsButton = findViewById(R.id.permissionsButton)
        accessibilityButton = findViewById(R.id.accessibilityButton)
    }
    
    private fun setupClickListeners() {
        startButton.setOnClickListener {
            if (allPermissionsGranted()) {
                startVoiceService()
            } else {
                Toast.makeText(this, "Please grant all permissions first", Toast.LENGTH_SHORT).show()
            }
        }
        
        permissionsButton.setOnClickListener {
            requestAllPermissions()
        }
        
        accessibilityButton.setOnClickListener {
            openAccessibilitySettings()
        }
    }
    
    private fun startVoiceService() {
        val intent = Intent(this, VoiceControlService::class.java)
        startForegroundService(intent)
        
        // Save service state for auto-restart on boot
        val prefs = getSharedPreferences("MobileBudhdhi", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("service_enabled", true).apply()
        
        statusText.text = "Mobile Budhdhi is now active and listening..."
        startButton.text = "Service Running"
        startButton.isEnabled = false
    }
    
    private fun requestAllPermissions() {
        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.MODIFY_AUDIO_SETTINGS,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.FOREGROUND_SERVICE
        )
        
        // Add notification permission for Android 13+
        val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions + Manifest.permission.POST_NOTIFICATIONS
        } else {
            permissions
        }
        
        ActivityCompat.requestPermissions(this, permissionsToRequest, PERMISSION_REQUEST_CODE)
        
        // Request overlay permission separately as it requires a different flow
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName"))
            startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
        }
    }
    
    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivityForResult(intent, ACCESSIBILITY_REQUEST_CODE)
    }
    
    private fun allPermissionsGranted(): Boolean {
        val requiredPermissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_CONTACTS
        )
        
        return requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    private fun updateStatus() {
        when {
            !allPermissionsGranted() -> {
                statusText.text = "Please grant all required permissions"
                startButton.isEnabled = false
            }
            !permissionManager.isAccessibilityServiceEnabled(this) -> {
                statusText.text = "Please enable Accessibility Service"
                startButton.isEnabled = false
            }
            else -> {
                statusText.text = "Ready to start Mobile Budhdhi"
                startButton.isEnabled = true
            }
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            updateStatus()
        }
    }
    
    override fun onResume() {
        super.onResume()
        updateStatus()
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        when (requestCode) {
            ACCESSIBILITY_REQUEST_CODE -> {
                // Check if accessibility service is now enabled
                if (permissionManager.isAccessibilityServiceEnabled(this)) {
                    Toast.makeText(this, "Accessibility service enabled", Toast.LENGTH_SHORT).show()
                }
                updateStatus()
            }
            OVERLAY_PERMISSION_REQUEST_CODE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (Settings.canDrawOverlays(this)) {
                        Toast.makeText(this, "Overlay permission granted", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Overlay permission is required for full functionality", Toast.LENGTH_LONG).show()
                    }
                }
                updateStatus()
            }
        }
    }
}