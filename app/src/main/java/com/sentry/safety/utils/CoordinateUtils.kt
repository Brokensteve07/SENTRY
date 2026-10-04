package com.sentry.safety.utils

import android.graphics.RectF
import com.sentry.safety.data.models.Direction

object CoordinateUtils {

    /**
     * Determines horizontal sector from bounding box normalized coordinates [0.0, 1.0]
     */
    fun determineDirection(box: RectF): Direction {
        val centerX = box.centerX()
        return when {
            centerX < 0.38f -> Direction.LEFT
            centerX > 0.62f -> Direction.RIGHT
            else -> Direction.CENTER
        }
    }

    /**
     * Approximates relative distance from bounding box height & width fraction
     * Prototype formula: smaller box -> farther distance, larger box -> closer distance
     */
    fun estimateDistanceMeters(box: RectF): Float {
        val area = (box.width() * box.height()).coerceIn(0.01f, 1.0f)
        // Baseline scale: area of 0.25 is ~ 3 meters, area of 0.04 is ~ 10 meters
        val estimated = 1.5f / kotlin.math.sqrt(area)
        return estimated.coerceIn(1.0f, 25.0f)
    }
}
