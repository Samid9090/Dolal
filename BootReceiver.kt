package com.parentkidsapp.kids

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.parentkidsapp.utils.PreferenceManager

class BootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                val preferenceManager = PreferenceManager(context)
                
                // Only auto-start if kids mode is active and monitoring is enabled
                if (preferenceManager.isKidsMode() && preferenceManager.isMonitoringActive()) {
                    startMonitoringService(context)
                }
            }
        }
    }
    
    private fun startMonitoringService(context: Context) {
        val serviceIntent = Intent(context, MonitoringService::class.java)
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            // Handle service start failure
        }
    }
}

