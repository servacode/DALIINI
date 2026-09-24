package com.servacode.directory.core.network

import com.servacode.directory.core.maps.ManeuverKind
import com.servacode.directory.core.maps.ManeuverModifier
import com.servacode.directory.core.maps.MapPoint
import com.servacode.directory.core.maps.MapProviderConfig
import com.servacode.directory.core.maps.NavigationRoute
import com.servacode.directory.core.maps.ProviderEndpointPolicy
import com.servacode.directory.core.maps.RouteManeuver
import com.servacode.directory.core.maps.RoutingException
import com.servacode.directory.core.maps.RoutingFailure
import com.servacode.directory.core.maps.RoutingProfile
import com.servacode.directory.core.maps.RoutingProvider
import com.servacode.directory.core.observability.Observability
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Valhalla: the engine every route in this app is computed by.
 *
 * It is ours, it runs on our own machines over Syria's OpenStreetMap extract alone, and it is
 * the reason the three travel modes are three real answers rather than one answer relabelled:
 * OSRM compiles a single profile into its graph, so asking it to walk means rebuilding it.
 *
 * Everything Valhalla-shaped stops at this file. The domain asks for [RoutingProfile.WALKING]
 * and is given a [NavigationRoute]; which costing model that became, what units came back and
 * how the geometry was packed are answered here and nowhere else.
 */
