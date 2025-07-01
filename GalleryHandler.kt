package com.parentkidsapp.kids

import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import com.parentkidsapp.models.GalleryItem
import com.parentkidsapp.services.FirebaseService
import com.parentkidsapp.utils.PreferenceManager
import java.util.*

class GalleryHandler(
    private val context: Context,
    private val firebaseService: FirebaseService
) {
    
    private var preferenceManager = PreferenceManager(context)
    
    fun getGalleryItems(callback: (Boolean, List<GalleryItem>?) -> Unit) {
        try {
            val galleryItems = mutableListOf<GalleryItem>()
            val deviceId = preferenceManager.getDeviceId() ?: return
            
            // Get images
            val imageItems = getMediaItems(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, deviceId)
            galleryItems.addAll(imageItems)
            
            // Get videos
            val videoItems = getMediaItems(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, deviceId)
            galleryItems.addAll(videoItems)
            
            // Sort by date modified (newest first)
            galleryItems.sortByDescending { it.dateModified }
            
            // Save gallery items to Firebase
            galleryItems.forEach { item ->
                firebaseService.saveGalleryItem(item)
            }
            
            callback(true, galleryItems)
            
        } catch (e: Exception) {
            callback(false, null)
        }
    }
    
    private fun getMediaItems(uri: android.net.Uri, deviceId: String): List<GalleryItem> {
        val items = mutableListOf<GalleryItem>()
        
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.DATE_MODIFIED
        )
        
        val cursor: Cursor? = context.contentResolver.query(
            uri,
            projection,
            null,
            null,
            "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
        )
        
        cursor?.use { c ->
            val idColumn = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameColumn = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val dataColumn = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
            val sizeColumn = c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val mimeTypeColumn = c.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val dateAddedColumn = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val dateModifiedColumn = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
            
            while (c.moveToNext()) {
                val id = c.getLong(idColumn)
                val name = c.getString(nameColumn)
                val data = c.getString(dataColumn)
                val size = c.getLong(sizeColumn)
                val mimeType = c.getString(mimeTypeColumn)
                val dateAdded = c.getLong(dateAddedColumn)
                val dateModified = c.getLong(dateModifiedColumn)
                
                val galleryItem = GalleryItem(
                    id = "${deviceId}_$id",
                    deviceId = deviceId,
                    fileName = name ?: "Unknown",
                    filePath = data ?: "",
                    fileSize = size,
                    mimeType = mimeType ?: "",
                    dateAdded = Date(dateAdded * 1000),
                    dateModified = Date(dateModified * 1000)
                )
                
                items.add(galleryItem)
            }
        }
        
        return items
    }
}

