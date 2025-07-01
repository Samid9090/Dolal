package com.parentkidsapp.models

import java.util.Date

data class DeviceInfo(
    val deviceId: String,
    val deviceName: String,
    val androidVersion: String,
    val appVersion: String,
    val lastSeen: Date,
    val isOnline: Boolean = false
)

data class CameraCapture(
    val id: String,
    val deviceId: String,
    val cameraType: CameraType,
    val imageUrl: String,
    val timestamp: Date,
    val location: LocationData? = null
)

enum class CameraType {
    FRONT, BACK
}

data class ScreenCapture(
    val id: String,
    val deviceId: String,
    val imageUrl: String,
    val timestamp: Date,
    val screenWidth: Int,
    val screenHeight: Int
)

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val timestamp: Date,
    val address: String? = null
)

data class NotificationData(
    val id: String,
    val deviceId: String,
    val packageName: String,
    val appName: String,
    val title: String?,
    val text: String?,
    val timestamp: Date,
    val isOngoing: Boolean = false
)

data class GalleryItem(
    val id: String,
    val deviceId: String,
    val fileName: String,
    val filePath: String,
    val fileSize: Long,
    val mimeType: String,
    val dateAdded: Date,
    val dateModified: Date,
    val thumbnailUrl: String? = null,
    val fullImageUrl: String? = null
)

data class PairingRequest(
    val parentDeviceId: String,
    val kidsDeviceId: String,
    val pairingToken: String,
    val timestamp: Date,
    val status: PairingStatus = PairingStatus.PENDING
)

enum class PairingStatus {
    PENDING, ACCEPTED, REJECTED, EXPIRED
}

data class MonitoringCommand(
    val id: String,
    val parentDeviceId: String,
    val kidsDeviceId: String,
    val commandType: CommandType,
    val parameters: Map<String, Any> = emptyMap(),
    val timestamp: Date,
    val status: CommandStatus = CommandStatus.PENDING
)

enum class CommandType {
    CAPTURE_FRONT_CAMERA,
    CAPTURE_BACK_CAMERA,
    CAPTURE_SCREENSHOT,
    START_SCREEN_MIRROR,
    STOP_SCREEN_MIRROR,
    GET_LOCATION,
    GET_GALLERY,
    GET_NOTIFICATIONS
}

enum class CommandStatus {
    PENDING, EXECUTING, COMPLETED, FAILED
}

data class MonitoringResponse(
    val commandId: String,
    val status: CommandStatus,
    val data: Any? = null,
    val error: String? = null,
    val timestamp: Date
)

