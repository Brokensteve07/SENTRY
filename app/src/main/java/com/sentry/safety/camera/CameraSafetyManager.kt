package com.sentry.safety.camera

import android.content.Context
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import com.sentry.safety.data.models.DetectedObject
import kotlinx.coroutines.flow.StateFlow

interface CameraSafetyManager {
    val isCameraActive: StateFlow<Boolean>
    val isCameraAvailable: Boolean
    val latestDetections: StateFlow<List<DetectedObject>>

    fun startCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        onFrameAnalyzed: ((List<DetectedObject>) -> Unit)? = null
    )
    fun stopCamera()
    fun release()
}
