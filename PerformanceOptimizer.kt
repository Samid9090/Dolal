package com.parentkidsapp.utils

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import androidx.work.*
import java.util.concurrent.TimeUnit

class PerformanceOptimizer(private val context: Context) {
    
    private val preferenceManager = PreferenceManager(context)
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    
    companion object {
        private const val BATTERY_OPTIMIZATION_WORK = "battery_optimization_work"
        private const val MEMORY_CLEANUP_WORK = "memory_cleanup_work"
        private const val LOW_BATTERY_THRESHOLD = 20
        private const val CRITICAL_BATTERY_THRESHOLD = 10
    }
    
    fun optimizeForBatteryLife() {
        val batteryLevel = getCurrentBatteryLevel()
        
        when {
            batteryLevel <= CRITICAL_BATTERY_THRESHOLD -> {
                applyCriticalBatteryOptimizations()
            }
            batteryLevel <= LOW_BATTERY_THRESHOLD -> {
                applyLowBatteryOptimizations()
            }
            else -> {
                applyNormalBatteryOptimizations()
            }
    }
    
    private fun getCurrentBatteryLevel(): Int {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        
        return if (level != -1 && scale != -1) {
            (level * 100 / scale.toFloat()).toInt()
        } else {
            100 // Default to full battery if unable to read
        }
    }
    
    private fun applyCriticalBatteryOptimizations() {
        // Reduce location update frequency
        preferenceManager.setLocationUpdateInterval(300000L) // 5 minutes
        
        // Disable screen mirroring
        preferenceManager.setScreenMirroringEnabled(false)
        
        // Reduce image quality
        preferenceManager.setImageQuality(50)
        
        // Increase data upload batch size
        preferenceManager.setDataUploadBatchSize(10)
        
        scheduleOptimizationWork(60) // Check every hour
    }
    
    private fun applyLowBatteryOptimizations() {
        // Reduce location update frequency
        preferenceManager.setLocationUpdateInterval(120000L) // 2 minutes
        
        // Reduce image quality
        preferenceManager.setImageQuality(70)
        
        // Increase data upload batch size
        preferenceManager.setDataUploadBatchSize(5)
        
        scheduleOptimizationWork(30) // Check every 30 minutes
    }
    
    private fun applyNormalBatteryOptimizations() {
        // Normal location update frequency
        preferenceManager.setLocationUpdateInterval(60000L) // 1 minute
        
        // Normal image quality
        preferenceManager.setImageQuality(90)
        
        // Normal data upload batch size
        preferenceManager.setDataUploadBatchSize(1)
        
        scheduleOptimizationWork(15) // Check every 15 minutes
    }
    
    private fun scheduleOptimizationWork(intervalMinutes: Long) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val optimizationWork = PeriodicWorkRequestBuilder<BatteryOptimizationWorker>(
            intervalMinutes, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            BATTERY_OPTIMIZATION_WORK,
            ExistingPeriodicWorkPolicy.REPLACE,
            optimizationWork
        )
    }
    
    fun optimizeMemoryUsage() {
        // Clear image caches
        clearImageCaches()
        
        // Cleanup old data
        scheduleDataCleanup()
        
        // Force garbage collection
        System.gc()
    }
    
    private fun clearImageCaches() {
        // Clear Glide cache if using Glide
        try {
            val glideClass = Class.forName("com.bumptech.glide.Glide")
            val getMethod = glideClass.getMethod("get", Context::class.java)
            val glideInstance = getMethod.invoke(null, context)
            val clearMemoryMethod = glideInstance.javaClass.getMethod("clearMemory")
            clearMemoryMethod.invoke(glideInstance)
        } catch (e: Exception) {
            // Glide not available or error clearing cache
        }
    }
    
    private fun scheduleDataCleanup() {
        val cleanupWork = OneTimeWorkRequestBuilder<DataCleanupWorker>()
            .setInitialDelay(1, TimeUnit.MINUTES)
            .build()
        
        WorkManager.getInstance(context).enqueue(cleanupWork)
    }
    
    fun getMemoryInfo(): MemoryInfo {
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        return MemoryInfo(
            availableMemory = memoryInfo.availMem,
            totalMemory = memoryInfo.totalMem,
            threshold = memoryInfo.threshold,
            lowMemory = memoryInfo.lowMemory
        )
    }
    
    fun isDeviceInPowerSaveMode(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            powerManager.isPowerSaveMode
        } else {
            false
        }
    }
    
    fun requestBatteryOptimizationExemption(): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent().apply {
                action = android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                data = android.net.Uri.parse("package:${context.packageName}")
            }
        } else {
            null
        }
    }
    
    fun isIgnoringBatteryOptimizations(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true
        }
    }
    
    fun getPerformanceMetrics(): PerformanceMetrics {
        val batteryLevel = getCurrentBatteryLevel()
        val memoryInfo = getMemoryInfo()
        val powerSaveMode = isDeviceInPowerSaveMode()
        val batteryOptimizationIgnored = isIgnoringBatteryOptimizations()
        
        return PerformanceMetrics(
            batteryLevel = batteryLevel,
            availableMemoryMB = (memoryInfo.availableMemory / 1024 / 1024).toInt(),
            totalMemoryMB = (memoryInfo.totalMemory / 1024 / 1024).toInt(),
            lowMemory = memoryInfo.lowMemory,
            powerSaveMode = powerSaveMode,
            batteryOptimizationIgnored = batteryOptimizationIgnored
        )
    }
}

data class MemoryInfo(
    val availableMemory: Long,
    val totalMemory: Long,
    val threshold: Long,
    val lowMemory: Boolean
)

data class PerformanceMetrics(
    val batteryLevel: Int,
    val availableMemoryMB: Int,
    val totalMemoryMB: Int,
    val lowMemory: Boolean,
    val powerSaveMode: Boolean,
    val batteryOptimizationIgnored: Boolean
)

class BatteryOptimizationWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    
    override fun doWork(): Result {
        val optimizer = PerformanceOptimizer(applicationContext)
        optimizer.optimizeForBatteryLife()
        return Result.success()
    }
}

class DataCleanupWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    
    override fun doWork(): Result {
        // Cleanup old data files, logs, etc.
        val optimizer = PerformanceOptimizer(applicationContext)
        optimizer.optimizeMemoryUsage()
        return Result.success()
    }
}

