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

/**
 * The position the app may already read, for placing a map: the last fix, else a fresh one
 * within [timeoutMillis]. Never asks for the permission; without it the answer is null.
 */
suspend fun LocationProvider.fixWithoutPrompt(timeoutMillis: Long = 3_000L): LocationFix? =
    lastKnown() ?: (current(timeoutMillis) as? LocationResult.Available)?.fix

/**
 * The only location permissions this app ever asks for, both of them foreground.
 *
 * Background location is not among them and never will be: the platform treats it as a separate,
 * sensitive request, the app has no use that would justify it, and `15-SECURITY-PRIVACY.md`
 * forbids it. The screen that asks and the screens that offer to sort by distance all launch
 * exactly this list.
 */
val FOREGROUND_LOCATION_PERMISSIONS = listOf(
    "android.permission.ACCESS_COARSE_LOCATION",
    "android.permission.ACCESS_FINE_LOCATION",
)
