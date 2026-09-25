package com.servacode.directory.core.maps

import kotlin.math.abs
import kotlin.math.asinh
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.tan

/**
 * The map a device keeps for itself, so that a trip does not end when the connection does.
 *
 * Syria's connection goes. A route already computed is guided from the phone — the engine walks
 * the line it was given and needs nobody — but the map under it is tiles fetched one by one, and
 * without them the guidance runs over an empty grey field. So the tiles around the reader's
 * province are fetched once, while there is a network to fetch them from, and kept.
 *
 * Nothing here talks to MapLibre or to Android: this is the arithmetic of which tiles and how
 * many, which is what decides whether the download is twenty megabytes or two hundred. It is
 * separated so the harness can check it, because getting it wrong is expensive on someone else's
 * data plan.
 */

/** The square of the world a pack covers, in degrees. */
data class MapPackBox(
    val north: Double,
    val east: Double,
    val south: Double,
    val west: Double,
) {
    init {
        require(north >= south) { "north is below south" }
        require(east >= west) { "east is west of west" }
    }
}

/**
 * The box around a point, measured in kilometres on the ground.
 *
 * A degree of latitude is 110.574 km wherever you stand. A degree of longitude is that same
 * distance multiplied by the cosine of the latitude — at Raqqa's 35.95° a degree of longitude is
 * 90 km, not 111 — so a box that is square in degrees is a tall rectangle on the ground. This
 * converts each side separately, which is what makes the pack cover the same distance in every
 * direction.
 *
 * The centre is pulled inside the projection before the box is measured, not after: a point past
 * the last latitude a Mercator map has would otherwise give a box whose north edge is south of its
 * south edge. A box that would cross the antimeridian is cut at it rather than wrapping, which no
 * province in this product does and which a tile pyramid cannot express anyway.
 */
fun mapPackBox(centre: MapPoint, radiusKm: Double = PACK_RADIUS_KM): MapPackBox {
    // A span wider than the world is the world; clamping it first is what keeps every range
    // below valid, whatever radius a caller asks for.
    val latitudeSpan = (radiusKm / KM_PER_LATITUDE_DEGREE).coerceIn(0.0, MAX_LATITUDE)
    val latitude = centre.latitude.coerceIn(-MAX_LATITUDE + latitudeSpan, MAX_LATITUDE - latitudeSpan)
    val shrink = max(cos(Math.toRadians(latitude)), MIN_COSINE)
    val longitudeSpan = (radiusKm / (KM_PER_LATITUDE_DEGREE * shrink)).coerceIn(0.0, 180.0)
    val longitude = centre.longitude.coerceIn(-180.0 + longitudeSpan, 180.0 - longitudeSpan)
    return MapPackBox(
        north = min(latitude + latitudeSpan, MAX_LATITUDE),
        south = max(latitude - latitudeSpan, -MAX_LATITUDE),
        east = min(longitude + longitudeSpan, 180.0),
        west = max(longitude - longitudeSpan, -180.0),
    )
}

/**
 * How far around the province's centre the pack reaches.
 *
 * Twenty-five kilometres covers a Syrian provincial city and the roads out of it, which is where
 * the facilities in this directory are and where a trip in it runs. It is a compromise with
 * someone's data: the cost of a pack grows with the square of this number, so fifty kilometres
 * would not be twice the download but four times it.
 */
const val PACK_RADIUS_KM = 25.0

/**
 * The zooms the pack holds.
 *
 * Six shows the province as a shape, which is what a reader sees when they pull back. Fourteen
 * shows streets as streets — enough to follow a route and recognise a junction. Fifteen would
 * quadruple the download for labels on alleys, and the map still works above fourteen: it draws
 * the fourteen tile scaled up, which is how every offline map behaves at its edge.
 */
const val PACK_MIN_ZOOM = 6
const val PACK_MAX_ZOOM = 14

