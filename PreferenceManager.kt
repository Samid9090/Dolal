package com.parentkidsapp.utils

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    
    companion object {
        private const val PREF_NAME = "ParentKidsAppPrefs"
        private const val KEY_PAIRING_TOKEN = "pairing_token"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_IS_PARENT_MODE = "is_parent_mode"
        private const val KEY_IS_KIDS_MODE = "is_kids_mode"
        private const val KEY_PARENT_DEVICE_ID = "parent_device_id"
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_PERMISSIONS_GRANTED = "permissions_granted"
        private const val KEY_MONITORING_ACTIVE = "monitoring_active"
    }
    
    fun setPairingToken(token: String) {
        sharedPreferences.edit().putString(KEY_PAIRING_TOKEN, token).apply()
    }
    
    fun getPairingToken(): String? {
        return sharedPreferences.getString(KEY_PAIRING_TOKEN, null)
    }
    
    fun setDeviceId(deviceId: String) {
        sharedPreferences.edit().putString(KEY_DEVICE_ID, deviceId).apply()
    }
    
    fun getDeviceId(): String? {
        return sharedPreferences.getString(KEY_DEVICE_ID, null)
    }
    
    fun setParentMode(isParent: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_IS_PARENT_MODE, isParent).apply()
    }
    
    fun isParentMode(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_PARENT_MODE, false)
    }
    
    fun setKidsMode(isKids: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_IS_KIDS_MODE, isKids).apply()
    }
    
    fun isKidsMode(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_KIDS_MODE, false)
    }
    
    fun setParentDeviceId(parentDeviceId: String) {
        sharedPreferences.edit().putString(KEY_PARENT_DEVICE_ID, parentDeviceId).apply()
    }
    
    fun getParentDeviceId(): String? {
        return sharedPreferences.getString(KEY_PARENT_DEVICE_ID, null)
    }
    
    fun setFirstLaunch(isFirst: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_FIRST_LAUNCH, isFirst).apply()
    }
    
    fun isFirstLaunch(): Boolean {
        return sharedPreferences.getBoolean(KEY_FIRST_LAUNCH, true)
    }
    
    fun setPermissionsGranted(granted: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_PERMISSIONS_GRANTED, granted).apply()
    }
    
    fun arePermissionsGranted(): Boolean {
        return sharedPreferences.getBoolean(KEY_PERMISSIONS_GRANTED, false)
    }
    
    fun setMonitoringActive(active: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_MONITORING_ACTIVE, active).apply()
    }
    
    fun isMonitoringActive(): Boolean {
        return sharedPreferences.getBoolean(KEY_MONITORING_ACTIVE, false)
    }
    
    fun clearAll() {
        sharedPreferences.edit().clear().apply()
    }
    
    // Additional methods for privacy and performance management
    fun setConsentGiven(consentType: com.parentkidsapp.utils.ConsentType, timestamp: Long) {
        sharedPreferences.edit().putLong("consent_${consentType.name}", timestamp).apply()
    }
    
    fun isConsentGiven(consentType: com.parentkidsapp.utils.ConsentType): Boolean {
        return sharedPreferences.getLong("consent_${consentType.name}", 0L) > 0L
    }
    
    fun getConsentTimestamp(consentType: com.parentkidsapp.utils.ConsentType): Long? {
        val timestamp = sharedPreferences.getLong("consent_${consentType.name}", 0L)
        return if (timestamp > 0L) timestamp else null
    }
    
    fun revokeConsent(consentType: com.parentkidsapp.utils.ConsentType) {
        sharedPreferences.edit().remove("consent_${consentType.name}").apply()
    }
    
    fun logDataAccess(dataType: com.parentkidsapp.utils.DataAccessType, purpose: String, timestamp: Long) {
        val logEntry = "${dataType.name}|$purpose|$timestamp"
        val existingLogs = sharedPreferences.getStringSet("data_access_log", mutableSetOf()) ?: mutableSetOf()
        existingLogs.add(logEntry)
        
        // Keep only last 100 entries
        if (existingLogs.size > 100) {
            val sortedLogs = existingLogs.sortedBy { it.split("|")[2].toLong() }
            existingLogs.clear()
            existingLogs.addAll(sortedLogs.takeLast(100))
        }
        
        sharedPreferences.edit().putStringSet("data_access_log", existingLogs).apply()
    }
    
    fun getDataAccessLog(): List<com.parentkidsapp.utils.DataAccessLog> {
        val logs = sharedPreferences.getStringSet("data_access_log", emptySet()) ?: emptySet()
        return logs.mapNotNull { logEntry ->
            val parts = logEntry.split("|")
            if (parts.size == 3) {
                try {
                    com.parentkidsapp.utils.DataAccessLog(
                        dataType = com.parentkidsapp.utils.DataAccessType.valueOf(parts[0]),
                        purpose = parts[1],
                        timestamp = parts[2].toLong()
                    )
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
        }.sortedByDescending { it.timestamp }
    }
    
    // Performance optimization settings
    fun setLocationUpdateInterval(interval: Long) {
        sharedPreferences.edit().putLong("location_update_interval", interval).apply()
    }
    
    fun getLocationUpdateInterval(): Long {
        return sharedPreferences.getLong("location_update_interval", 60000L)
    }
    
    fun setScreenMirroringEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("screen_mirroring_enabled", enabled).apply()
    }
    
    fun isScreenMirroringEnabled(): Boolean {
        return sharedPreferences.getBoolean("screen_mirroring_enabled", true)
    }
    
    fun setImageQuality(quality: Int) {
        sharedPreferences.edit().putInt("image_quality", quality).apply()
    }
    
    fun getImageQuality(): Int {
        return sharedPreferences.getInt("image_quality", 90)
    }
    
    fun setDataUploadBatchSize(size: Int) {
        sharedPreferences.edit().putInt("data_upload_batch_size", size).apply()
    }
    
    fun getDataUploadBatchSize(): Int {
        return sharedPreferences.getInt("data_upload_batch_size", 1)
    }
}

