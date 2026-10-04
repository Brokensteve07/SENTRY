package com.sentry.safety.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import com.sentry.safety.data.models.MotionContext
import com.sentry.safety.data.models.PhoneOrientation
import com.sentry.safety.data.models.UserMotionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.sqrt

class SensorSafetyManagerImpl(
    private val context: Context
) : SensorSafetyManager, SensorEventListener {

    private val tag = "SensorSafetyManager"

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val rotationVector = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val _isSensorActive = MutableStateFlow(false)
    override val isSensorActive: StateFlow<Boolean> = _isSensorActive.asStateFlow()

    private val _motionState = MutableStateFlow(MotionContext())
    override val motionState: StateFlow<MotionContext> = _motionState.asStateFlow()

    override val isSensorAvailable: Boolean
        get() = accelerometer != null

    // Tracking state
    private var lastAccelMagnitude = 9.8f
    private var stepCount = 0
    private var lastStepTime = 0L
    private var freeFallDetected = false
    private var freeFallTime = 0L
    private var isManuallyFacingAway = false

    override fun startSensors() {
        if (_isSensorActive.value || sensorManager == null) return

        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        gyroscope?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        rotationVector?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }

        _isSensorActive.value = true
        Log.i(tag, "Motion sensors started.")
    }

    override fun stopSensors() {
        if (!_isSensorActive.value || sensorManager == null) return

        sensorManager.unregisterListener(this)
        _isSensorActive.value = false
        Log.i(tag, "Motion sensors stopped.")
    }

    override fun setOrientationFacingRoad(facingRoad: Boolean) {
        isManuallyFacingAway = !facingRoad
        val current = _motionState.value
        _motionState.value = current.copy(
            orientation = if (facingRoad) PhoneOrientation.FACING_ROAD else PhoneOrientation.FACING_AWAY
        )
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> processAccelerometer(event.values)
            Sensor.TYPE_GYROSCOPE -> processGyroscope(event.values)
            Sensor.TYPE_ROTATION_VECTOR -> processRotation(event.values)
        }
    }

    private fun processAccelerometer(values: FloatArray) {
        val x = values[0]
        val y = values[1]
        val z = values[2]

        val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
        val delta = abs(magnitude - lastAccelMagnitude)
        lastAccelMagnitude = magnitude

        val currentTime = System.currentTimeMillis()

        // 1. Walking detection: rhythmic acceleration fluctuation
        val isWalking = if (delta in 1.2f..4.8f) {
            if (currentTime - lastStepTime > 350) {
                stepCount++
                lastStepTime = currentTime
            }
            true
        } else {
            currentTime - lastStepTime < 1800
        }

        // 2. Sudden deceleration / braking (drop or spike > 6.5 m/s^2)
        val suddenBraking = delta > 6.5f

        // 3. Fall detection: free fall (<3.0 m/s^2) followed by impact (>22.0 m/s^2)
        var fallDetected = false
        if (magnitude < 3.2f) {
            freeFallDetected = true
            freeFallTime = currentTime
        } else if (freeFallDetected && magnitude > 22.0f && (currentTime - freeFallTime < 1200)) {
            fallDetected = true
            freeFallDetected = false
            Log.w(tag, "Fall detected via accelerometer signature!")
        }

        // 4. Orientation estimation from gravitational vector
        val orientation = if (isManuallyFacingAway) {
            PhoneOrientation.FACING_AWAY
        } else {
            when {
                y > 7.0f -> PhoneOrientation.FACING_ROAD // Held upright facing forward
                z > 8.0f -> PhoneOrientation.TILTED_DOWN // Held flat looking down
                magnitude < 4.0f -> PhoneOrientation.IN_POCKET
                else -> PhoneOrientation.FACING_ROAD
            }
        }

        val motionState = when {
            fallDetected -> UserMotionState.FALL_DETECTED
            suddenBraking -> UserMotionState.VEHICLE
            isWalking -> UserMotionState.WALKING
            else -> UserMotionState.STATIONARY
        }

        _motionState.value = _motionState.value.copy(
            motionState = motionState,
            orientation = orientation,
            accelerationMagnitude = magnitude,
            suddenBraking = suddenBraking,
            fallDetected = fallDetected,
            timestamp = currentTime
        )
    }

    private fun processGyroscope(values: FloatArray) {
        val rx = values[0]
        val ry = values[1]
        val rz = values[2]
        val angularSpeed = sqrt((rx * rx + ry * ry + rz * rz).toDouble()).toFloat()

        // Sudden rotation indicates sudden turn away or loss of control
        if (angularSpeed > 3.5f) {
            _motionState.value = _motionState.value.copy(
                suddenAcceleration = true
            )
        }
    }

    private fun processRotation(values: FloatArray) {
        val rotationMatrix = FloatArray(9)
        SensorManager.getRotationMatrixFromVector(rotationMatrix, values)
        val orientationAngles = FloatArray(3)
        SensorManager.getOrientation(rotationMatrix, orientationAngles)

        val pitch = orientationAngles[1]
        // If user looks down or turns away
        if (!isManuallyFacingAway && abs(pitch) > 1.2f) {
            _motionState.value = _motionState.value.copy(
                orientation = PhoneOrientation.TILTED_DOWN
            )
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }
}
