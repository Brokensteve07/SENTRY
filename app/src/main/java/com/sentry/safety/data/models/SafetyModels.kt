package com.sentry.safety.data.models

import android.graphics.RectF

enum class RiskLevel {
    NORMAL,    // 0 - 30
    CAUTION,   // 31 - 60
    WARNING,   // 61 - 80
    HIGH       // 81 - 100
}

enum class Direction {
    LEFT,
    CENTER,
    RIGHT,
    NONE
}

enum class ObjectThreat {
    VEHICLE,      // car, bus, truck, motorcycle
    PEDESTRIAN,   // person
    BICYCLE,      // bicycle
    OBSTACLE,     // road barriers, construction, drain
    HAZARD,       // fire, smoke, broken glass
    NONE
}

enum class MovementTrajectory {
    APPROACHING,
    MOVING_AWAY,
    CROSSING_LEFT_TO_RIGHT,
    CROSSING_RIGHT_TO_LEFT,
    STATIONARY,
    UNKNOWN
}

enum class SoundClassification {
    HORN,
    ALARM,
    IMPACT,
    GLASS_BREAK,
    SHOUT,
    SIREN,
    AMBIENT,
    NONE
}

enum class UserMotionState {
    WALKING,
    RUNNING,
    CYCLING,
    VEHICLE,
    STATIONARY,
    FALL_DETECTED
}

enum class PhoneOrientation {
    FACING_ROAD,
    FACING_AWAY,
    TILTED_DOWN,
    IN_POCKET,
    UNKNOWN
}

enum class SafetyMode(val title: String, val description: String) {
    PEDESTRIAN("Pedestrian", "Detects approaching vehicles, horns, obstacles & road hazards"),
    HOME("Home", "Monitors smoke/fire, alarms, glass break & unusual impacts"),
    ELDERLY("Elderly", "Tracks fall events, prolonged inactivity & distress cries"),
    RIDER("Rider", "Monitors sudden braking, high acceleration & rear/blindspot threats"),
    DEMO("Demo Mode", "Controlled hackathon demonstration with interactive scenarios")
}

data class DetectedObject(
    val trackingId: Int,
    val label: String,
    val confidence: Float,
    val boundingBox: RectF,
    val direction: Direction = Direction.CENTER,
    val trajectory: MovementTrajectory = MovementTrajectory.UNKNOWN,
    val estimatedDistanceMeters: Float = 5.0f,
    val timestamp: Long = System.currentTimeMillis()
) {
    val threatType: ObjectThreat
        get() = when (label.lowercase()) {
            "car", "truck", "bus", "motorcycle", "vehicle" -> ObjectThreat.VEHICLE
            "bicycle" -> ObjectThreat.BICYCLE
            "person" -> ObjectThreat.PEDESTRIAN
            "obstacle", "barrier", "drain" -> ObjectThreat.OBSTACLE
            "fire", "smoke" -> ObjectThreat.HAZARD
            else -> ObjectThreat.NONE
        }
}

data class SoundEvent(
    val soundType: SoundClassification = SoundClassification.NONE,
    val confidence: Float = 0.0f,
    val decibels: Float = 45.0f,
    val isSuddenSpike: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

data class MotionContext(
    val motionState: UserMotionState = UserMotionState.STATIONARY,
    val orientation: PhoneOrientation = PhoneOrientation.FACING_ROAD,
    val accelerationMagnitude: Float = 9.8f,
    val suddenBraking: Boolean = false,
    val suddenAcceleration: Boolean = false,
    val fallDetected: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

data class LocationContext(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val speedMps: Float = 0.0f,
    val isMoving: Boolean = false,
    val accuracyMeters: Float = 10.0f,
    val environmentHint: String = "Normal Road/Sidewalk",
    val timestamp: Long = System.currentTimeMillis()
)

data class FusedContext(
    val detectedObjects: List<DetectedObject> = emptyList(),
    val primaryThreatObject: DetectedObject? = null,
    val soundEvent: SoundEvent = SoundEvent(),
    val motionContext: MotionContext = MotionContext(),
    val locationContext: LocationContext = LocationContext(),
    val safetyMode: SafetyMode = SafetyMode.PEDESTRIAN,
    val inattentionDetected: Boolean = false,
    val combinedThreatSummary: String = "Surroundings Safe",
    val timestamp: Long = System.currentTimeMillis()
)

data class RiskResult(
    val riskScore: Int = 0,               // 0 to 100
    val riskLevel: RiskLevel = RiskLevel.NORMAL,
    val primaryThreat: ObjectThreat = ObjectThreat.NONE,
    val direction: Direction = Direction.NONE,
    val reason: String = "Environment appears safe.",
    val recommendedAction: String = "MONITOR",
    val visualWarning: String = "",
    val voiceWarning: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class SensorStatus(
    val cameraActive: Boolean = false,
    val cameraAvailable: Boolean = true,
    val audioActive: Boolean = false,
    val audioAvailable: Boolean = true,
    val motionActive: Boolean = false,
    val motionAvailable: Boolean = true,
    val locationActive: Boolean = false,
    val locationAvailable: Boolean = true
)
