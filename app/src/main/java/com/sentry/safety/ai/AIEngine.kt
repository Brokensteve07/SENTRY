package com.sentry.safety.ai

import android.graphics.Bitmap
import com.sentry.safety.data.models.DetectedObject

interface AIEngine {
    val isModelLoaded: Boolean
    fun detectObjects(bitmap: Bitmap, rotationDegrees: Int): List<DetectedObject>
    fun setConfidenceThreshold(threshold: Float)
    fun close()
}
