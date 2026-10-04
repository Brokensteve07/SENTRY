package com.sentry.safety.modes

import com.sentry.safety.data.models.SafetyMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ModeConfig(
    val mode: SafetyMode,
    val enableCameraAnalysis: Boolean = true,
    val enableAudioClassification: Boolean = true,
    val enableMotionTracking: Boolean = true,
    val enableLocationContext: Boolean = true,
    val alertThresholdRisk: Int = 60,
    val primaryThreatFocus: String = "Vehicles and Road Crossings"
)

class SafetyModeManager {

    private val _currentMode = MutableStateFlow(SafetyMode.PEDESTRIAN)
    val currentMode: StateFlow<SafetyMode> = _currentMode.asStateFlow()

    private val _currentConfig = MutableStateFlow(getConfigForMode(SafetyMode.PEDESTRIAN))
    val currentConfig: StateFlow<ModeConfig> = _currentConfig.asStateFlow()

    fun switchMode(mode: SafetyMode) {
        _currentMode.value = mode
        _currentConfig.value = getConfigForMode(mode)
    }

    private fun getConfigForMode(mode: SafetyMode): ModeConfig {
        return when (mode) {
            SafetyMode.PEDESTRIAN -> ModeConfig(
                mode = mode,
                enableCameraAnalysis = true,
                enableAudioClassification = true,
                enableMotionTracking = true,
                enableLocationContext = true,
                alertThresholdRisk = 60,
                primaryThreatFocus = "Approaching Vehicles, Horns, Obstacles"
            )
            SafetyMode.HOME -> ModeConfig(
                mode = mode,
                enableCameraAnalysis = true,
                enableAudioClassification = true,
                enableMotionTracking = false, // stationary inside home
                enableLocationContext = false,
                alertThresholdRisk = 50,
                primaryThreatFocus = "Smoke/Fire, Alarms, Glass Shatter"
            )
            SafetyMode.ELDERLY -> ModeConfig(
                mode = mode,
                enableCameraAnalysis = false, // battery saving, primarily inertial/audio
                enableAudioClassification = true,
                enableMotionTracking = true,
                enableLocationContext = true,
                alertThresholdRisk = 65,
                primaryThreatFocus = "Sudden Fall, Prolonged Inactivity, Distress Voice"
            )
            SafetyMode.RIDER -> ModeConfig(
                mode = mode,
                enableCameraAnalysis = true,
                enableAudioClassification = true,
                enableMotionTracking = true,
                enableLocationContext = true,
                alertThresholdRisk = 55,
                primaryThreatFocus = "Sudden Braking, Rapid Decel, Blindspot Vehicles"
            )
            SafetyMode.DEMO -> ModeConfig(
                mode = mode,
                enableCameraAnalysis = true,
                enableAudioClassification = true,
                enableMotionTracking = true,
                enableLocationContext = true,
                alertThresholdRisk = 40,
                primaryThreatFocus = "Controlled Hackathon Interactive Scenarios"
            )
        }
    }
}
