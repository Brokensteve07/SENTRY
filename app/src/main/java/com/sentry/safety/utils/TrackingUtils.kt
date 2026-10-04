package com.sentry.safety.utils

import android.graphics.RectF
import com.sentry.safety.data.models.MovementTrajectory

object TrackingUtils {

    /**
     * Calculates Intersection over Union between two bounding boxes.
     * Uses raw field arithmetic (no mocked methods) so this works in JVM unit tests.
     */
    fun calculateIoU(boxA: RectF, boxB: RectF): Float {
        val interLeft = kotlin.math.max(boxA.left, boxB.left)
        val interTop = kotlin.math.max(boxA.top, boxB.top)
        val interRight = kotlin.math.min(boxA.right, boxB.right)
        val interBottom = kotlin.math.min(boxA.bottom, boxB.bottom)

        val interArea = kotlin.math.max(0.0f, interRight - interLeft) * kotlin.math.max(0.0f, interBottom - interTop)
        val areaA = (boxA.right - boxA.left) * (boxA.bottom - boxA.top)
        val areaB = (boxB.right - boxB.left) * (boxB.bottom - boxB.top)

        val unionArea = areaA + areaB - interArea
        if (unionArea <= 0f) return 0f
        return interArea / unionArea
    }

    /**
     * JVM-safe core trajectory estimator — accepts raw floats with no Android framework dependency.
     *
     * @param prevArea  width * height of the previous bounding box (normalised 0..1)
     * @param currArea  width * height of the current bounding box  (normalised 0..1)
     * @param deltaX    change in horizontal centre (positive = rightward drift)
     */
    fun estimateTrajectory(prevArea: Float, currArea: Float, deltaX: Float): MovementTrajectory {
        val areaChange = (currArea - prevArea) / prevArea.coerceAtLeast(0.001f)
        return when {
            areaChange > 0.15f  -> MovementTrajectory.APPROACHING
            areaChange < -0.15f -> MovementTrajectory.MOVING_AWAY
            deltaX > 0.08f      -> MovementTrajectory.CROSSING_LEFT_TO_RIGHT
            deltaX < -0.08f     -> MovementTrajectory.CROSSING_RIGHT_TO_LEFT
            else                -> MovementTrajectory.STATIONARY
        }
    }

    /**
     * Convenience overload accepting RectF bounding boxes.
     * Use this inside the camera / AI pipeline at runtime on a real Android device.
     * (RectF constructor is mocked in JVM unit tests, so prefer the Float overload in tests.)
     */
    fun estimateTrajectory(prevBox: RectF, currBox: RectF): MovementTrajectory {
        val prevArea = (prevBox.right - prevBox.left) * (prevBox.bottom - prevBox.top)
        val currArea = (currBox.right - currBox.left) * (currBox.bottom - currBox.top)
        val prevCenterX = (prevBox.left + prevBox.right) / 2f
        val currCenterX = (currBox.left + currBox.right) / 2f
        return estimateTrajectory(prevArea, currArea, currCenterX - prevCenterX)
    }
}
