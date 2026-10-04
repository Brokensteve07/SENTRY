package com.sentry.safety.context

import com.sentry.safety.data.models.DetectedObject
import com.sentry.safety.data.models.Direction
import com.sentry.safety.data.models.FusedContext
import com.sentry.safety.data.models.LocationContext
import com.sentry.safety.data.models.MotionContext
import com.sentry.safety.data.models.MovementTrajectory
import com.sentry.safety.data.models.ObjectThreat
import com.sentry.safety.data.models.PhoneOrientation
import com.sentry.safety.data.models.SafetyMode
import com.sentry.safety.data.models.SoundClassification
import com.sentry.safety.data.models.SoundEvent
import com.sentry.safety.data.models.UserMotionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ContextEngineImpl : ContextEngine {

    private val _fusedContext = MutableStateFlow(FusedContext())
    override val fusedContext: StateFlow<FusedContext> = _fusedContext.asStateFlow()

    private var currentDetections: List<DetectedObject> = emptyList()
    private var currentSound: SoundEvent = SoundEvent()
    private var currentMotion: MotionContext = MotionContext()
    private var currentLocation: LocationContext = LocationContext()
    private var currentMode: SafetyMode = SafetyMode.PEDESTRIAN

    override fun updateDetections(detections: List<DetectedObject>) {
        this.currentDetections = detections
        evaluateContext()
    }

    override fun updateSound(soundEvent: SoundEvent) {
        this.currentSound = soundEvent
        evaluateContext()
    }

    override fun updateMotion(motionContext: MotionContext) {
        this.currentMotion = motionContext
        evaluateContext()
    }

    override fun updateLocation(locationContext: LocationContext) {
        this.currentLocation = locationContext
        evaluateContext()
    }

    override fun setSafetyMode(mode: SafetyMode) {
        this.currentMode = mode
        evaluateContext()
    }

    override fun evaluateContext(): FusedContext {
        // 1. Identify primary threat object
        val primaryThreat = currentDetections.maxByOrNull { obj ->
            val threatWeight = when (obj.threatType) {
                ObjectThreat.VEHICLE -> 50
                ObjectThreat.OBSTACLE -> 30
                ObjectThreat.HAZARD -> 45
                ObjectThreat.BICYCLE -> 25
                ObjectThreat.PEDESTRIAN -> 15
                ObjectThreat.NONE -> 0
            }
            val approachWeight = when (obj.trajectory) {
                MovementTrajectory.APPROACHING -> 35
                MovementTrajectory.CROSSING_LEFT_TO_RIGHT,
                MovementTrajectory.CROSSING_RIGHT_TO_LEFT -> 20
                MovementTrajectory.STATIONARY -> 10
                MovementTrajectory.MOVING_AWAY -> 0
                MovementTrajectory.UNKNOWN -> 5
            }
            threatWeight + approachWeight + (obj.confidence * 20).toInt()
        }

        // 2. Determine user inattention:
        // If phone is tilted down or facing away from road while moving, user is inattentive
        val isUserMoving = currentMotion.motionState == UserMotionState.WALKING ||
                currentMotion.motionState == UserMotionState.RUNNING ||
                currentLocation.isMoving

        val isPhoneOrientedAway = currentMotion.orientation == PhoneOrientation.FACING_AWAY ||
                currentMotion.orientation == PhoneOrientation.TILTED_DOWN

        val inattentionDetected = isUserMoving && isPhoneOrientedAway

        // 3. Multi-Signal Summary synthesis
        val summary = buildSummary(primaryThreat, currentSound, inattentionDetected, currentMotion)

        val fused = FusedContext(
            detectedObjects = currentDetections,
            primaryThreatObject = primaryThreat,
            soundEvent = currentSound,
            motionContext = currentMotion,
            locationContext = currentLocation,
            safetyMode = currentMode,
            inattentionDetected = inattentionDetected,
            combinedThreatSummary = summary,
            timestamp = System.currentTimeMillis()
        )

        _fusedContext.value = fused
        return fused
    }

    private fun buildSummary(
        threat: DetectedObject?,
        sound: SoundEvent,
        inattentive: Boolean,
        motion: MotionContext
    ): String {
        if (motion.fallDetected) {
            return "Fall impact detected! User may need assistance."
        }

        if (threat != null && threat.threatType == ObjectThreat.VEHICLE) {
            val dir = threat.direction.name.lowercase()
            val isHorn = sound.soundType == SoundClassification.HORN
            val isApproaching = threat.trajectory == MovementTrajectory.APPROACHING

            return when {
                isApproaching && isHorn && inattentive ->
                    "Critical: Fast vehicle approaching from $dir with horn sounding while user is looking away."
                isApproaching && isHorn ->
                    "High Danger: Approaching vehicle with horn from $dir."
                isApproaching ->
                    "Caution: Vehicle closing distance from $dir."
                else ->
                    "Vehicle detected on $dir side."
            }
        }

        if (threat != null && threat.threatType == ObjectThreat.OBSTACLE) {
            return "Physical obstacle detected ahead in movement path."
        }

        if (sound.soundType == SoundClassification.HORN) {
            return "Acoustic horn alert detected nearby."
        }

        if (sound.soundType == SoundClassification.ALARM) {
            return "Emergency alarm acoustic signature detected."
        }

        return "Surroundings monitored - Safe."
    }
}
