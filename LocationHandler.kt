package com.parentkidsapp.kids

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.parentkidsapp.models.LocationData
import com.parentkidsapp.services.FirebaseService
import com.parentkidsapp.utils.PreferenceManager
import java.util.*

class LocationHandler(
    private val context: Context,
    private val firebaseService: FirebaseService
) {
    
    private var fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private var locationCallback: LocationCallback? = null
    private var preferenceManager = PreferenceManager(context)
    private var geocoder: Geocoder = Geocoder(context, Locale.getDefault())
    
    companion object {
        private const val LOCATION_UPDATE_INTERVAL = 60000L // 1 minute
        private const val LOCATION_FASTEST_INTERVAL = 30000L // 30 seconds
    }
    
    fun startLocationUpdates() {
        if (!hasLocationPermission()) {
            return
        }
        
        val locationRequest = LocationRequest.create().apply {
            interval = LOCATION_UPDATE_INTERVAL
            fastestInterval = LOCATION_FASTEST_INTERVAL
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    processLocation(location)
                }
            }
        }
        
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            // Handle permission error
        }
    }
    
    fun stopLocationUpdates() {
        locationCallback?.let { callback ->
            fusedLocationClient.removeLocationUpdates(callback)
        }
    }
    
    fun getCurrentLocation(callback: (Boolean, LocationData?) -> Unit) {
        if (!hasLocationPermission()) {
            callback(false, null)
            return
        }
        
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val locationData = createLocationData(location)
                    callback(true, locationData)
                } else {
                    // Request fresh location
                    requestFreshLocation(callback)
                }
            }.addOnFailureListener {
                callback(false, null)
            }
        } catch (e: SecurityException) {
            callback(false, null)
        }
    }
    
    private fun requestFreshLocation(callback: (Boolean, LocationData?) -> Unit) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            numUpdates = 1
        }
        
        val freshLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    val locationData = createLocationData(location)
                    callback(true, locationData)
                } ?: callback(false, null)
            }
        }
        
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                freshLocationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            callback(false, null)
        }
    }
    
    private fun processLocation(location: Location) {
        val locationData = createLocationData(location)
        val deviceId = preferenceManager.getDeviceId() ?: return
        
        // Save location to Firebase
        firebaseService.saveLocationData(locationData, deviceId)
    }
    
    private fun createLocationData(location: Location): LocationData {
        val address = getAddressFromLocation(location.latitude, location.longitude)
        
        return LocationData(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracy = location.accuracy,
            timestamp = Date(location.time),
            address = address
        )
    }
    
    private fun getAddressFromLocation(latitude: Double, longitude: Double): String? {
        return try {
            val addresses: List<Address> = geocoder.getFromLocation(latitude, longitude, 1) ?: emptyList()
            if (addresses.isNotEmpty()) {
                val address = addresses[0]
                buildString {
                    if (address.thoroughfare != null) append(address.thoroughfare).append(", ")
                    if (address.locality != null) append(address.locality).append(", ")
                    if (address.adminArea != null) append(address.adminArea).append(", ")
                    if (address.countryName != null) append(address.countryName)
                }.trimEnd(',', ' ')
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED &&
        ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}

