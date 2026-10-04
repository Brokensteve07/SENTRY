package com.sentry.safety.risk

import com.sentry.safety.data.models.Direction
import com.sentry.safety.data.models.FusedContext
import com.sentry.safety.data.models.MovementTrajectory
import com.sentry.safety.data.models.ObjectThreat
import com.sentry.safety.data.models.RiskLevel
import com.sentry.safety.data.models.RiskResult
import com.sentry.safety.data.models.SafetyMode
import com.sentry.safety.data.models.SoundClassification
import com.sentry.safety.data.models.UserMotionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RiskEngineImpl : RiskEngine {

    private val _currentRiskResult = MutableStateFlow(RiskResult())
    override val currentRiskResult: StateFlow<RiskResult> = _currentRiskResult.asStateFlow()

    private val rules = mutableListOf<RiskRule>()

    init {
        // Register default modular risk rules
        registerRule(ApproachingVehicleRule())
        registerRule(AcousticWarningRule())
        registerRule(UserInattentionRule())
        registerRule(FallDetectionRule())
        registerRule(ObstacleProximityRule())
        registerRule(SuddenBrakingRule())
    }

    override fun registerRule(rule: RiskRule) {
        rules.add(rule)
    }

    override fun evaluate(fusedContext: FusedContext): RiskResult {
        var totalScore = 0
        val reasons = mutableListOf<String>()

        for (rule in rules) {
            val eval = rule.evaluate(fusedContext)
            if (eval.matched) {
                totalScore += (eval.scoreContribution * rule.weight).toInt()
                if (eval.reason.isNotEmpty()) {
                    reasons.add(eval.reason)
                }
            }
        }

        val clampedScore = totalScore.coerceIn(0, 100)

        val riskLevel = when {
            clampedScore >= 81 -> RiskLevel.HIGH
            clampedScore >= 61 -> RiskLevel.WARNING
            clampedScore >= 31 -> RiskLevel.CAUTION
            else -> RiskLevel.NORMAL
        }

        val primaryThreat = fusedContext.primaryThreatObject?.threatType ?: when {
            fusedContext.motionContext.fallDetected -> ObjectThreat.HAZARD
            fusedContext.soundEvent.soundType == SoundClassification.HORN -> ObjectThreat.VEHICLE
            fusedContext.soundEvent.soundType == SoundClassification.ALARM -> ObjectThreat.HAZARD
            else -> ObjectThreat.NONE
        }

        val direction = fusedContext.primaryThreatObject?.direction ?: Direction.CENTER

        val combinedReason = if (reasons.isNotEmpty()) {
            reasons.joinToString(" + ")
        } else {
            "Surroundings appear clear and normal."
        }

        val recommendedAction = when (riskLevel) {
            RiskLevel.HIGH -> "WARN_USER_URGENT"
            RiskLevel.WARNING -> "WARN_USER"
            RiskLevel.CAUTION -> "NOTIFY_SUBTLE"
            RiskLevel.NORMAL -> "MONITOR"
        }

        val dirText = when (direction) {
            Direction.LEFT -> "FROM YOUR LEFT"
            Direction.RIGHT -> "FROM YOUR RIGHT"
            Direction.CENTER -> "AHEAD"
            Direction.NONE -> ""
        }

        val visualWarning = when {
            fusedContext.motionContext.fallDetected -> "⚠️ POSSIBLE FALL DETECTED"
            primaryThreat == ObjectThreat.VEHICLE && riskLevel >= RiskLevel.WARNING ->
                "⚠️ VEHICLE APPROACHING\n$dirText"
            primaryThreat == ObjectThreat.OBSTACLE && riskLevel >= RiskLevel.CAUTION ->
                "⚠️ OBSTACLE AHEAD"
            fusedContext.soundEvent.soundType == SoundClassification.ALARM ->
                "⚠️ EMERGENCY ALARM DETECTED"
            riskLevel >= RiskLevel.CAUTION ->
                "⚠️ CAUTION IN SURROUNDINGS"
            else -> ""
        }

        val voiceWarning = when {
            fusedContext.motionContext.fallDetected -> "Possible fall detected."
            primaryThreat == ObjectThreat.VEHICLE && direction == Direction.RIGHT ->
                "Vehicle approaching from your right."
            primaryThreat == ObjectThreat.VEHICLE && direction == Direction.LEFT ->
                "Vehicle approaching from your left."
            primaryThreat == ObjectThreat.VEHICLE ->
                "Be careful, vehicle approaching."
            primaryThreat == ObjectThreat.OBSTACLE ->
                "Obstacle ahead."
            fusedContext.soundEvent.soundType == SoundClassification.ALARM ->
                "Possible emergency detected."
            fusedContext.motionContext.suddenBraking ->
                "Sudden braking detected."
            else -> "Area appears safe."
        }

        val result = RiskResult(
            riskScore = clampedScore,
            riskLevel = riskLevel,
            primaryThreat = primaryThreat,
            direction = direction,
            reason = combinedReason,
            recommendedAction = recommendedAction,
            visualWarning = visualWarning,
            voiceWarning = voiceWarning,
            timestamp = System.currentTimeMillis()
        )

        _currentRiskResult.value = result
        return result
    }
}

