package com.servacode.directory.core.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class AndroidLocationProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : LocationProvider {
    private val manager = context.getSystemService(LocationManager::class.java)

    override suspend fun current(timeoutMillis: Long): LocationResult {
        if (!hasLocationPermission()) return LocationResult.PermissionDenied
        val provider = bestProvider() ?: return lastKnown()?.let(LocationResult::Available)
            ?: LocationResult.Unavailable
        val location = withTimeoutOrNull(timeoutMillis) {
            suspendCancellableCoroutine<Location?> { continuation ->
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        manager.removeUpdates(this)
                        if (continuation.isActive) continuation.resume(location)
                    }
                    override fun onProviderEnabled(provider: String) = Unit
                    override fun onProviderDisabled(provider: String) = Unit
                    @Deprecated("Legacy callback")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
                }
                runCatching {
                    manager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
                }.onFailure {
                    manager.removeUpdates(listener)
                    if (continuation.isActive) continuation.resume(null)
                }
                continuation.invokeOnCancellation { manager.removeUpdates(listener) }
            }
        }
        return location?.toFix()?.let(LocationResult::Available)
            ?: lastKnown()?.let(LocationResult::Available)
            ?: LocationResult.Unavailable
    }

    override fun lastKnown(): LocationFix? {
        if (!hasLocationPermission()) return null
        return listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
            .maxByOrNull { it.time }
            ?.toFix()
    }

    private fun hasLocationPermission(): Boolean {
        val precise = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val approximate = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        return precise || approximate
    }

    private fun bestProvider(): String? = when {
        manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
        manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
        else -> null
    }

    private fun Location.toFix() = LocationFix(
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = accuracy,
        precise = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED,
        capturedAtEpochMillis = time,
    )
}