@Singleton
class ValhallaRoutingProvider @Inject constructor(
    @MapProviderHttpClient private val client: OkHttpClient,
    private val config: MapProviderConfig,
    private val observability: Observability,
) : RoutingProvider {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    override suspend fun route(
        origin: MapPoint,
        destination: MapPoint,
        profile: RoutingProfile,
    ): NavigationRoute = withContext(Dispatchers.IO) {
        val from = origin.requireRoutable()
        val to = destination.requireRoutable()
        val base = runCatching { ProviderEndpointPolicy.requireConfiguredHttps(config.routingBaseUrl) }
            .getOrElse { throw failure(RoutingFailure.NOT_CONFIGURED, "routing endpoint rejected", it) }
            .trimEnd('/')
            .toHttpUrlOrNull()
            ?: throw failure(RoutingFailure.NOT_CONFIGURED, "routing endpoint is not a URL")

        val body = json.encodeToString(
            ValhallaRequest.serializer(),
            ValhallaRequest(
                locations = listOf(
                    ValhallaLocation(from.latitude, from.longitude),
                    ValhallaLocation(to.latitude, to.longitude),
                ),
                costing = profile.costing(),
            ),
        )
        val request = Request.Builder()
            .url(base.newBuilder().addPathSegment("route").build())
            .post(body.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        // Anything the transport itself refuses — no engine listening, a name that will not
        // resolve, a socket that times out — is one condition to the person holding the phone:
        // the route could not be asked for.
        val response = runCatching { client.newCall(request).execute() }
            .getOrElse { throw failure(RoutingFailure.UNREACHABLE, "routing engine not reachable", it) }

        response.use {
            val payload = it.body.string()
            if (!it.isSuccessful) throw rejected(it.code, payload, profile)
            val decoded = runCatching { json.decodeFromString(ValhallaResponse.serializer(), payload) }
                .getOrElse { cause ->
                    throw failure(RoutingFailure.MALFORMED, "route response is not JSON", cause)
                }
            val trip = decoded.trip
                ?: throw rejected(it.code, payload, profile)
            trip.toDomain()
        }
    }

    /**
     * Which costing model each of our three modes is, and why.
     *
     * `pedestrian` and `auto` are the stable, obvious answers. The motorcycle is the one that
     * needed deciding, because Valhalla 3.9.0 offers two candidates:
     *
     *  - `motor_scooter` is stable, but its own documentation says it avoids higher class roads
     *    and it defaults to a 45 km/h top speed. That models a moped. Sending a motorcyclist in
     *    a Syrian city down side streets to keep off the arterial roads is the wrong route.
     *  - `motorcycle` is marked BETA, which in Valhalla means its tuning options are newer — not
     *    that it is absent. It is listed as a full motorised costing everywhere else in the same
     *    reference (hierarchy pruning, closure factor, track and service-road penalties), and
     *    `travel_type: "motorcycle"` is a documented response value.
     *
     * So: `motorcycle`. Verified against the 3.9.0 route API reference, which is the version
     * scripts/valhalla.sh pins.
     */
    private fun RoutingProfile.costing(): String = when (this) {
        RoutingProfile.WALKING -> "pedestrian"
        RoutingProfile.MOTORCYCLE -> "motorcycle"
        RoutingProfile.DRIVING -> "auto"
    }

    private fun ValhallaTrip.toDomain(): NavigationRoute {
        val geometry = legs.flatMapIndexed { index, leg ->
            val points = decodePolyline6(leg.shape)
            // Consecutive legs repeat the point they meet at; one route wants it once.
            if (index == 0) points else points.drop(1)
        }
        if (geometry.size < 2) {
            throw failure(RoutingFailure.MALFORMED, "route geometry has fewer than two points")
        }
        val toMeters = units.metersPerUnit()
        val maneuvers = mutableListOf<RouteManeuver>()
        var consumed = 0
        legs.forEachIndexed { index, leg ->
            val offset = if (index == 0) 0 else consumed - 1
            leg.maneuvers.forEach { maneuvers += it.toDomain(geometry, offset, toMeters) }
            consumed += decodePolyline6(leg.shape).size
        }
        return NavigationRoute(
            geometry = geometry,
            distanceMeters = (summary.length * toMeters).coerceAtLeast(0.0),
            durationSeconds = summary.time.coerceAtLeast(0.0),
            maneuvers = maneuvers,
        )
    }

    private fun ValhallaManeuver.toDomain(
        geometry: List<MapPoint>,
        offset: Int,
        toMeters: Double,
    ): RouteManeuver = RouteManeuver(
        kind = type.maneuverKind(),
        modifier = type.maneuverModifier(),
        point = geometry[(beginShapeIndex + offset).coerceIn(geometry.indices)],
        streetName = streetNames.firstOrNull()?.trim()?.ifBlank { null },
        distanceMeters = (length * toMeters).coerceAtLeast(0.0),
        durationSeconds = time.coerceAtLeast(0.0),
        roundaboutExit = roundaboutExitCount,
    )

    /**
     * What the engine refused, in terms the screen can act on.
     *
     * Valhalla answers a refusal with its own numeric code, which distinguishes "there is no
     * road here" from "you are asking to cross half a continent" from "that is not a costing".
     * Collapsing them all into one message would tell someone standing in a village that the
     * server is broken when the truth is that nothing near them is mapped.
     */
    private fun rejected(httpStatus: Int, payload: String, profile: RoutingProfile): RoutingException {
        val error = runCatching { json.decodeFromString(ValhallaResponse.serializer(), payload) }.getOrNull()
        val code = error?.errorCode
        val kind = when {
            code == null && httpStatus >= 500 -> RoutingFailure.ENGINE_ERROR
            code == null -> RoutingFailure.MALFORMED
            code in NO_ROUTE_CODES -> RoutingFailure.NO_ROUTE
            code in UNROUTABLE_POINT_CODES -> RoutingFailure.UNROUTABLE_POINT
            code == TOO_FAR_CODE -> RoutingFailure.TOO_FAR
            code in BAD_COSTING_CODES -> RoutingFailure.NOT_CONFIGURED
            code in BAD_LOCATION_CODES -> RoutingFailure.INVALID_POINTS
            httpStatus >= 500 -> RoutingFailure.ENGINE_ERROR
            else -> RoutingFailure.NO_ROUTE
        }
        // The costing and the engine's own code, which is what a developer needs to read.
        // No coordinate is recorded: where a person was standing is not a diagnostic.
        return failure(kind, "http $httpStatus, engine code ${code ?: "none"}, costing ${profile.costing()}")
    }

    private fun failure(kind: RoutingFailure, detail: String, cause: Throwable? = null): RoutingException {
        observability.recordError("routing.${kind.name.lowercase()}")
        return RoutingException(kind, detail, cause)
    }

    private fun MapPoint.requireRoutable(): MapPoint {
        if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0 ||
            latitude.isNaN() || longitude.isNaN()
        ) {
            throw failure(RoutingFailure.INVALID_POINTS, "coordinate outside the supported range")
        }
        return this
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        // From the 3.9.0 reference's internal error code table.
        val NO_ROUTE_CODES = setOf(170, 441, 442, 443)
        val UNROUTABLE_POINT_CODES = setOf(171, 172)
        val BAD_COSTING_CODES = setOf(124, 125, 412)
        val BAD_LOCATION_CODES = setOf(130, 131, 132, 420, 421, 422, 423)
        const val TOO_FAR_CODE = 154
    }
}

