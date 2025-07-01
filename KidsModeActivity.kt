package com.parentkidsapp.kids

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.parentkidsapp.R
import com.parentkidsapp.services.FirebaseService
import com.parentkidsapp.utils.PreferenceManager
import java.util.UUID

class KidsModeActivity : AppCompatActivity() {

    private lateinit var preferenceManager: PreferenceManager
    private lateinit var firebaseService: FirebaseService
    
    private lateinit var connectionStatusTextView: TextView
    private lateinit var permissionsStatusTextView: TextView
    private lateinit var grantPermissionsButton: Button
    private lateinit var progressBar: ProgressBar
    
    private var fromDeepLink = false
    
    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
        private const val DEVICE_ADMIN_REQUEST_CODE = 1002
        private const val OVERLAY_PERMISSION_REQUEST_CODE = 1003
        private const val NOTIFICATION_ACCESS_REQUEST_CODE = 1004
        private const val USAGE_STATS_REQUEST_CODE = 1005
        
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kids_mode)

        preferenceManager = PreferenceManager(this)
        firebaseService = FirebaseService()
        
        fromDeepLink = intent.getBooleanExtra("from_deep_link", false)

        // Set kids mode
        preferenceManager.setKidsMode(true)
        preferenceManager.setParentMode(false)

        setupViews()
        setupClickListeners()
        
        if (fromDeepLink) {
            handlePairingFromDeepLink()
        } else {
            checkSetupStatus()
        }
    }

    private fun setupViews() {
        connectionStatusTextView = findViewById(R.id.connectionStatusTextView)
        permissionsStatusTextView = findViewById(R.id.permissionsStatusTextView)
        grantPermissionsButton = findViewById(R.id.grantPermissionsButton)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupClickListeners() {
        grantPermissionsButton.setOnClickListener {
            requestAllPermissions()
        }
    }

    private fun handlePairingFromDeepLink() {
        val pairingToken = preferenceManager.getPairingToken()
        if (pairingToken != null) {
            connectionStatusTextView.text = "Pairing with parent device..."
            connectionStatusTextView.setTextColor(getColor(R.color.warning_color))
            
            // Initialize Firebase and pair with parent
            initializeFirebaseAndPair(pairingToken)
        } else {
            connectionStatusTextView.text = "Invalid pairing token"
            connectionStatusTextView.setTextColor(getColor(R.color.error_color))
        }
    }

    private fun checkSetupStatus() {
        if (preferenceManager.arePermissionsGranted() && preferenceManager.isMonitoringActive()) {
            // Already set up, start monitoring
            startMonitoringMode()
        } else {
            // Need setup
            showSetupUI()
        }
    }

    private fun showSetupUI() {
        grantPermissionsButton.visibility = Button.VISIBLE
        findViewById<TextView>(R.id.permissionsExplanationTextView).visibility = TextView.VISIBLE
        
        updatePermissionStatus()
    }

    private fun updatePermissionStatus() {
        val allPermissionsGranted = checkAllPermissions()
        
        if (allPermissionsGranted) {
            permissionsStatusTextView.text = "All permissions granted"
            permissionsStatusTextView.setTextColor(getColor(R.color.success_color))
            grantPermissionsButton.text = "Complete Setup"
        } else {
            permissionsStatusTextView.text = "Permissions needed"
            permissionsStatusTextView.setTextColor(getColor(R.color.warning_color))
            grantPermissionsButton.text = getString(R.string.grant_permissions)
        }
    }

    private fun checkAllPermissions(): Boolean {
        // Check basic permissions
        for (permission in REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        
        // Check special permissions
        if (!Settings.canDrawOverlays(this)) return false
        if (!isNotificationAccessGranted()) return false
        if (!isUsageStatsPermissionGranted()) return false
        if (!isDeviceAdminActive()) return false
        
        return true
    }

    private fun requestAllPermissions() {
        if (!checkAllPermissions()) {
            requestBasicPermissions()
        } else {
            completeSetup()
        }
    }

    private fun requestBasicPermissions() {
        val permissionsToRequest = REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest, PERMISSION_REQUEST_CODE)
        } else {
            requestSpecialPermissions()
        }
    }

    private fun requestSpecialPermissions() {
        // Request overlay permission
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            intent.data = Uri.parse("package:$packageName")
            startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
            return
        }
        
        // Request notification access
        if (!isNotificationAccessGranted()) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            startActivityForResult(intent, NOTIFICATION_ACCESS_REQUEST_CODE)
            return
        }
        
        // Request usage stats permission
        if (!isUsageStatsPermissionGranted()) {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            startActivityForResult(intent, USAGE_STATS_REQUEST_CODE)
            return
        }
        
        // Request device admin
        if (!isDeviceAdminActive()) {
            requestDeviceAdmin()
            return
        }
        
        completeSetup()
    }

    private fun requestDeviceAdmin() {
        val devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val componentName = ComponentName(this, DeviceAdminReceiver::class.java)
        
        if (!devicePolicyManager.isAdminActive(componentName)) {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Device admin is required for security features")
            startActivityForResult(intent, DEVICE_ADMIN_REQUEST_CODE)
        }
    }

    private fun isNotificationAccessGranted(): Boolean {
        val enabledListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return enabledListeners?.contains(packageName) == true
    }

    private fun isUsageStatsPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val appOps = getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
            val mode = appOps.checkOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
            mode == android.app.AppOpsManager.MODE_ALLOWED
        } else {
            true
        }
    }

    private fun isDeviceAdminActive(): Boolean {
        val devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val componentName = ComponentName(this, DeviceAdminReceiver::class.java)
        return devicePolicyManager.isAdminActive(componentName)
    }

    private fun initializeFirebaseAndPair(pairingToken: String) {
        // Generate device ID if not exists
        var deviceId = preferenceManager.getDeviceId()
        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString()
            preferenceManager.setDeviceId(deviceId)
        }

        // Initialize Firebase service
        firebaseService.initialize(deviceId) { success ->
            runOnUiThread {
                if (success) {
                    connectionStatusTextView.text = "Connected to parent"
                    connectionStatusTextView.setTextColor(getColor(R.color.success_color))
                    
                    // Start monitoring if permissions are granted
                    if (checkAllPermissions()) {
                        startMonitoringMode()
                    } else {
                        showSetupUI()
                    }
                } else {
                    connectionStatusTextView.text = "Connection failed"
                    connectionStatusTextView.setTextColor(getColor(R.color.error_color))
                }
            }
        }
    }

    private fun completeSetup() {
        if (checkAllPermissions()) {
            preferenceManager.setPermissionsGranted(true)
            preferenceManager.setFirstLaunch(false)
            
            Toast.makeText(this, getString(R.string.success_setup_complete), Toast.LENGTH_SHORT).show()
            
            startMonitoringMode()
        } else {
            Toast.makeText(this, "Please grant all required permissions", Toast.LENGTH_SHORT).show()
            updatePermissionStatus()
        }
    }

    private fun startMonitoringMode() {
        preferenceManager.setMonitoringActive(true)
        
        // Start monitoring service
        val serviceIntent = Intent(this, MonitoringService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        
        // Hide setup UI
        grantPermissionsButton.visibility = Button.GONE
        findViewById<TextView>(R.id.permissionsExplanationTextView).visibility = TextView.GONE
        progressBar.visibility = ProgressBar.GONE
        
        connectionStatusTextView.text = "Monitoring active"
        connectionStatusTextView.setTextColor(getColor(R.color.success_color))
        permissionsStatusTextView.text = "All systems ready"
        permissionsStatusTextView.setTextColor(getColor(R.color.success_color))
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            requestSpecialPermissions()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        when (requestCode) {
            OVERLAY_PERMISSION_REQUEST_CODE,
            NOTIFICATION_ACCESS_REQUEST_CODE,
            USAGE_STATS_REQUEST_CODE,
            DEVICE_ADMIN_REQUEST_CODE -> {
                requestSpecialPermissions()
            }
        }
    }
}

