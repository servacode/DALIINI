package com.servacode.directory.core.network

import com.servacode.directory.core.maps.GeocodingProvider
import com.servacode.directory.core.maps.GeocodingResult
import com.servacode.directory.core.maps.ManeuverKind
import com.servacode.directory.core.maps.ManeuverModifier
import com.servacode.directory.core.maps.MapPoint
import com.servacode.directory.core.maps.MapProviderConfig
import com.servacode.directory.core.maps.NavigationRoute
import com.servacode.directory.core.maps.ProviderEndpointPolicy
import com.servacode.directory.core.maps.RouteManeuver
import com.servacode.directory.core.maps.RoutingProfile
import com.servacode.directory.core.maps.RoutingProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import javax.inject.Inject
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MapProviderHttpClient

class MapProviderException(message: String, cause: Throwable? = null) : IOException(message, cause)

@Singleton
class OsrmRoutingProvider @Inject constructor(
    @MapProviderHttpClient private val client: OkHttpClient,
    private val config: MapProviderConfig,
) : RoutingProvider {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun route(
        origin: MapPoint,
        destination: MapPoint,
        profile: RoutingProfile,
    ): NavigationRoute = withContext(Dispatchers.IO) {
        origin.requireValidForProvider()
        destination.requireValidForProvider()
        val base = configuredUrl(config.routingBaseUrl)
        val coordinates = "${origin.longitude},${origin.latitude};" +
            "${destination.longitude},${destination.latitude}"
        val url = base.newBuilder()
            .addPathSegments("route/v1")
            .addPathSegment(profile.wireName)
            .addEncodedPathSegment(coordinates)
            .addQueryParameter("steps", "true")
            .addQueryParameter("geometries", "geojson")
            .addQueryParameter("overview", "full")
            .addQueryParameter("alternatives", "false")
            .build()
        val request = Request.Builder().url(url).get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw MapProviderException("Routing provider returned HTTP ${response.code}.")
            }
            val body = response.body.string()
            val payload = runCatching { json.decodeFromString<OsrmResponse>(body) }
                .getOrElse { throw MapProviderException("Routing response is invalid.", it) }
            if (payload.code != "Ok") {
                throw MapProviderException("Routing provider rejected the route: ${payload.code}.")
            }
            payload.routes.firstOrNull()?.toDomain()
                ?: throw MapProviderException("Routing provider returned no route.")
        }
    }

    private fun OsrmRoute.toDomain(): NavigationRoute {
        val geometryPoints = geometry.coordinates.map(::coordinateToPoint)
        val maneuvers = legs.flatMap { leg -> leg.steps.map { it.toDomain() } }
        return NavigationRoute(
            geometry = geometryPoints,
            distanceMeters = distance,
            durationSeconds = duration,
            maneuvers = maneuvers,
        )
    }

    private fun OsrmStep.toDomain(): RouteManeuver = RouteManeuver(
        kind = maneuver.type.toManeuverKind(maneuver.modifier),
        modifier = maneuver.modifier.toManeuverModifier(),
        point = coordinateToPoint(maneuver.location),
        streetName = name.trim().ifBlank { null },
        distanceMeters = distance,
        durationSeconds = duration,
        roundaboutExit = maneuver.exit,
    )
}

@Singleton
class NominatimGeocodingProvider @Inject constructor(
    @MapProviderHttpClient private val client: OkHttpClient,
    private val config: MapProviderConfig,
) : GeocodingProvider {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun forward(query: String, limit: Int): List<GeocodingResult> =
        withContext(Dispatchers.IO) {
            val normalized = query.trim()
            if (normalized.isBlank()) return@withContext emptyList()
            val safeLimit = limit.coerceIn(1, 10)
            val url = configuredUrl(config.geocodingBaseUrl).newBuilder()
                .addPathSegment("search")
                .addQueryParameter("q", normalized)
                .addQueryParameter("format", "jsonv2")
                .addQueryParameter("limit", safeLimit.toString())
                .addQueryParameter("accept-language", "ar")
                .build()
            val request = providerRequest(url.toString())
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw MapProviderException("Geocoding provider returned HTTP ${response.code}.")
                }
                val places = runCatching {
                    json.decodeFromString<List<NominatimPlace>>(response.body.string())
                }.getOrElse { throw MapProviderException("Geocoding response is invalid.", it) }
                places.mapNotNull(NominatimPlace::toDomainOrNull)
            }
        }

    override suspend fun reverse(point: MapPoint): GeocodingResult? = withContext(Dispatchers.IO) {
        point.requireValidForProvider()
        val url = configuredUrl(config.geocodingBaseUrl).newBuilder()
            .addPathSegment("reverse")
            .addQueryParameter("lat", point.latitude.toString())
            .addQueryParameter("lon", point.longitude.toString())
            .addQueryParameter("format", "jsonv2")
            .addQueryParameter("accept-language", "ar")
            .build()
        val request = providerRequest(url.toString())
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw MapProviderException("Geocoding provider returned HTTP ${response.code}.")
            }
            runCatching { json.decodeFromString<NominatimPlace>(response.body.string()) }
                .getOrElse { throw MapProviderException("Reverse geocoding response is invalid.", it) }
                .toDomainOrNull()
        }
    }

    private fun providerRequest(url: String): Request {
        val userAgent = config.geocodingUserAgent.trim()
        if (userAgent.isBlank() || '<' in userAgent || '>' in userAgent) {
            throw MapProviderException("Geocoding user agent is not configured.")
        }
        return Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .header("Accept", "application/json")
            .get()
            .build()
    }
}

