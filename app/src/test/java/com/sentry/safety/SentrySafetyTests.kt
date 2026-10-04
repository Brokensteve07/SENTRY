package com.sentry.safety

import android.graphics.RectF
import com.sentry.safety.context.ContextEngineImpl
import com.sentry.safety.data.models.DetectedObject
import com.sentry.safety.data.models.Direction
import com.sentry.safety.data.models.MotionContext
import com.sentry.safety.data.models.MovementTrajectory
import com.sentry.safety.data.models.ObjectThreat
import com.sentry.safety.data.models.PhoneOrientation
import com.sentry.safety.data.models.RiskLevel
import com.sentry.safety.data.models.SafetyMode
import com.sentry.safety.data.models.SoundClassification
import com.sentry.safety.data.models.SoundEvent
import com.sentry.safety.data.models.UserMotionState
import com.sentry.safety.risk.RiskEngineImpl
import com.sentry.safety.utils.CoordinateUtils
import com.sentry.safety.utils.TrackingUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SentrySafetyTests {

    private lateinit var contextEngine: ContextEngineImpl
    private lateinit var riskEngine: RiskEngineImpl

    @Before
    fun setUp() {
        contextEngine = ContextEngineImpl()
        riskEngine = RiskEngineImpl()
    }

    @Test
    fun testDirectionCalculation() {
        // Left sector: centerX < 0.38 → (0.10 + 0.30)/2 = 0.20
        assertEquals(Direction.LEFT, CoordinateUtils.determineDirection(0.20f))

        // Center sector: 0.38 <= centerX <= 0.62 → (0.40 + 0.60)/2 = 0.50
        assertEquals(Direction.CENTER, CoordinateUtils.determineDirection(0.50f))

        // Right sector: centerX > 0.62 → (0.70 + 0.90)/2 = 0.80
        assertEquals(Direction.RIGHT, CoordinateUtils.determineDirection(0.80f))
    }

    @Test
    fun testTrajectoryEstimation() {
        // Expanding: prevArea=0.01, currArea=0.09 → areaChange = (0.09-0.01)/0.01 = 8.0 → APPROACHING
        assertEquals(
            MovementTrajectory.APPROACHING,
            TrackingUtils.estimateTrajectory(prevArea = 0.01f, currArea = 0.09f, deltaX = 0.0f)
        )

        // Shrinking: prevArea=0.09, currArea=0.0036 → areaChange ≈ -0.96 → MOVING_AWAY
        assertEquals(
            MovementTrajectory.MOVING_AWAY,
            TrackingUtils.estimateTrajectory(prevArea = 0.09f, currArea = 0.0036f, deltaX = 0.0f)
        )

        // Lateral rightward drift, no significant size change → CROSSING_LEFT_TO_RIGHT
        assertEquals(
            MovementTrajectory.CROSSING_LEFT_TO_RIGHT,
            TrackingUtils.estimateTrajectory(prevArea = 0.04f, currArea = 0.04f, deltaX = 0.15f)
        )

        // Stationary — no significant area change, no drift
        assertEquals(
            MovementTrajectory.STATIONARY,
            TrackingUtils.estimateTrajectory(prevArea = 0.04f, currArea = 0.042f, deltaX = 0.01f)
        )
    }


    @Test
    fun testRiskScoringVehicleAlone() {
        // Vehicle stationary/far alone should only produce LOW/NORMAL or CAUTION
        val car = DetectedObject(
            trackingId = 1,
            label = "Car",
            confidence = 0.85f,
            boundingBox = RectF(0.65f, 0.3f, 0.85f, 0.6f),
            direction = Direction.RIGHT,
            trajectory = MovementTrajectory.STATIONARY,
            estimatedDistanceMeters = 10.0f
        )
        contextEngine.updateDetections(listOf(car))
        val fused = contextEngine.evaluateContext()
        val risk = riskEngine.evaluate(fused)

        assertEquals(ObjectThreat.VEHICLE, risk.primaryThreat)
        assertTrue("Vehicle alone risk should be <= 50", risk.riskScore <= 50)
    }

    @Test
    fun testContextFusionVehicleApproachingAndHorn() {
        // Approaching vehicle + horn
        val car = DetectedObject(
            trackingId = 1,
            label = "Car",
            confidence = 0.92f,
            boundingBox = RectF(0.65f, 0.3f, 0.95f, 0.7f),
            direction = Direction.RIGHT,
            trajectory = MovementTrajectory.APPROACHING,
            estimatedDistanceMeters = 3.5f
        )
        val horn = SoundEvent(
            soundType = SoundClassification.HORN,
            confidence = 0.88f,
            decibels = 80f
        )

        contextEngine.updateDetections(listOf(car))
        contextEngine.updateSound(horn)
        val fused = contextEngine.evaluateContext()
        val risk = riskEngine.evaluate(fused)

        assertTrue("Approaching vehicle + horn should have risk >= 75", risk.riskScore >= 75)
        assertEquals(Direction.RIGHT, risk.direction)
        assertEquals("Vehicle approaching from your right.", risk.voiceWarning)
    }

    @Test
    fun testContextFusionVehicleHornAndInattention() {
        // Multi-signal: Vehicle approaching from right + horn + walking + user looking away
        val car = DetectedObject(
            trackingId = 1,
            label = "Car",
            confidence = 0.95f,
            boundingBox = RectF(0.68f, 0.25f, 0.98f, 0.75f),
            direction = Direction.RIGHT,
            trajectory = MovementTrajectory.APPROACHING,
            estimatedDistanceMeters = 2.8f
        )
        val horn = SoundEvent(
            soundType = SoundClassification.HORN,
            confidence = 0.90f,
            decibels = 82f
        )
        val motion = MotionContext(
            motionState = UserMotionState.WALKING,
            orientation = PhoneOrientation.FACING_AWAY
        )

        contextEngine.updateDetections(listOf(car))
        contextEngine.updateSound(horn)
        contextEngine.updateMotion(motion)

        val fused = contextEngine.evaluateContext()
        val risk = riskEngine.evaluate(fused)

        assertTrue("Fused context should flag inattention", fused.inattentionDetected)
        assertTrue("Risk score should be HIGH (>= 81)", risk.riskScore >= 81)
        assertEquals(RiskLevel.HIGH, risk.riskLevel)
        assertEquals(Direction.RIGHT, risk.direction)
        assertEquals("Vehicle approaching from your right.", risk.voiceWarning)
        assertTrue("Reason should mention approaching vehicle", risk.reason.contains("Approaching vehicle"))
        assertTrue("Reason should mention user attention away", risk.reason.contains("User attention away"))
    }

    @Test
    fun testObstacleAheadRisk() {
        val obstacle = DetectedObject(
            trackingId = 2,
            label = "Obstacle",
            confidence = 0.88f,
            boundingBox = RectF(0.40f, 0.40f, 0.60f, 0.80f),
            direction = Direction.CENTER,
            trajectory = MovementTrajectory.STATIONARY
        )
        contextEngine.updateDetections(listOf(obstacle))
        val fused = contextEngine.evaluateContext()
        val risk = riskEngine.evaluate(fused)

        assertEquals(ObjectThreat.OBSTACLE, risk.primaryThreat)
        assertEquals("Obstacle ahead.", risk.voiceWarning)
    }

    @Test
    fun testFallDetectionEscalation() {
        val fallMotion = MotionContext(
            motionState = UserMotionState.FALL_DETECTED,
            fallDetected = true
        )
        contextEngine.setSafetyMode(SafetyMode.ELDERLY)
        contextEngine.updateMotion(fallMotion)

        val fused = contextEngine.evaluateContext()
        val risk = riskEngine.evaluate(fused)

        assertEquals(RiskLevel.HIGH, risk.riskLevel)
        assertEquals("Possible fall detected.", risk.voiceWarning)
    }

    @Test
    fun testRiskReductionWhenThreatClears() {
        // Start with danger
        testContextFusionVehicleHornAndInattention()

        // Clear detections and sounds
        contextEngine.updateDetections(emptyList())
        contextEngine.updateSound(SoundEvent(soundType = SoundClassification.AMBIENT))
        contextEngine.updateMotion(MotionContext(motionState = UserMotionState.STATIONARY))

        val fused = contextEngine.evaluateContext()
        val risk = riskEngine.evaluate(fused)

        assertEquals(0, risk.riskScore)
        assertEquals(RiskLevel.NORMAL, risk.riskLevel)
        assertEquals("Area appears safe.", risk.voiceWarning)
    }
}
