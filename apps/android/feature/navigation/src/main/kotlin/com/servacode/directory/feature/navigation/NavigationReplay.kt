package com.servacode.directory.feature.navigation

import com.servacode.directory.core.maps.GeoMath
import com.servacode.directory.core.maps.MapPoint
import com.servacode.directory.core.maps.RoutingProfile
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * A trip made up, so guidance can be watched without anyone leaving the room.
 *
 * Live navigation is the one part of this app that cannot be proved at a desk: it needs a person
 * moving along a road for the map to follow, the maneuvers to advance and the voice to speak.
 * This makes readings that walk the route the engine actually returned, so the whole of it can be
 * seen — and heard — from a chair.
 *
 * Two things about it are deliberate, and both were learned the hard way in RahalGo's driver
 * navigation (`docs/design/RAHALGO-NAVIGATION-AUDIT.md`):
 *
 *  - **It walks the route's own geometry, never a straight line to the destination.** A straight
 *    line goes over houses, so the off-route detector calls it a detour on the first reading and
 *    the trip spends itself recomputing. A test that fails for that reason is read as a broken
 *    product.
 *  - **It moves at a street's speed, not a walker's.** Guidance times itself by how fast someone
 *    is going, and a bearing changes too little to be believed below a couple of metres a second.
 *    Someone testing on foot at 1.1 m/s sees an arrow that will not turn and a voice that will not
 *    speak, and concludes that nothing works.
 *
 * The readings are clean on purpose. The first thing worth proving is that the thing works at its
 * best; noise and vague fixes are what the engine's own tests already put it through.
 */
internal data class ReplayFix(
    val point: MapPoint,
    val bearingDegrees: Float,
    val speedMetersPerSecond: Float,
    val accuracyMeters: Float,
    val atMillis: Long,
)

internal object NavigationReplay {
    /** About 30 km/h: a motorcycle in a neighbourhood, and above every threshold that matters. */
    const val DEFAULT_SPEED_MPS = 8.3f

    /**
     * How fast the made-up traveller goes, by how they said they were travelling.
     *
     * A demonstration that walks a route at thirty kilometres an hour is a demonstration of a
     * motorcycle: guidance times itself by speed, so the voice would speak where it never
     * would on foot and the reader would be shown a trip that is not theirs. Walking here is a
     * brisk 5 km/h, which is what the engine's own thresholds are written against.
     */
    fun speedFor(profile: RoutingProfile): Float = when (profile) {
        RoutingProfile.WALKING -> 1.4f
        RoutingProfile.MOTORCYCLE -> DEFAULT_SPEED_MPS
        RoutingProfile.DRIVING -> 11.0f
    }

    /** One reading a second, which is the interval real guidance runs at. */
    const val DEFAULT_STEP_SECONDS = 1.0

    fun fixes(
        geometry: List<MapPoint>,
        speedMetersPerSecond: Float = DEFAULT_SPEED_MPS,
        stepSeconds: Double = DEFAULT_STEP_SECONDS,
        startMillis: Long = 0L,
    ): List<ReplayFix> {
        if (geometry.size < 2 || speedMetersPerSecond <= 0f || stepSeconds <= 0.0) return emptyList()

        val legs = DoubleArray(geometry.size - 1) { GeoMath.distanceMeters(geometry[it], geometry[it + 1]) }
        val total = legs.sum()
        if (total <= 0.0) return emptyList()

        val step = speedMetersPerSecond * stepSeconds
        val fixes = ArrayList<ReplayFix>((total / step).toInt() + 2)
        var leg = 0
        var travelledInLeg = 0.0
        var travelled = 0.0
        var at = startMillis

        while (travelled <= total && leg < legs.size) {
            val from = geometry[leg]
            val to = geometry[leg + 1]
            val fraction = if (legs[leg] <= 0.0) 0.0 else travelledInLeg / legs[leg]
            fixes += ReplayFix(
                point = MapPoint(
                    latitude = from.latitude + (to.latitude - from.latitude) * fraction,
                    longitude = from.longitude + (to.longitude - from.longitude) * fraction,
                ),
                bearingDegrees = bearing(from, to),
                speedMetersPerSecond = speedMetersPerSecond,
                // Sharp on purpose: the engine refuses to let a vague reading decide anything,
                // and a demonstration in which nothing is decided demonstrates nothing.
                accuracyMeters = 5f,
                atMillis = at,
            )
            at += (stepSeconds * 1000).toLong()
            travelled += step
            travelledInLeg += step
            // What overshoots one leg is spent on the next. Restarting each leg from its start
            // would make the speed depend on how finely the route happens to be drawn.
            while (leg < legs.size && travelledInLeg >= legs[leg]) {
                travelledInLeg -= legs[leg]
                leg += 1
            }
        }

        // The destination itself, said plainly. Stopping two metres short is not arriving, and
        // then the one thing the trip was run to see never happens.
        val last = geometry.last()
        fixes += ReplayFix(
            point = last,
            bearingDegrees = bearing(geometry[geometry.size - 2], last),
            speedMetersPerSecond = 0f,
            accuracyMeters = 5f,
            atMillis = at,
        )
        return fixes
    }

    /** From `a` towards `b`, in degrees clockwise from north. */
    private fun bearing(a: MapPoint, b: MapPoint): Float {
        val fromLatitude = Math.toRadians(a.latitude)
        val toLatitude = Math.toRadians(b.latitude)
        val deltaLongitude = Math.toRadians(b.longitude - a.longitude)
        val y = sin(deltaLongitude) * cos(toLatitude)
        val x = cos(fromLatitude) * sin(toLatitude) -
            sin(fromLatitude) * cos(toLatitude) * cos(deltaLongitude)
        return ((Math.toDegrees(atan2(y, x)) + 360.0) % 360.0).toFloat()
    }
}
