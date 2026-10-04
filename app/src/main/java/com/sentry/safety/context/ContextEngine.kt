package com.sentry.safety.context

import com.sentry.safety.data.models.DetectedObject
import com.sentry.safety.data.models.FusedContext
import com.sentry.safety.data.models.LocationContext
import com.sentry.safety.data.models.MotionContext
import com.sentry.safety.data.models.SafetyMode
import com.sentry.safety.data.models.SoundEvent
import kotlinx.coroutines.flow.StateFlow

interface ContextEngine {
    val fusedContext: StateFlow<FusedContext>

    fun updateDetections(detections: List<DetectedObject>)
    fun updateSound(soundEvent: SoundEvent)
    fun updateMotion(motionContext: MotionContext)
    fun updateLocation(locationContext: LocationContext)
    fun setSafetyMode(mode: SafetyMode)

    fun evaluateContext(): FusedContext
}
