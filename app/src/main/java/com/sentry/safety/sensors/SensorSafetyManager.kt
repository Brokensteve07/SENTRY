package com.sentry.safety.sensors

import com.sentry.safety.data.models.MotionContext
import kotlinx.coroutines.flow.StateFlow

interface SensorSafetyManager {
    val isSensorActive: StateFlow<Boolean>
    val isSensorAvailable: Boolean
    val motionState: StateFlow<MotionContext>

    fun startSensors()
    fun stopSensors()
    fun setOrientationFacingRoad(facingRoad: Boolean)
}