/**
 * How many tiles that is.
 *
 * The count is what the download costs and what MapLibre's own limit is measured in, so it is
 * worth knowing before asking anyone to wait for it. Slippy-map indices: longitude is linear,
 * latitude is the Mercator projection, and a box is the rectangle of indices its corners fall in.
 */
fun packTileCount(
    box: MapPackBox,
    minZoom: Int = PACK_MIN_ZOOM,
    maxZoom: Int = PACK_MAX_ZOOM,
): Long {
    var total = 0L
    for (zoom in minZoom..maxZoom) {
        val side = 1L shl zoom
        val left = tileX(box.west, zoom)
        val right = tileX(box.east, zoom)
        val top = tileY(box.north, zoom)
        val bottom = tileY(box.south, zoom)
        val across = (right - left + 1).coerceIn(1, side)
        val down = (bottom - top + 1).coerceIn(1, side)
        total += across * down
    }
    return total
}

/**
 * Roughly what the pack weighs, for telling someone the cost before they pay it.
 *
 * An estimate and nothing more: a tile of empty desert is a few hundred bytes and a tile of
 * central Damascus is a hundred kilobytes. Once a download starts, MapLibre reports the real
 * figure and that is what the screen shows instead.
 */
fun packEstimatedBytes(box: MapPackBox, minZoom: Int = PACK_MIN_ZOOM, maxZoom: Int = PACK_MAX_ZOOM): Long =
    packTileCount(box, minZoom, maxZoom) * AVERAGE_TILE_BYTES

/**
 * A vector tile of this style, as served.
 *
 * Measured, not guessed: twenty-nine tiles at zooms 10 to 14 around Raqqa, read from the local
 * store that serves the same cartography the app draws, came to a mean of 28.9 kB and a median of
 * 19.7 kB — the mean is the higher of the two because a tile of the city centre is five times a
 * tile of the country around it. The tiles are stored and served gzipped, so this is what the pack
 * weighs on the device rather than what it would weigh uncompressed.
 */
const val AVERAGE_TILE_BYTES = 28_000L

/** Where a pack stands. */
sealed interface MapPackState {
    /** Nothing has been kept yet. */
    data object Absent : MapPackState

    /** The province has no point to centre a pack on, and no position has been read either. */
    data object Unknown : MapPackState

    data class Downloading(
        /**
         * Resources fetched so far, and resources needed.
         *
         * Resources rather than tiles: the engine counts the style's fonts and icons alongside
         * them, and it is the engine's own numbers that move while a download runs. [packTileCount]
         * is what the cost is estimated from before one starts.
         */
        val completed: Long,
        val required: Long,
        /** Bytes on the device, as the engine has written them. */
        val bytes: Long,
        /**
         * Whether it is moving.
         *
         * A pack half fetched and then stopped — the app was closed, the network went, the reader
         * was on their own data — keeps what it has. Saying "not downloaded" to someone who has
         * already spent twenty megabytes on it would be a lie, and would invite them to spend
         * them again, so a stopped download says so and offers to carry on.
         */
        val running: Boolean = true,
    ) : MapPackState {
        /**
         * How far along, or null while the engine has not yet counted what it needs.
         *
         * MapLibre reports a required count that is an estimate until it has read the style's own
         * sources, and a fraction computed from an estimate that then grows runs backwards. Null
         * says "working" and the screen says that rather than a number it would have to take back.
         */
        val fraction: Float?
            get() = if (required <= 0) null else (completed.toFloat() / required).coerceIn(0f, 1f)
    }

    /** Kept, and this is what it took. */
    data class Ready(val tiles: Long, val bytes: Long) : MapPackState

    /** Stopped, for a reason worth telling the reader about. */
    data class Failed(val reason: MapPackFailure) : MapPackState
}

/**
 * Why a pack stopped.
 *
 * Named rather than worded, like every other decision in this app: the sentences are in the
 * design system's resources.
 */
