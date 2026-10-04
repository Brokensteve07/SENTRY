package com.sentry.safety.utils

import android.graphics.RectF
import com.sentry.safety.data.models.Direction

object CoordinateUtils {

    /**
     * Determines horizontal sector from raw normalized center X coordinate [0.0, 1.0].
     * This overload is safe to call from JVM unit tests (no Android framework dependency).
     */
    fun determineDirection(centerX: Float): Direction = when {
        centerX < 0.38f -> Direction.LEFT
        centerX > 0.62f -> Direction.RIGHT
        else -> Direction.CENTER
    }

    /**
     * Convenience overload that accepts an Android RectF bounding box.
     * Use at runtime inside the camera/AI pipeline.
     */
    fun determineDirection(box: RectF): Direction =
        determineDirection((box.left + box.right) / 2f)

    /**
     * Approximates relative distance from bounding box width & height fractions.
     * JVM-safe overload taking raw floats.
     */
    fun estimateDistanceMeters(boxWidth: Float, boxHeight: Float): Float {
        val area = (boxWidth * boxHeight).coerceIn(0.01f, 1.0f)
        // Baseline scale: area of 0.25 is ~ 3 meters, area of 0.04 is ~ 10 meters
        val estimated = 1.5f / kotlin.math.sqrt(area)
        return estimated.coerceIn(1.0f, 25.0f)
    }

    /**
     * Convenience overload that accepts an Android RectF bounding box.
     */
    fun estimateDistanceMeters(box: RectF): Float =
        estimateDistanceMeters(box.right - box.left, box.bottom - box.top)
}
