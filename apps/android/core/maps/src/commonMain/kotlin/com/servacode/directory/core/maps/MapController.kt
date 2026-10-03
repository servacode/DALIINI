package com.servacode.directory.core.maps

import com.servacode.directory.core.model.GeoPoint

data class MapPoint(val latitude: Double, val longitude: Double)

fun GeoPoint.toMapPoint() = MapPoint(latitude, longitude)

/**
 * Where the map looks from.
 *
 * [tilt] is the angle off straight down, in degrees. Zero is a plan, which is what a map of a
 * city wants; a navigator wants the road ahead to take more of the screen than the road behind,
 * and that is what tilting gives.
 */
data class MapCamera(
    val center: MapPoint,
    val zoom: Double,
    val bearing: Double = 0.0,
    val tilt: Double = 0.0,
)

data class FacilityMapPin(
    val facilityId: String,
    val point: MapPoint,
    val label: String,
    /**
     * The mark this facility's section wears, as a drawable this app already has.
     *
     * A map full of identical pins asks the reader to tap each one to find out what it is, and
     * asks someone who does not read to give up. A cross is a hospital, a mortar and pestle is
     * a pharmacy, and both are understood without a word. Null falls back to the plain pin.
     *
     * A drawable id rather than a name, because which drawable a section wears is the design
     * system's answer and this file is compiled without it — or the Android framework.
     */
    val iconRes: Int? = null,
)

interface MapController {
    /** Where the map is looking now, or null before it has a position. */
    val camera: MapCamera?
    /**
     * [durationMillis] is how long the animation takes. It exists because a navigator's camera
     * must arrive before the next reading does: a demonstration that reports five positions a
     * second with a one-second animation is a camera permanently catching up.
     */
    fun moveCamera(camera: MapCamera, animated: Boolean = true, durationMillis: Int = FOLLOW_MILLIS)

    /** Replaces the facility markers; the one with [selectedFacilityId] is shown selected. */
    fun showFacilities(pins: List<FacilityMapPin>, selectedFacilityId: String? = null)
    fun clearFacilities()

    /** A tap on a facility marker, or on the name shown above a selected one. */
    fun setOnFacilitySelected(listener: (String) -> Unit)
    fun showSelectionPoint(point: MapPoint)
    fun setOnPointSelected(listener: (MapPoint) -> Unit)
    fun showRoute(points: List<MapPoint>, colorArgb: Int)
    fun showNavigationLocation(point: MapPoint)
}

object MapStyle {
    /**
     * Whether a real map has been configured, or the build is still carrying the placeholder.
     *
     * A shipped build must be served over TLS, and the templated host is not a host at all.
     * Cleartext is allowed only where the flavor already allows it — the local one, which
     * serves the style and its tiles from this machine over `adb reverse` and cannot have a
     * certificate for `localhost`. Nothing but a loopback address qualifies, so this cannot
     * be used to point a real build at an unprotected server.
     */
    fun isConfigured(styleUrl: String): Boolean {
        if (styleUrl.contains("<ROOT_DOMAIN>")) return false
        if (styleUrl.startsWith("https://")) return true
        return LOOPBACK.any { styleUrl.startsWith("http://$it") }
    }

    /**
     * This machine, reached from the phone through `adb reverse`, and never anything else.
     *
     * Allowing these costs a shipped build nothing: `app/build.gradle.kts` refuses to assemble
     * a release whose endpoints are not HTTPS or that name any of these hosts, so a real build
     * can never carry a URL this branch would accept.
     */
    private val LOOPBACK = listOf("localhost", "127.0.0.1", "10.0.2.2")
}

/** What a camera animation lasts when nothing says otherwise: one reading's worth, in life. */
const val FOLLOW_MILLIS = 900
