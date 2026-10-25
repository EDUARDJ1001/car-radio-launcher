package com.eduard.crvlauncher.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.roundToInt

data class GeoFix(
    val lat: Double,
    val lon: Double,
    /** Degrees clockwise from north, or null when stopped / unknown. */
    val bearing: Float?,
    val speedKmh: Int?,
)

/** Live GPS position for the navigation card (1 s updates while the home screen is visible). */
class LocationRepository(private val context: Context) {

    private val _fix = MutableStateFlow<GeoFix?>(null)
    val fix: StateFlow<GeoFix?> = _fix

    private val manager = context.getSystemService(LocationManager::class.java)

    private val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) = publish(location)
        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
        override fun onProviderEnabled(provider: String) = Unit
        override fun onProviderDisabled(provider: String) = Unit
    }

    @SuppressLint("MissingPermission")
    fun start() {
        val granted = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted || manager == null) return
        runCatching {
            manager.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let(::publish)
            manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, listener, Looper.getMainLooper())
        }
    }

    fun stop() {
        runCatching { manager?.removeUpdates(listener) }
    }

    private fun publish(l: Location) {
        val moving = l.hasSpeed() && l.speed > 1.5f // ~5 km/h: below that GPS bearing is noise
        _fix.value = GeoFix(
            lat = l.latitude,
            lon = l.longitude,
            bearing = if (moving && l.hasBearing()) l.bearing else _fix.value?.bearing,
            speedKmh = if (l.hasSpeed()) (l.speed * 3.6f).roundToInt() else null,
        )
    }
}