enum class MapPackFailure {
    /** The network went before the pack was finished. It resumes by itself. */
    CONNECTION,

    /** The tile server refused. */
    SERVER,

    /** More tiles than the engine is allowed to keep — the box or the zooms are too generous. */
    TILE_LIMIT,

    OTHER,
}

/** What the app should do about the pack. */
enum class MapPackAction { NOTHING, DOWNLOAD, PAUSE }

/**
 * Whether to fetch the province's map now, stop fetching it, or leave it alone.
 *
 * The whole policy, in one function with no Android in it, because it is the part that spends
 * someone's money:
 *
 * * nothing at all once the reader has said they do not want the map;
 * * fetch only on a connection nobody pays by the megabyte for — a pack is tens of megabytes;
 * * carry on from a download that stopped, because what arrived is kept and starting over is paid
 *   for twice;
 * * stop a download the *app* started when the free connection goes, and never stop one the reader
 *   asked for, because they asked for it;
 * * try again after a connection failed, and not after the server refused or the pack was too
 *   large — those come back the same way however often they are asked.
 */
fun mapPackAction(
    state: MapPackState,
    unmetered: Boolean,
    wanted: Boolean,
    startedByApp: Boolean,
): MapPackAction {
    if (!wanted) return MapPackAction.NOTHING
    return when (state) {
        MapPackState.Unknown, is MapPackState.Ready -> MapPackAction.NOTHING
        MapPackState.Absent -> if (unmetered) MapPackAction.DOWNLOAD else MapPackAction.NOTHING
        is MapPackState.Downloading -> when {
            !state.running && unmetered -> MapPackAction.DOWNLOAD
            state.running && !unmetered && startedByApp -> MapPackAction.PAUSE
            else -> MapPackAction.NOTHING
        }
        is MapPackState.Failed -> when (state.reason) {
            MapPackFailure.CONNECTION -> if (unmetered) MapPackAction.DOWNLOAD else MapPackAction.NOTHING
            MapPackFailure.SERVER, MapPackFailure.TILE_LIMIT, MapPackFailure.OTHER -> MapPackAction.NOTHING
        }
    }
}

/** Which province's pack, and the point it is centred on. */
data class MapPackTarget(val provinceId: String, val centre: MapPoint) {
    val box: MapPackBox get() = mapPackBox(centre)
}

private fun tileX(longitude: Double, zoom: Int): Long {
    val side = 1L shl zoom
    val fraction = (longitude + 180.0) / 360.0
    return floor(fraction * side).toLong().coerceIn(0, side - 1)
}

private fun tileY(latitude: Double, zoom: Int): Long {
    val side = 1L shl zoom
    val clamped = latitude.coerceIn(-MAX_LATITUDE, MAX_LATITUDE)
    val radians = Math.toRadians(clamped)
    // asinh(tan(lat)) is the Mercator y, and the same number as ln(tan + sec) without the
    // cancellation error near the equator.
    val mercator = asinh(tan(radians))
    val fraction = (1.0 - mercator / Math.PI) / 2.0
    return floor(fraction * side).toLong().coerceIn(0, side - 1)
}

/** One degree of latitude, in kilometres: the mean meridian degree. */
private const val KM_PER_LATITUDE_DEGREE = 110.574

/** Past the poles the projection has no answer, and neither has a map. */
private const val MAX_LATITUDE = 85.05112878

/** Near a pole the cosine goes to nothing and a box would swallow the world. */
private const val MIN_COSINE = 0.01

/** Whether two boxes are the same ground, allowing for the last digits of a stored double. */
fun MapPackBox.sameAs(other: MapPackBox): Boolean =
    abs(north - other.north) < BOX_TOLERANCE &&
        abs(south - other.south) < BOX_TOLERANCE &&
        abs(east - other.east) < BOX_TOLERANCE &&
        abs(west - other.west) < BOX_TOLERANCE

private const val BOX_TOLERANCE = 1e-6
