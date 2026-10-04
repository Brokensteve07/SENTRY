package com.sentry.safety.ui

import android.app.Application
import android.graphics.RectF
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sentry.safety.ai.AIEngine
import com.sentry.safety.ai.AIEngineImpl
import com.sentry.safety.alerts.AlertEngine
import com.sentry.safety.alerts.AlertEngineImpl
import com.sentry.safety.audio.AudioSafetyManager
import com.sentry.safety.audio.AudioSafetyManagerImpl
import com.sentry.safety.camera.CameraSafetyManager
import com.sentry.safety.camera.CameraSafetyManagerImpl
import com.sentry.safety.context.ContextEngine
import com.sentry.safety.context.ContextEngineImpl
import com.sentry.safety.data.models.DetectedObject
import com.sentry.safety.data.models.Direction
import com.sentry.safety.data.models.FusedContext
import com.sentry.safety.data.models.LocationContext
import com.sentry.safety.data.models.MotionContext
import com.sentry.safety.data.models.MovementTrajectory
import com.sentry.safety.data.models.ObjectThreat
import com.sentry.safety.data.models.PhoneOrientation
import com.sentry.safety.data.models.RiskLevel
import com.sentry.safety.data.models.RiskResult
import com.sentry.safety.data.models.SafetyMode
import com.sentry.safety.data.models.SensorStatus
import com.sentry.safety.data.models.SoundClassification
import com.sentry.safety.data.models.SoundEvent
import com.sentry.safety.data.models.UserMotionState
import com.sentry.safety.location.LocationSafetyManager
import com.sentry.safety.location.LocationSafetyManagerImpl
import com.sentry.safety.modes.SafetyModeManager
import com.sentry.safety.risk.RiskEngine
import com.sentry.safety.risk.RiskEngineImpl
import com.sentry.safety.sensors.SensorSafetyManager
import com.sentry.safety.sensors.SensorSafetyManagerImpl
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SentryViewModel(application: Application) : AndroidViewModel(application) {

    // Core Modular Components
    val aiEngine: AIEngine = AIEngineImpl(application)
    val cameraManager: CameraSafetyManager = CameraSafetyManagerImpl(application, aiEngine)
    val sensorManager: SensorSafetyManager = SensorSafetyManagerImpl(application)
    val audioManager: AudioSafetyManager = AudioSafetyManagerImpl(application)
    val locationManager: LocationSafetyManager = LocationSafetyManagerImpl(application)
    val contextEngine: ContextEngine = ContextEngineImpl()
    val riskEngine: RiskEngine = RiskEngineImpl()
    val alertEngine: AlertEngine = AlertEngineImpl(application)
    val modeManager: SafetyModeManager = SafetyModeManager()

    // Protection Active State
    private val _isProtectionActive = MutableStateFlow(false)
    val isProtectionActive: StateFlow<Boolean> = _isProtectionActive.asStateFlow()

    // Debug Panel expanded state
    private val _isDebugExpanded = MutableStateFlow(false)
    val isDebugExpanded: StateFlow<Boolean> = _isDebugExpanded.asStateFlow()

    // SOS Emergency Dialog State (for Elderly Mode)
    private val _showSosDialog = MutableStateFlow(false)
    val showSosDialog: StateFlow<Boolean> = _showSosDialog.asStateFlow()

    private val _sosCountdown = MutableStateFlow(10)
    val sosCountdown: StateFlow<Int> = _sosCountdown.asStateFlow()

    private var sosCountdownJob: Job? = null

    // Combined Sensor Status
    val sensorStatus: StateFlow<SensorStatus> = combine(
        cameraManager.isCameraActive,
        audioManager.isAudioActive,
        sensorManager.isSensorActive,
        locationManager.isLocationActive
    ) { cam, aud, mot, loc ->
        SensorStatus(
            cameraActive = cam,
            cameraAvailable = cameraManager.isCameraAvailable,
            audioActive = aud,
            audioAvailable = audioManager.isAudioAvailable,
            motionActive = mot,
            motionAvailable = sensorManager.isSensorAvailable,
            locationActive = loc,
            locationAvailable = locationManager.isLocationAvailable
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, SensorStatus())

    val currentMode: StateFlow<SafetyMode> = modeManager.currentMode
    val fusedContext: StateFlow<FusedContext> = contextEngine.fusedContext
    val riskResult: StateFlow<RiskResult> = riskEngine.currentRiskResult
    val lastAlert: StateFlow<RiskResult?> = alertEngine.lastTriggeredAlert

    init {
        // Wire up context engine updates to trigger risk evaluation & alerts
        viewModelScope.launch {
            fusedContext.collect { fused ->
                if (_isProtectionActive.value) {
                    val risk = riskEngine.evaluate(fused)
                    alertEngine.processRiskResult(risk)

                    if (fused.motionContext.fallDetected && !_showSosDialog.value) {
                        triggerSosWorkflow()
                    }
                }
            }
        }

        // Wire up sensor streams to context engine
        viewModelScope.launch {
            sensorManager.motionState.collect { motion ->
                contextEngine.updateMotion(motion)
            }
        }
        viewModelScope.launch {
            audioManager.soundEvents.collect { sound ->
                contextEngine.updateSound(sound)
            }
        }
        viewModelScope.launch {
            locationManager.locationContext.collect { loc ->
                contextEngine.updateLocation(loc)
            }
        }
    }

    fun toggleProtection() {
        if (_isProtectionActive.value) {
            stopProtection()
        } else {
            startProtection()
        }
    }

    fun startProtection() {
        _isProtectionActive.value = true
        sensorManager.startSensors()
        audioManager.startAudioMonitoring()
        locationManager.startLocationUpdates()
    }

    fun stopProtection() {
        _isProtectionActive.value = false
        cameraManager.stopCamera()
        sensorManager.stopSensors()
        audioManager.stopAudioMonitoring()
        locationManager.stopLocationUpdates()
        alertEngine.stopAlerts()
        cancelSos()
    }

    fun setMode(mode: SafetyMode) {
        modeManager.switchMode(mode)
        contextEngine.setSafetyMode(mode)
    }

    fun toggleDebug() {
        _isDebugExpanded.value = !_isDebugExpanded.value
    }

    fun setPhoneFacingRoad(facingRoad: Boolean) {
        sensorManager.setOrientationFacingRoad(facingRoad)
    }

    // ==========================================
    // HACKATHON DEMO MODE SCENARIOS
    // ==========================================

    fun playDemoScenario(scenarioIndex: Int) {
        setMode(SafetyMode.DEMO)
        startProtection()

        when (scenarioIndex) {
            1 -> {
                // Scenario 1: Vehicle approaching from right + horn + walking + facing away
                val vehicle = DetectedObject(
                    trackingId = 101,
                    label = "Car",
                    confidence = 0.93f,
                    boundingBox = RectF(0.68f, 0.28f, 0.95f, 0.72f),
                    direction = Direction.RIGHT,
                    trajectory = MovementTrajectory.APPROACHING,
                    estimatedDistanceMeters = 3.2f
                )
                val horn = SoundEvent(
                    soundType = SoundClassification.HORN,
                    confidence = 0.88f,
                    decibels = 82f,
                    isSuddenSpike = true
                )
                val motion = MotionContext(
                    motionState = UserMotionState.WALKING,
                    orientation = PhoneOrientation.FACING_AWAY,
                    accelerationMagnitude = 11.2f
                )

                contextEngine.updateDetections(listOf(vehicle))
                contextEngine.updateSound(horn)
                contextEngine.updateMotion(motion)
            }
            2 -> {
                // Scenario 2: Obstacle ahead
                val obstacle = DetectedObject(
                    trackingId = 102,
                    label = "Obstacle",
                    confidence = 0.89f,
                    boundingBox = RectF(0.38f, 0.45f, 0.62f, 0.85f),
                    direction = Direction.CENTER,
                    trajectory = MovementTrajectory.STATIONARY,
                    estimatedDistanceMeters = 2.0f
                )
                val ambient = SoundEvent(soundType = SoundClassification.AMBIENT, decibels = 48f)
                val motion = MotionContext(
                    motionState = UserMotionState.WALKING,
                    orientation = PhoneOrientation.FACING_ROAD
                )

                contextEngine.updateDetections(listOf(obstacle))
                contextEngine.updateSound(ambient)
                contextEngine.updateMotion(motion)
            }
            3 -> {
                // Scenario 3: Vehicle approaching from left
                val vehicle = DetectedObject(
                    trackingId = 103,
                    label = "Truck",
                    confidence = 0.91f,
                    boundingBox = RectF(0.05f, 0.25f, 0.35f, 0.75f),
                    direction = Direction.LEFT,
                    trajectory = MovementTrajectory.APPROACHING,
                    estimatedDistanceMeters = 4.1f
                )
                val ambient = SoundEvent(soundType = SoundClassification.AMBIENT, decibels = 55f)
                val motion = MotionContext(
                    motionState = UserMotionState.WALKING,
                    orientation = PhoneOrientation.FACING_ROAD
                )

                contextEngine.updateDetections(listOf(vehicle))
                contextEngine.updateSound(ambient)
                contextEngine.updateMotion(motion)
            }
            4 -> {
                // Scenario 4: Area safe
                contextEngine.updateDetections(emptyList())
                contextEngine.updateSound(SoundEvent(soundType = SoundClassification.AMBIENT, decibels = 42f))
                contextEngine.updateMotion(
                    MotionContext(
                        motionState = UserMotionState.STATIONARY,
                        orientation = PhoneOrientation.FACING_ROAD
                    )
                )
            }
            5 -> {
                // Scenario 5: Fall event (Elderly Mode test)
                setMode(SafetyMode.ELDERLY)
                val motion = MotionContext(
                    motionState = UserMotionState.FALL_DETECTED,
                    fallDetected = true,
                    accelerationMagnitude = 26.5f
                )
                contextEngine.updateMotion(motion)
                triggerSosWorkflow()
            }
        }
    }

    // ==========================================
    // ELDERLY MODE SOS WORKFLOW
    // ==========================================

    private fun triggerSosWorkflow() {
        _showSosDialog.value = true
        _sosCountdown.value = 10
        sosCountdownJob?.cancel()
        sosCountdownJob = viewModelScope.launch {
            while (_sosCountdown.value > 0) {
                delay(1000)
                _sosCountdown.value -= 1
            }
            // Once countdown reaches 0, trigger SOS confirmation alert
            alertEngine.triggerImmediateAlert(
                title = "EMERGENCY SOS DISPATCHED",
                spokenWarning = "Emergency contact notified.",
                direction = Direction.CENTER,
                level = RiskLevel.HIGH
            )
        }
    }

    fun cancelSos() {
        sosCountdownJob?.cancel()
        sosCountdownJob = null
        _showSosDialog.value = false
        _sosCountdown.value = 10
    }

    fun confirmSosHelp() {
        sosCountdownJob?.cancel()
        _sosCountdown.value = 0
        alertEngine.triggerImmediateAlert(
            title = "EMERGENCY HELP REQUESTED",
            spokenWarning = "Emergency assistance requested.",
            direction = Direction.CENTER,
            level = RiskLevel.HIGH
        )
    }

    override fun onCleared() {
        super.onCleared()
        stopProtection()
        cameraManager.release()
        alertEngine.release()
        aiEngine.close()
    }
}