// ==========================================
// MODULAR RISK RULES
// ==========================================

class ApproachingVehicleRule : RiskRule {
    override val name = "ApproachingVehicleRule"
    override val weight = 1.0f

    override fun evaluate(context: FusedContext): RuleEvaluation {
        val threat = context.primaryThreatObject ?: return RuleEvaluation(0, false)
        if (threat.threatType != ObjectThreat.VEHICLE) return RuleEvaluation(0, false)

        var score = 30 // Base vehicle detection presence
        val isApproaching = threat.trajectory == MovementTrajectory.APPROACHING

        if (isApproaching) {
            score += 35
        }

        if (threat.estimatedDistanceMeters < 5.0f) {
            score += 15
        }

        val reason = if (isApproaching) "Approaching vehicle" else "Vehicle detected"
        return RuleEvaluation(score, true, reason)
    }
}

class AcousticWarningRule : RiskRule {
    override val name = "AcousticWarningRule"
    override val weight = 1.0f

    override fun evaluate(context: FusedContext): RuleEvaluation {
        val sound = context.soundEvent
        return when (sound.soundType) {
            SoundClassification.HORN -> RuleEvaluation(20, true, "Horn sound")
            SoundClassification.ALARM -> RuleEvaluation(45, true, "Alarm siren")
            SoundClassification.IMPACT -> RuleEvaluation(40, true, "Impact noise")
            SoundClassification.GLASS_BREAK -> RuleEvaluation(35, true, "Glass shatter")
            SoundClassification.SHOUT -> RuleEvaluation(25, true, "Distress vocalization")
            else -> RuleEvaluation(0, false)
        }
    }
}

class UserInattentionRule : RiskRule {
    override val name = "UserInattentionRule"
    override val weight = 1.0f

    override fun evaluate(context: FusedContext): RuleEvaluation {
        // Boosts risk if user is walking and looking away from an approaching threat
        if (context.inattentionDetected && context.primaryThreatObject?.trajectory == MovementTrajectory.APPROACHING) {
            return RuleEvaluation(20, true, "User attention away")
        }
        return RuleEvaluation(0, false)
    }
}

class FallDetectionRule : RiskRule {
    override val name = "FallDetectionRule"
    override val weight = 1.0f

    override fun evaluate(context: FusedContext): RuleEvaluation {
        if (context.motionContext.fallDetected || context.motionContext.motionState == UserMotionState.FALL_DETECTED) {
            return RuleEvaluation(90, true, "Sudden fall motion signature")
        }
        return RuleEvaluation(0, false)
    }
}

class ObstacleProximityRule : RiskRule {
    override val name = "ObstacleProximityRule"
    override val weight = 1.0f

    override fun evaluate(context: FusedContext): RuleEvaluation {
        val threat = context.primaryThreatObject ?: return RuleEvaluation(0, false)
        if (threat.threatType == ObjectThreat.OBSTACLE) {
            return RuleEvaluation(45, true, "Obstacle in path")
        }
        return RuleEvaluation(0, false)
    }
}

class SuddenBrakingRule : RiskRule {
    override val name = "SuddenBrakingRule"
    override val weight = 1.0f

    override fun evaluate(context: FusedContext): RuleEvaluation {
        if (context.motionContext.suddenBraking && context.safetyMode == SafetyMode.RIDER) {
            return RuleEvaluation(65, true, "Rapid deceleration spike")
        }
        return RuleEvaluation(0, false)
    }
}
