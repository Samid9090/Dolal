package com.parentkidsapp.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.util.*

class PrivacyManager(private val context: Context) {
    
    companion object {
        private const val PRIVACY_POLICY_URL = "https://parentkidsapp.com/privacy"
        private const val TERMS_OF_SERVICE_URL = "https://parentkidsapp.com/terms"
        private const val COPPA_COMPLIANCE_URL = "https://parentkidsapp.com/coppa"
        private const val GDPR_COMPLIANCE_URL = "https://parentkidsapp.com/gdpr"
    }
    
    private val preferenceManager = PreferenceManager(context)
    
    fun recordConsentGiven(consentType: ConsentType) {
        val timestamp = Date().time
        preferenceManager.setConsentGiven(consentType, timestamp)
    }
    
    fun isConsentGiven(consentType: ConsentType): Boolean {
        return preferenceManager.isConsentGiven(consentType)
    }
    
    fun getConsentTimestamp(consentType: ConsentType): Long? {
        return preferenceManager.getConsentTimestamp(consentType)
    }
    
    fun revokeConsent(consentType: ConsentType) {
        preferenceManager.revokeConsent(consentType)
    }
    
    fun openPrivacyPolicy() {
        openUrl(PRIVACY_POLICY_URL)
    }
    
    fun openTermsOfService() {
        openUrl(TERMS_OF_SERVICE_URL)
    }
    
    fun openCoppaCompliance() {
        openUrl(COPPA_COMPLIANCE_URL)
    }
    
    fun openGdprCompliance() {
        openUrl(GDPR_COMPLIANCE_URL)
    }
    
    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }
    
    fun getDataCollectionSummary(): DataCollectionSummary {
        return DataCollectionSummary(
            cameraAccess = "Camera is used for safety monitoring and environmental checks",
            locationTracking = "Location is tracked for safety and emergency purposes",
            notificationAccess = "Notifications are monitored to ensure appropriate content",
            galleryAccess = "Gallery is accessed to monitor media content for safety",
            screenMonitoring = "Screen activity is monitored for safety and appropriate usage",
            dataRetention = "Data is retained for 30 days and then automatically deleted",
            dataSharing = "Data is only shared between paired parent and child devices",
            encryption = "All data is encrypted in transit and at rest using AES-256 encryption"
        )
    }
    
    fun generateDataExportRequest(): String {
        val deviceId = preferenceManager.getDeviceId() ?: "unknown"
        val timestamp = Date()
        
        return """
            Data Export Request
            ===================
            
            Device ID: $deviceId
            Request Date: $timestamp
            
            Data Categories:
            - Camera captures
            - Location history
            - Notification logs
            - Gallery metadata
            - Screen capture logs
            
            To complete your data export request, please contact support@parentkidsapp.com
            with this request ID: ${generateRequestId()}
            
            Your data will be provided within 30 days as required by GDPR.
        """.trimIndent()
    }
    
    fun generateDataDeletionRequest(): String {
        val deviceId = preferenceManager.getDeviceId() ?: "unknown"
        val timestamp = Date()
        
        return """
            Data Deletion Request
            ====================
            
            Device ID: $deviceId
            Request Date: $timestamp
            
            This request will permanently delete all data associated with this device:
            - Camera captures
            - Location history
            - Notification logs
            - Gallery metadata
            - Screen capture logs
            
            To complete your data deletion request, please contact support@parentkidsapp.com
            with this request ID: ${generateRequestId()}
            
            Your data will be deleted within 30 days as required by GDPR.
        """.trimIndent()
    }
    
    private fun generateRequestId(): String {
        return "REQ_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
    
    fun logDataAccess(dataType: DataAccessType, purpose: String) {
        val timestamp = Date().time
        preferenceManager.logDataAccess(dataType, purpose, timestamp)
    }
    
    fun getDataAccessLog(): List<DataAccessLog> {
        return preferenceManager.getDataAccessLog()
    }
}

enum class ConsentType {
    CAMERA_ACCESS,
    LOCATION_TRACKING,
    NOTIFICATION_ACCESS,
    GALLERY_ACCESS,
    SCREEN_MONITORING,
    DATA_COLLECTION,
    PARENTAL_MONITORING
}

enum class DataAccessType {
    CAMERA_CAPTURE,
    LOCATION_READ,
    NOTIFICATION_READ,
    GALLERY_READ,
    SCREEN_CAPTURE,
    DATA_UPLOAD,
    DATA_DOWNLOAD
}

data class DataCollectionSummary(
    val cameraAccess: String,
    val locationTracking: String,
    val notificationAccess: String,
    val galleryAccess: String,
    val screenMonitoring: String,
    val dataRetention: String,
    val dataSharing: String,
    val encryption: String
)

data class DataAccessLog(
    val dataType: DataAccessType,
    val purpose: String,
    val timestamp: Long
)

