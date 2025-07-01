package com.parentkidsapp.services

import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.parentkidsapp.models.*
import java.util.*

class FirebaseService {
    
    private lateinit var database: DatabaseReference
    private lateinit var storage: StorageReference
    private var deviceId: String? = null
    
    companion object {
        private const val DEVICES_PATH = "devices"
        private const val COMMANDS_PATH = "commands"
        private const val RESPONSES_PATH = "responses"
        private const val PAIRING_PATH = "pairing"
        private const val CAPTURES_PATH = "captures"
        private const val LOCATIONS_PATH = "locations"
        private const val NOTIFICATIONS_PATH = "notifications"
        private const val GALLERY_PATH = "gallery"
    }
    
    fun initialize(deviceId: String, callback: (Boolean) -> Unit) {
        this.deviceId = deviceId
        
        try {
            database = FirebaseDatabase.getInstance().reference
            storage = FirebaseStorage.getInstance().reference
            
            // Register device
            registerDevice(deviceId) { success ->
                callback(success)
            }
        } catch (e: Exception) {
            callback(false)
        }
    }
    
    private fun registerDevice(deviceId: String, callback: (Boolean) -> Unit) {
        val deviceInfo = DeviceInfo(
            deviceId = deviceId,
            deviceName = android.os.Build.MODEL,
            androidVersion = android.os.Build.VERSION.RELEASE,
            appVersion = "1.0",
            lastSeen = Date(),
            isOnline = true
        )
        
        database.child(DEVICES_PATH).child(deviceId).setValue(deviceInfo)
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }
    
    fun sendCommand(parentDeviceId: String, kidsDeviceId: String, commandType: CommandType, callback: (Boolean, Any?) -> Unit) {
        val commandId = UUID.randomUUID().toString()
        val command = MonitoringCommand(
            id = commandId,
            parentDeviceId = parentDeviceId,
            kidsDeviceId = kidsDeviceId,
            commandType = commandType,
            timestamp = Date()
        )
        
        database.child(COMMANDS_PATH).child(kidsDeviceId).child(commandId).setValue(command)
            .addOnSuccessListener {
                // Listen for response
                listenForCommandResponse(commandId, callback)
            }
            .addOnFailureListener { callback(false, null) }
    }
    
    private fun listenForCommandResponse(commandId: String, callback: (Boolean, Any?) -> Unit) {
        val responseRef = database.child(RESPONSES_PATH).child(commandId)
        
        responseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val response = snapshot.getValue(MonitoringResponse::class.java)
                if (response != null && response.status == CommandStatus.COMPLETED) {
                    callback(true, response.data)
                    responseRef.removeEventListener(this)
                } else if (response != null && response.status == CommandStatus.FAILED) {
                    callback(false, response.error)
                    responseRef.removeEventListener(this)
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                callback(false, error.message)
            }
        })
    }
    
    fun listenForCommands(kidsDeviceId: String, callback: (MonitoringCommand) -> Unit) {
        database.child(COMMANDS_PATH).child(kidsDeviceId)
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val command = snapshot.getValue(MonitoringCommand::class.java)
                    if (command != null && command.status == CommandStatus.PENDING) {
                        callback(command)
                    }
                }
                
                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onChildRemoved(snapshot: DataSnapshot) {}
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {}
            })
    }
    
    fun sendCommandResponse(commandId: String, status: CommandStatus, data: Any? = null, error: String? = null) {
        val response = MonitoringResponse(
            commandId = commandId,
            status = status,
            data = data,
            error = error,
            timestamp = Date()
        )
        
        database.child(RESPONSES_PATH).child(commandId).setValue(response)
    }
    
    fun uploadImage(imageBytes: ByteArray, path: String, callback: (Boolean, String?) -> Unit) {
        val imageRef = storage.child("images/$path/${UUID.randomUUID()}.jpg")
        
        imageRef.putBytes(imageBytes)
            .addOnSuccessListener { taskSnapshot ->
                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    callback(true, uri.toString())
                }.addOnFailureListener {
                    callback(false, null)
                }
            }
            .addOnFailureListener {
                callback(false, null)
            }
    }
    
    fun saveCameraCapture(capture: CameraCapture) {
        database.child(CAPTURES_PATH).child(capture.deviceId).child(capture.id).setValue(capture)
    }
    
    fun saveScreenCapture(capture: ScreenCapture) {
        database.child(CAPTURES_PATH).child(capture.deviceId).child(capture.id).setValue(capture)
    }
    
    fun saveLocationData(location: LocationData, deviceId: String) {
        database.child(LOCATIONS_PATH).child(deviceId).push().setValue(location)
    }
    
    fun saveNotificationData(notification: NotificationData) {
        database.child(NOTIFICATIONS_PATH).child(notification.deviceId).child(notification.id).setValue(notification)
    }
    
    fun saveGalleryItem(item: GalleryItem) {
        database.child(GALLERY_PATH).child(item.deviceId).child(item.id).setValue(item)
    }
    
    fun updateDeviceStatus(deviceId: String, isOnline: Boolean) {
        database.child(DEVICES_PATH).child(deviceId).child("isOnline").setValue(isOnline)
        database.child(DEVICES_PATH).child(deviceId).child("lastSeen").setValue(Date())
    }
}