private fun configuredUrl(raw: String) = ProviderEndpointPolicy.requireConfiguredHttps(raw)
    .trimEnd('/')
    .toHttpUrlOrNull()
    ?: throw MapProviderException("Map provider URL is invalid.")

private fun MapPoint.requireValidForProvider() {
    if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) {
        throw MapProviderException("Coordinate is outside the supported range.")
    }
}

private fun coordinateToPoint(values: List<Double>): MapPoint {
    if (values.size < 2) throw MapProviderException("Provider coordinate is incomplete.")
    return MapPoint(latitude = values[1], longitude = values[0]).also {
        it.requireValidForProvider()
    }
}

private fun String.toManeuverKind(modifier: String?): ManeuverKind = when (lowercase()) {
    "depart" -> ManeuverKind.DEPART
    "arrive" -> ManeuverKind.ARRIVE
    "turn" -> if (modifier == "uturn") ManeuverKind.UTURN else ManeuverKind.TURN
    "continue", "new name", "notification" -> ManeuverKind.CONTINUE
    "merge", "on ramp", "off ramp" -> ManeuverKind.MERGE
    "fork", "end of road" -> ManeuverKind.FORK
    "roundabout", "rotary", "roundabout turn" -> ManeuverKind.ROUNDABOUT
    else -> ManeuverKind.UNKNOWN
}

private fun String?.toManeuverModifier(): ManeuverModifier = when (this?.lowercase()) {
    "left" -> ManeuverModifier.LEFT
    "right" -> ManeuverModifier.RIGHT
    "slight left" -> ManeuverModifier.SLIGHT_LEFT
    "slight right" -> ManeuverModifier.SLIGHT_RIGHT
    "sharp left" -> ManeuverModifier.SHARP_LEFT
    "sharp right" -> ManeuverModifier.SHARP_RIGHT
    "straight" -> ManeuverModifier.STRAIGHT
    "uturn" -> ManeuverModifier.UTURN
    else -> ManeuverModifier.UNKNOWN
}

private fun NominatimPlace.toDomainOrNull(): GeocodingResult? {
    val latitude = lat.toDoubleOrNull() ?: return null
    val longitude = lon.toDoubleOrNull() ?: return null
    val point = MapPoint(latitude, longitude)
    return runCatching {
        point.requireValidForProvider()
        GeocodingResult(point = point, displayName = displayName.trim())
    }.getOrNull()
}

@Serializable
private data class OsrmResponse(
    val code: String,
    val routes: List<OsrmRoute> = emptyList(),
)

@Serializable
private data class OsrmRoute(
    val distance: Double,
    val duration: Double,
    val geometry: GeoJsonLineString,
    val legs: List<OsrmLeg> = emptyList(),
)

@Serializable
private data class GeoJsonLineString(
    val coordinates: List<List<Double>>,
)

@Serializable
private data class OsrmLeg(
    val steps: List<OsrmStep> = emptyList(),
)

@Serializable
private data class OsrmStep(
    val distance: Double,
    val duration: Double,
    val name: String = "",
    val maneuver: OsrmManeuver,
)

@Serializable
private data class OsrmManeuver(
    val type: String,
    val modifier: String? = null,
    val location: List<Double>,
    val exit: Int? = null,
)

@Serializable
private data class NominatimPlace(
    val lat: String,
    val lon: String,
    @SerialName("display_name") val displayName: String,
)
