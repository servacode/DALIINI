package com.servacode.directory.core.maps

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
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

/**
 * How the person is travelling, in the product's own words.
 *
 * These are the three ways someone gets to a facility here, and nothing in this enum names a
 * routing engine: which costing model or profile string each one becomes is the adapter's
 * business, and two adapters answer it differently.
 *
 * [wireName] is OSRM's profile vocabulary, which the legacy OSRM adapter puts in its path.
 * OSRM has no motorcycle profile at all, so it reads the same road network as a car.
 */
enum class RoutingProfile(val wireName: String) {
    DRIVING("driving"),
    WALKING("walking"),
    MOTORCYCLE("driving"),
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

/**
 * Why a route could not be given, in the few kinds a screen can actually say something about.
 *
 * A routing engine distinguishes "nothing near you is mapped" from "those two places are not
 * connected" from "I am not running". Someone standing in a village deserves the first answer
 * rather than being told the server is broken, so the distinction survives all the way up.
 */
enum class RoutingFailure {
    /** No engine answered: it is down, unreachable, or the request timed out. */
    UNREACHABLE,

    /** The endpoint is a placeholder, or names a costing the engine does not have. */
    NOT_CONFIGURED,

    /** The two places are real and mapped, but no way joins them for this mode. */
    NO_ROUTE,

    /** There is no road near one of the two points that this mode may travel on. */
    UNROUTABLE_POINT,

    /** Further than the engine will route for this mode. */
    TOO_FAR,

    /** A coordinate the engine or this app refuses as a point on earth. */
    INVALID_POINTS,

    /** The engine answered, but not with a route this app can read. */
    MALFORMED,

    /** The engine failed on its own side. */
    ENGINE_ERROR,
}

/**
 * A refusal with its reason kept.
 *
 * [detail] is for a log, never for a screen: it carries the engine's own status and code and
 * the costing that was asked for, and deliberately carries no coordinate — where a person was
 * standing is not a diagnostic.
 */
class RoutingException(
    val failure: RoutingFailure,
    val detail: String,
    cause: Throwable? = null,
) : java.io.IOException("$failure: $detail", cause)

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
    /**
     * The style the map draws, and the one an offline pack is fetched from.
     *
     * The screens are handed this by the app as a parameter, which is how it has always reached
     * them; it is here as well because a pack is created against a style URL and kept against it,
     * and the thing that creates one has no screen to be handed anything by.
     */
    val styleUrl: String = "",
)

class ProviderConfigurationException(message: String) : IllegalStateException(message)

object ProviderEndpointPolicy {
    private val forbiddenHosts = setOf(
        "router.project-osrm.org",
        "demotiles.maplibre.org",
    )

    /**
     * The hosts that are this machine, from the phone's side of an `adb reverse` and from an
     * emulator alike. Nothing outside the developer's own desk can answer on one of them.
     */
    private val loopbackHosts = setOf("localhost", "127.0.0.1", "10.0.2.2")

    fun requireConfiguredHttps(raw: String): String {
        val value = raw.trim()
        if (value.isBlank() || '<' in value || '>' in value) {
            throw ProviderConfigurationException("Map provider endpoint is not configured.")
        }
        // A routing engine that we host ourselves is reached over the loopback during
        // development, where there is no certificate to present and nothing in flight leaves
        // the machine. This is the same allowance the map style already makes, and it widens
        // nothing for staging or production: neither is addressed by a loopback host.
        val host = value.substringAfter("://", "").substringBefore('/').substringBefore(':').lowercase()
        val isLoopback = value.startsWith("http://") && host in loopbackHosts
        if (!value.startsWith("https://") && !isLoopback) {
            throw ProviderConfigurationException("Map provider endpoint must use HTTPS.")
        }
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

    /**
     * The part of the way that is still ahead.
     *
     * A line that stays whole while someone walks along it is a picture of a plan, not of a
     * trip: the traveller cannot tell what they have done from what is left. This cuts the
     * geometry at the segment they are nearest to and starts it where they are.
     */
    fun remainingGeometry(geometry: List<MapPoint>, from: MapPoint): List<MapPoint> {
        if (geometry.size < 2) return geometry
        var nearest = 0
        var shortest = Double.MAX_VALUE
        for (index in 0 until geometry.lastIndex) {
            val distance = distanceToSegmentMeters(from, geometry[index], geometry[index + 1])
            if (distance < shortest) {
                shortest = distance
                nearest = index
            }
        }
        val ahead = geometry.drop(nearest + 1)
        // Two points at least, or there is no line to draw and the map would blank the route.
        return if (ahead.isEmpty()) geometry.takeLast(2) else listOf(from) + ahead
    }

    /**
     * The compass bearing from one point to another, in degrees clockwise from north.
     *
     * Used to place a made-up starting point up the road the traveller is actually coming from,
     * and to point the arrow on the map the way it is moving.
     */
    fun bearingDegrees(from: MapPoint, to: MapPoint): Double {
        from.requireValid()
        to.requireValid()
        val fromLatitude = from.latitude.toRadians()
        val toLatitude = to.latitude.toRadians()
        val deltaLongitude = (to.longitude - from.longitude).toRadians()
        val y = sin(deltaLongitude) * cos(toLatitude)
        val x = cos(fromLatitude) * sin(toLatitude) -
            sin(fromLatitude) * cos(toLatitude) * cos(deltaLongitude)
        return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }

    /**
     * The point a given distance away along a bearing, on the sphere.
     *
     * It lands wherever the arithmetic puts it — in a field, on a roof — and that is fine: a
     * routing engine snaps a request to the nearest road, so what comes back is a real place on a
     * real street. Nothing here pretends to know where the roads are.
     */
    fun pointAtBearing(from: MapPoint, bearingDegrees: Double, meters: Double): MapPoint {
        from.requireValid()
        val angular = meters / EARTH_RADIUS_METERS
        val bearing = bearingDegrees.toRadians()
        val latitude = from.latitude.toRadians()
        val longitude = from.longitude.toRadians()
        val newLatitude = asin(
            sin(latitude) * cos(angular) + cos(latitude) * sin(angular) * cos(bearing),
        )
        val newLongitude = longitude + atan2(
            sin(bearing) * sin(angular) * cos(latitude),
            cos(angular) - sin(latitude) * sin(newLatitude),
        )
        return MapPoint(
            latitude = Math.toDegrees(newLatitude).coerceIn(-90.0, 90.0),
            longitude = ((Math.toDegrees(newLongitude) + 540.0) % 360.0) - 180.0,
        )
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
