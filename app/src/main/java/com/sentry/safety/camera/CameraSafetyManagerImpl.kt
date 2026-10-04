package com.sentry.safety.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.sentry.safety.ai.AIEngine
import com.sentry.safety.data.models.DetectedObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraSafetyManagerImpl(
    private val context: Context,
    private val aiEngine: AIEngine
) : CameraSafetyManager {

    private val tag = "CameraSafetyManager"

    private val _isCameraActive = MutableStateFlow(false)
    override val isCameraActive: StateFlow<Boolean> = _isCameraActive.asStateFlow()

    private var _isCameraAvailable = true
    override val isCameraAvailable: Boolean
        get() = _isCameraAvailable

    private val _latestDetections = MutableStateFlow<List<DetectedObject>>(emptyList())
    override val latestDetections: StateFlow<List<DetectedObject>> = _latestDetections.asStateFlow()

    private var cameraExecutor: ExecutorService? = null
    private var cameraProvider: ProcessCameraProvider? = null

    // Frame throttling: minimum 100ms interval (~10 FPS) to preserve battery
    private var lastAnalyzedTimestamp = 0L
    private val frameIntervalMs = 100L

    override fun startCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        onFrameAnalyzed: ((List<DetectedObject>) -> Unit)?
    ) {
        if (_isCameraActive.value) return

        cameraExecutor = Executors.newSingleThreadExecutor()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build()

                imageAnalysis.setAnalyzer(cameraExecutor!!) { imageProxy ->
                    analyzeImage(imageProxy, onFrameAnalyzed)
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )

                _isCameraActive.value = true
                _isCameraAvailable = true
                Log.i(tag, "CameraX started successfully.")
            } catch (exc: Exception) {
                Log.e(tag, "Failed to initialize CameraX: ${exc.message}", exc)
                _isCameraAvailable = false
                _isCameraActive.value = false
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun analyzeImage(
        imageProxy: ImageProxy,
        onFrameAnalyzed: ((List<DetectedObject>) -> Unit)?
    ) {
        val currentTimestamp = System.currentTimeMillis()
        if (currentTimestamp - lastAnalyzedTimestamp < frameIntervalMs) {
            imageProxy.close()
            return
        }
        lastAnalyzedTimestamp = currentTimestamp

        try {
            val bitmap = imageProxy.toBitmap()
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees

            val detections = aiEngine.detectObjects(bitmap, rotationDegrees)
            _latestDetections.value = detections
            onFrameAnalyzed?.invoke(detections)
        } catch (e: Exception) {
            Log.e(tag, "Error analyzing camera frame: ${e.message}")
        } finally {
            imageProxy.close()
        }
    }

    override fun stopCamera() {
        try {
            cameraProvider?.unbindAll()
            cameraExecutor?.shutdown()
            cameraExecutor = null
            _isCameraActive.value = false
        } catch (e: Exception) {
            Log.e(tag, "Error stopping camera: ${e.message}")
        }
    }

    override fun release() {
        stopCamera()
    }
}
