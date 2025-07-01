package com.parentkidsapp.kids

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.parentkidsapp.models.NotificationData
import com.parentkidsapp.services.FirebaseService
import com.parentkidsapp.utils.PreferenceManager
import java.util.*

class NotificationListenerService : NotificationListenerService() {
    
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var firebaseService: FirebaseService
    
    override fun onCreate() {
        super.onCreate()
        preferenceManager = PreferenceManager(this)
        firebaseService = FirebaseService()
        
        // Initialize Firebase
        val deviceId = preferenceManager.getDeviceId()
        if (deviceId != null) {
            firebaseService.initialize(deviceId) { success ->
                // Firebase initialized
            }
        }
    }
    
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        
        if (sbn == null || !preferenceManager.isMonitoringActive()) {
            return
        }
        
        // Filter out our own notifications
        if (sbn.packageName == packageName) {
            return
        }
        
        try {
            val notification = sbn.notification
            val extras = notification.extras
            
            val title = extras.getCharSequence("android.title")?.toString()
            val text = extras.getCharSequence("android.text")?.toString()
            val appName = getApplicationLabel(sbn.packageName)
            
            val notificationData = NotificationData(
                id = "${sbn.packageName}_${sbn.id}_${System.currentTimeMillis()}",
                deviceId = preferenceManager.getDeviceId() ?: return,
                packageName = sbn.packageName,
                appName = appName,
                title = title,
                text = text,
                timestamp = Date(sbn.postTime),
                isOngoing = notification.flags and android.app.Notification.FLAG_ONGOING_EVENT != 0
            )
            
            // Save notification to Firebase
            firebaseService.saveNotificationData(notificationData)
            
        } catch (e: Exception) {
            // Handle error silently
        }
    }
    
    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        // Handle notification removal if needed
    }
    
    private fun getApplicationLabel(packageName: String): String {
        return try {
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(applicationInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }
}

