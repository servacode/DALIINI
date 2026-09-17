package com.servacode.directory.core.location

import kotlinx.coroutines.flow.Flow

data class LocationFix(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val precise: Boolean,
    val capturedAtEpochMillis: Long,
)

sealed interface LocationResult {
    data class Available(val fix: LocationFix) : LocationResult
    data object PermissionDenied : LocationResult
    data object Unavailable : LocationResult
}

interface LocationProvider {
    suspend fun current(timeoutMillis: Long = 8_000L): LocationResult
    fun lastKnown(): LocationFix?
    fun updates(minTimeMillis: Long = 1_000L): Flow<LocationResult>
}
