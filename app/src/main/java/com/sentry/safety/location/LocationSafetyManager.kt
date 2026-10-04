package com.sentry.safety.location

import com.sentry.safety.data.models.LocationContext
import kotlinx.coroutines.flow.StateFlow

interface LocationSafetyManager {
    val isLocationActive: StateFlow<Boolean>
    val isLocationAvailable: Boolean
    val locationContext: StateFlow<LocationContext>

    fun startLocationUpdates()
    fun stopLocationUpdates()
}