/** Valhalla reports length in whatever units were asked for; the domain speaks only metres. */
private fun String.metersPerUnit(): Double = when (lowercase()) {
    "mi", "miles" -> 1609.344
    else -> 1000.0
}

/**
 * Valhalla's geometry, which is not OSRM's.
 *
 * The same encoded-polyline algorithm, but at six decimal places rather than five — a route
 * decoded with the five-place divisor lands a tenth of the way to the equator from where it
 * belongs, which is why this is written out rather than borrowed.
 */
internal fun decodePolyline6(encoded: String): List<MapPoint> {
    if (encoded.isEmpty()) return emptyList()
    val points = ArrayList<MapPoint>(encoded.length / 4)
    var index = 0
    var latitude = 0
    var longitude = 0
    while (index < encoded.length) {
        latitude += encoded.readVarint(index) { index = it }
        if (index >= encoded.length) throw MalformedPolylineException("longitude is missing")
        longitude += encoded.readVarint(index) { index = it }
        points += MapPoint(latitude / 1e6, longitude / 1e6)
    }
    return points
}

private inline fun String.readVarint(start: Int, advanced: (Int) -> Unit): Int {
    var index = start
    var result = 0
    var shift = 0
    var byte: Int
    do {
        if (index >= length) throw MalformedPolylineException("truncated")
        byte = this[index++].code - 63
        if (byte < 0 || shift > 30) throw MalformedPolylineException("not an encoded polyline")
        result = result or ((byte and 0x1f) shl shift)
        shift += 5
    } while (byte >= 0x20)
    advanced(index)
    return if (result and 1 != 0) (result shr 1).inv() else result shr 1
}

internal class MalformedPolylineException(message: String) : IOException(message)

/** Valhalla's maneuver type carries the direction inside it; OSRM kept the two apart. */
internal fun Int.maneuverKind(): ManeuverKind = when (this) {
    1, 2, 3 -> ManeuverKind.DEPART
    4, 5, 6 -> ManeuverKind.ARRIVE
    7, 8, 17, 22 -> ManeuverKind.CONTINUE
    9, 10, 11, 14, 15, 16 -> ManeuverKind.TURN
    12, 13 -> ManeuverKind.UTURN
    18, 19, 20, 21, 25, 37, 38 -> ManeuverKind.MERGE
    23, 24 -> ManeuverKind.FORK
    26, 27 -> ManeuverKind.ROUNDABOUT
    else -> ManeuverKind.UNKNOWN
}

internal fun Int.maneuverModifier(): ManeuverModifier = when (this) {
    2, 5, 10, 18, 20, 23, 37 -> ManeuverModifier.RIGHT
    3, 6, 15, 19, 21, 24, 38 -> ManeuverModifier.LEFT
    9 -> ManeuverModifier.SLIGHT_RIGHT
    16 -> ManeuverModifier.SLIGHT_LEFT
    11 -> ManeuverModifier.SHARP_RIGHT
    14 -> ManeuverModifier.SHARP_LEFT
    12, 13 -> ManeuverModifier.UTURN
    1, 4, 7, 8, 17, 22, 25 -> ManeuverModifier.STRAIGHT
    else -> ManeuverModifier.UNKNOWN
}

@Serializable
private data class ValhallaRequest(
    val locations: List<ValhallaLocation>,
    val costing: String,
    /** Asked for explicitly so the adapter never has to guess what `length` means. */
    val units: String = "kilometers",
)

@Serializable
private data class ValhallaLocation(val lat: Double, val lon: Double)

@Serializable
private data class ValhallaResponse(
    val trip: ValhallaTrip? = null,
    @SerialName("error_code") val errorCode: Int? = null,
    val error: String? = null,
)

@Serializable
private data class ValhallaTrip(
    val units: String = "kilometers",
    val summary: ValhallaSummary = ValhallaSummary(),
    val legs: List<ValhallaLeg> = emptyList(),
)

@Serializable
private data class ValhallaSummary(
    val time: Double = 0.0,
    val length: Double = 0.0,
)

@Serializable
private data class ValhallaLeg(
    val shape: String = "",
    val maneuvers: List<ValhallaManeuver> = emptyList(),
)

@Serializable
private data class ValhallaManeuver(
    val type: Int = 0,
    @SerialName("street_names") val streetNames: List<String> = emptyList(),
    val time: Double = 0.0,
    val length: Double = 0.0,
    @SerialName("begin_shape_index") val beginShapeIndex: Int = 0,
    @SerialName("roundabout_exit_count") val roundaboutExitCount: Int? = null,
)
