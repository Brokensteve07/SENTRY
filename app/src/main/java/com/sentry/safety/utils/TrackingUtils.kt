package com.sentry.safety.utils

import android.graphics.RectF
import com.sentry.safety.data.models.MovementTrajectory

object TrackingUtils {

    /**
     * Calculates Intersection over Union between two bounding boxes
     */
    fun calculateIoU(boxA: RectF, boxB: RectF): Float {
        val interLeft = kotlin.math.max(boxA.left, boxB.left)
        val interTop = kotlin.math.max(boxA.top, boxB.top)
        val interRight = kotlin.math.min(boxA.right, boxB.right)
        val interBottom = kotlin.math.min(boxA.bottom, boxB.bottom)

        val interArea = kotlin.math.max(0.0f, interRight - interLeft) * kotlin.math.max(0.0f, interBottom - interTop)
        val areaA = boxA.width() * boxA.height()
        val areaB = boxB.width() * boxB.height()

        val unionArea = areaA + areaB - interArea
        if (unionArea <= 0f) return 0f
        return interArea / unionArea
    }

    /**
     * Estimates movement trajectory from consecutive bounding boxes of the same object
     */
    fun estimateTrajectory(prevBox: RectF, currBox: RectF): MovementTrajectory {
        val prevArea = prevBox.width() * prevBox.height()
        val currArea = currBox.width() * currBox.height()
        val areaChange = (currArea - prevArea) / prevArea.coerceAtLeast(0.001f)

        val deltaX = currBox.centerX() - prevBox.centerX()

        return when {
            // Significant box expansion indicates object is getting closer (approaching)
            areaChange > 0.15f -> MovementTrajectory.APPROACHING
            // Significant box shrinkage indicates object is moving away
            areaChange < -0.15f -> MovementTrajectory.MOVING_AWAY
            // Lateral drift
            deltaX > 0.08f -> MovementTrajectory.CROSSING_LEFT_TO_RIGHT
            deltaX < -0.08f -> MovementTrajectory.CROSSING_RIGHT_TO_LEFT
            else -> MovementTrajectory.STATIONARY
        }
    }
}
