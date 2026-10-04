package com.sentry.safety.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import com.sentry.safety.data.models.LocationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LocationSafetyManagerImpl(
    private val context: Context
) : LocationSafetyManager, LocationListener {

    private val tag = "LocationSafetyManager"

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    private val _isLocationActive = MutableStateFlow(false)
    override val isLocationActive: StateFlow<Boolean> = _isLocationActive.asStateFlow()

    private var _isLocationAvailable = true
    override val isLocationAvailable: Boolean
        get() = _isLocationAvailable

    private val _locationContext = MutableStateFlow(LocationContext())
    override val locationContext: StateFlow<LocationContext> = _locationContext.asStateFlow()

    @SuppressLint("MissingPermission")
    override fun startLocationUpdates() {
        if (_isLocationActive.value || locationManager == null) return

        try {
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (!isGpsEnabled && !isNetworkEnabled) {
                Log.w(tag, "Location providers disabled. Running in fallback mode.")
                _isLocationActive.value = true
                return
            }

            if (isNetworkEnabled) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    2000L,
                    2.0f,
                    this
                )
            }
            if (isGpsEnabled) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    2000L,
                    2.0f,
                    this
                )
            }

            _isLocationActive.value = true
            _isLocationAvailable = true
            Log.i(tag, "Location updates registered.")
        } catch (e: Exception) {
            Log.w(tag, "Location updates permission not granted or hardware unavailable: ${e.message}")
            _isLocationAvailable = false
            _isLocationActive.value = false
        }
    }

    override fun stopLocationUpdates() {
        try {
            locationManager?.removeUpdates(this)
            _isLocationActive.value = false
            Log.i(tag, "Location updates stopped.")
        } catch (e: Exception) {
            Log.e(tag, "Error stopping location: ${e.message}")
        }
    }

    override fun onLocationChanged(location: Location) {
        val speedMps = if (location.hasSpeed()) location.speed else 0.0f
        val isMoving = speedMps > 0.5f

        val hint = when {
            speedMps > 7.0f -> "Moving Vehicle / High Speed Road"
            speedMps > 1.0f -> "Pedestrian / Sidewalk Movement"
            else -> "Stationary / Urban Area"
        }

        _locationContext.value = LocationContext(
            latitude = location.latitude,
            longitude = location.longitude,
            speedMps = speedMps,
            isMoving = isMoving,
            accuracyMeters = location.accuracy,
            environmentHint = hint,
            timestamp = System.currentTimeMillis()
        )
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
}
