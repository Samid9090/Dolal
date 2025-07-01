package com.parentkidsapp.parent

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.Glide
import com.parentkidsapp.R
import com.parentkidsapp.models.CommandType
import com.parentkidsapp.services.FirebaseService
import com.parentkidsapp.utils.PreferenceManager
import java.util.UUID

class ParentModeActivity : AppCompatActivity() {

    private lateinit var preferenceManager: PreferenceManager
    private lateinit var firebaseService: FirebaseService
    
    private lateinit var connectionStatusTextView: TextView
    private lateinit var generateLinkButton: Button
    private lateinit var captureFrontCameraButton: Button
    private lateinit var captureBackCameraButton: Button
    private lateinit var startScreenMirrorButton: Button
    private lateinit var captureScreenshotButton: Button
    private lateinit var viewGalleryButton: Button
    private lateinit var trackLocationButton: Button
    private lateinit var viewNotificationsButton: Button
    private lateinit var capturedImageView: ImageView
    private lateinit var screenshotImageView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parent_mode)

        preferenceManager = PreferenceManager(this)
        firebaseService = FirebaseService()

        // Set parent mode
        preferenceManager.setParentMode(true)
        preferenceManager.setKidsMode(false)

        setupToolbar()
        setupViews()
        setupClickListeners()
        
        // Initialize Firebase and check connection
        initializeFirebase()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    private fun setupViews() {
        connectionStatusTextView = findViewById(R.id.connectionStatusTextView)
        generateLinkButton = findViewById(R.id.generateLinkButton)
        captureFrontCameraButton = findViewById(R.id.captureFrontCameraButton)
        captureBackCameraButton = findViewById(R.id.captureBackCameraButton)
        startScreenMirrorButton = findViewById(R.id.startScreenMirrorButton)
        captureScreenshotButton = findViewById(R.id.captureScreenshotButton)
        viewGalleryButton = findViewById(R.id.viewGalleryButton)
        trackLocationButton = findViewById(R.id.trackLocationButton)
        viewNotificationsButton = findViewById(R.id.viewNotificationsButton)
        capturedImageView = findViewById(R.id.capturedImageView)
        screenshotImageView = findViewById(R.id.screenshotImageView)
    }

    private fun setupClickListeners() {
        generateLinkButton.setOnClickListener {
            generatePairingLink()
        }

        captureFrontCameraButton.setOnClickListener {
            sendCommand(CommandType.CAPTURE_FRONT_CAMERA)
        }

        captureBackCameraButton.setOnClickListener {
            sendCommand(CommandType.CAPTURE_BACK_CAMERA)
        }

        startScreenMirrorButton.setOnClickListener {
            sendCommand(CommandType.START_SCREEN_MIRROR)
        }

        captureScreenshotButton.setOnClickListener {
            sendCommand(CommandType.CAPTURE_SCREENSHOT)
        }

        viewGalleryButton.setOnClickListener {
            sendCommand(CommandType.GET_GALLERY)
        }

        trackLocationButton.setOnClickListener {
            sendCommand(CommandType.GET_LOCATION)
        }

        viewNotificationsButton.setOnClickListener {
            sendCommand(CommandType.GET_NOTIFICATIONS)
        }
    }

    private fun initializeFirebase() {
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
                    connectionStatusTextView.text = "Connected"
                    connectionStatusTextView.setTextColor(getColor(R.color.success_color))
                } else {
                    connectionStatusTextView.text = "Connection failed"
                    connectionStatusTextView.setTextColor(getColor(R.color.error_color))
                }
            }
        }
    }

    private fun generatePairingLink() {
        val deviceId = preferenceManager.getDeviceId() ?: return
        val pairingToken = UUID.randomUUID().toString()
        
        // Store pairing token
        preferenceManager.setPairingToken(pairingToken)
        
        // Create pairing link
        val pairingLink = "parentkidsapp://kids?token=$pairingToken&parent=$deviceId"
        
        // Copy to clipboard
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Pairing Link", pairingLink)
        clipboard.setPrimaryClip(clip)
        
        Toast.makeText(this, "Pairing link copied to clipboard", Toast.LENGTH_LONG).show()
        
        // Also show the link in a dialog or share intent
        shareLink(pairingLink)
    }

    private fun shareLink(link: String) {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "Install and open this link on your child's device: $link")
            type = "text/plain"
        }
        startActivity(Intent.createChooser(shareIntent, "Share pairing link"))
    }

    private fun sendCommand(commandType: CommandType) {
        val kidsDeviceId = preferenceManager.getParentDeviceId()
        if (kidsDeviceId == null) {
            Toast.makeText(this, "No kids device connected", Toast.LENGTH_SHORT).show()
            return
        }

        val parentDeviceId = preferenceManager.getDeviceId() ?: return
        
        firebaseService.sendCommand(parentDeviceId, kidsDeviceId, commandType) { success, response ->
            runOnUiThread {
                if (success && response != null) {
                    handleCommandResponse(commandType, response)
                } else {
                    Toast.makeText(this, "Command failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun handleCommandResponse(commandType: CommandType, response: Any) {
        when (commandType) {
            CommandType.CAPTURE_FRONT_CAMERA, CommandType.CAPTURE_BACK_CAMERA -> {
                if (response is String) {
                    // Load image from URL
                    capturedImageView.visibility = ImageView.VISIBLE
                    Glide.with(this)
                        .load(response)
                        .into(capturedImageView)
                }
            }
            CommandType.CAPTURE_SCREENSHOT -> {
                if (response is String) {
                    // Load screenshot from URL
                    screenshotImageView.visibility = ImageView.VISIBLE
                    Glide.with(this)
                        .load(response)
                        .into(screenshotImageView)
                }
            }
            CommandType.GET_LOCATION -> {
                // Handle location data
                Toast.makeText(this, "Location received", Toast.LENGTH_SHORT).show()
            }
            CommandType.GET_GALLERY -> {
                // Handle gallery data
                Toast.makeText(this, "Gallery data received", Toast.LENGTH_SHORT).show()
            }
            CommandType.GET_NOTIFICATIONS -> {
                // Handle notifications data
                Toast.makeText(this, "Notifications received", Toast.LENGTH_SHORT).show()
            }
            else -> {
                Toast.makeText(this, "Command completed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}

