package com.servacode.directory.core.maps

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private const val EARTH_RADIUS_METERS = 6_371_000.0

fun MapPoint.requireValid(): MapPoint {
    require(latitude in -90.0..90.0) { "Latitude is out of range." }
    require(longitude in -180.0..180.0) { "Longitude is out of range." }
    return this
}

enum class RoutingProfile(val wireName: String) {
    DRIVING("driving"),
}

enum class ManeuverKind {
    DEPART,
    CONTINUE,
    TURN,
    MERGE,
    FORK,
    ROUNDABOUT,
    UTURN,
    ARRIVE,
    UNKNOWN,
}

enum class ManeuverModifier {
    LEFT,
    RIGHT,
    SLIGHT_LEFT,
    SLIGHT_RIGHT,
    SHARP_LEFT,
    SHARP_RIGHT,
    STRAIGHT,
    UTURN,
    UNKNOWN,
}

data class RouteManeuver(
    val kind: ManeuverKind,
    val modifier: ManeuverModifier,
    val point: MapPoint,
    val streetName: String?,
    val distanceMeters: Double,
    val durationSeconds: Double,
    val roundaboutExit: Int? = null,
)

data class NavigationRoute(
    val geometry: List<MapPoint>,
    val distanceMeters: Double,
    val durationSeconds: Double,
    val maneuvers: List<RouteManeuver>,
) {
    init {
        require(geometry.size >= 2) { "Route geometry requires at least two points." }
        require(distanceMeters >= 0.0) { "Route distance cannot be negative." }
        require(durationSeconds >= 0.0) { "Route duration cannot be negative." }
        geometry.forEach(MapPoint::requireValid)
        maneuvers.forEach {
            it.point.requireValid()
            require(it.distanceMeters >= 0.0)
            require(it.durationSeconds >= 0.0)
        }
    }

    val destination: MapPoint get() = geometry.last()
}

data class GeocodingResult(
    val point: MapPoint,
    val displayName: String,
)

interface RoutingProvider {
    suspend fun route(
        origin: MapPoint,
        destination: MapPoint,
        profile: RoutingProfile = RoutingProfile.DRIVING,
    ): NavigationRoute
}

interface GeocodingProvider {
    suspend fun forward(query: String, limit: Int = 5): List<GeocodingResult>
    suspend fun reverse(point: MapPoint): GeocodingResult?
}

data class MapProviderConfig(
    val routingBaseUrl: String,
    val geocodingBaseUrl: String,
    val geocodingUserAgent: String,
)

class ProviderConfigurationException(message: String) : IllegalStateException(message)

object ProviderEndpointPolicy {
    private val forbiddenHosts = setOf(
        "router.project-osrm.org",
        "demotiles.maplibre.org",
    )

    fun requireConfiguredHttps(raw: String): String {
        val value = raw.trim()
        if (value.isBlank() || '<' in value || '>' in value) {
            throw ProviderConfigurationException("Map provider endpoint is not configured.")
        }
        if (!value.startsWith("https://")) {
            throw ProviderConfigurationException("Map provider endpoint must use HTTPS.")
        }
        val host = value.removePrefix("https://").substringBefore('/').substringBefore(':').lowercase()
        if (host in forbiddenHosts) {
            throw ProviderConfigurationException("Public demo map endpoints are forbidden.")
        }
        return value
    }
}

object GeoMath {
    fun distanceMeters(a: MapPoint, b: MapPoint): Double {
        a.requireValid()
        b.requireValid()
        val lat1 = a.latitude.toRadians()
        val lat2 = b.latitude.toRadians()
        val deltaLat = (b.latitude - a.latitude).toRadians()
        val deltaLon = (b.longitude - a.longitude).toRadians()
        val h = sin(deltaLat / 2) * sin(deltaLat / 2) +
            cos(lat1) * cos(lat2) * sin(deltaLon / 2) * sin(deltaLon / 2)
        return 2 * EARTH_RADIUS_METERS * asin(min(1.0, sqrt(h)))
    }

    fun distanceToPolylineMeters(point: MapPoint, geometry: List<MapPoint>): Double {
        point.requireValid()
        require(geometry.isNotEmpty()) { "Polyline is empty." }
        if (geometry.size == 1) return distanceMeters(point, geometry.first())
        return geometry.zipWithNext().minOf { (a, b) ->
            distanceToSegmentMeters(point, a, b)
        }
    }

    private fun distanceToSegmentMeters(point: MapPoint, a: MapPoint, b: MapPoint): Double {
        a.requireValid()
        b.requireValid()
        val lat0 = point.latitude.toRadians()
        fun project(other: MapPoint): Pair<Double, Double> {
            val x = (other.longitude - point.longitude).toRadians() * cos(lat0) * EARTH_RADIUS_METERS
            val y = (other.latitude - point.latitude).toRadians() * EARTH_RADIUS_METERS
            return x to y
        }
        val (ax, ay) = project(a)
        val (bx, by) = project(b)
        val dx = bx - ax
        val dy = by - ay
        val lengthSquared = dx * dx + dy * dy
        if (lengthSquared == 0.0) return sqrt(ax * ax + ay * ay)
        val t = max(0.0, min(1.0, -(ax * dx + ay * dy) / lengthSquared))
        val closestX = ax + t * dx
        val closestY = ay + t * dy
        return sqrt(closestX * closestX + closestY * closestY)
    }

    private fun Double.toRadians(): Double = this * PI / 180.0
}
