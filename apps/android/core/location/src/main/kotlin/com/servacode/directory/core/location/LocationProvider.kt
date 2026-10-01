package com.servacode.directory.core.location

import kotlinx.coroutines.flow.Flow
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

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

/**
 * How far apart two fixes are, in metres.
 *
 * Whoever watches a stream of positions has to decide when one is a new place rather than the
 * same place reported again — a phone reports every second or two, and the platform is not a
 * cartographer to be consulted that often. The haversine formula on a sphere: the error against
 * a true ellipsoid is a fraction of a percent, and nothing here is deciding a border.
 */
fun LocationFix.metresTo(other: LocationFix): Double {
    val lat1 = latitude * PI / 180
    val lat2 = other.latitude * PI / 180
    val deltaLat = (other.latitude - latitude) * PI / 180
    val deltaLon = (other.longitude - longitude) * PI / 180
    val h = sin(deltaLat / 2) * sin(deltaLat / 2) +
        cos(lat1) * cos(lat2) * sin(deltaLon / 2) * sin(deltaLon / 2)
    return 2 * EARTH_RADIUS_METRES * asin(min(1.0, sqrt(h)))
}

private const val EARTH_RADIUS_METRES = 6_371_000.0
