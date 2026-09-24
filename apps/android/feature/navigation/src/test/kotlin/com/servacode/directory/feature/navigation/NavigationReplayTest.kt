package com.servacode.directory.feature.navigation

import com.servacode.directory.core.maps.GeoMath
import com.servacode.directory.core.maps.MapPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The made-up trip, checked for the two things that make it useless when they are wrong.
 *
 * If it strays from the route, the off-route detector calls it a detour and the demonstration
 * spends itself recomputing. If it moves too slowly, nothing that judges by speed ever fires and
 * the demonstration shows a product that looks broken but is not.
 */
class NavigationReplayTest {
    /** Three points in Damascus, a few streets apart, as a route's geometry would be. */
    private val geometry = listOf(
        MapPoint(33.5138, 36.2765),
        MapPoint(33.5100, 36.2800),
        MapPoint(33.5020, 36.2910),
    )

    @Test
    fun `every reading is on the route, not on a line drawn through the houses`() {
        val fixes = NavigationReplay.fixes(geometry)
        assertTrue("a route this long must produce many readings", fixes.size > 20)
        val worst = fixes.maxOf { GeoMath.distanceToPolylineMeters(it.point, geometry) }
        assertTrue("the worst reading was $worst m off the line", worst < 1.0)
    }

    @Test
    fun `it moves fast enough for everything that judges by speed`() {
        val fixes = NavigationReplay.fixes(geometry)
        // Everything but the last, which stands still at the destination on purpose.
        assertTrue(fixes.dropLast(1).all { it.speedMetersPerSecond >= 2f })
        assertEquals(0f, fixes.last().speedMetersPerSecond, 0.001f)
    }

    @Test
    fun `it ends at the destination itself, so arriving can actually happen`() {
        val last = NavigationReplay.fixes(geometry).last()
        assertEquals(0.0, GeoMath.distanceMeters(last.point, geometry.last()), 0.001)
    }

    @Test
    fun `it starts at the beginning and goes forward`() {
        val fixes = NavigationReplay.fixes(geometry)
        assertEquals(0.0, GeoMath.distanceMeters(fixes.first().point, geometry.first()), 0.001)
        val toEnd = fixes.map { GeoMath.distanceMeters(it.point, geometry.last()) }
        assertTrue("the trip must not go backwards", toEnd.zipWithNext().all { it.first >= it.second - 1.0 })
    }

    @Test
    fun `each reading carries a bearing, because one without forces a guess`() {
        assertTrue(NavigationReplay.fixes(geometry).all { it.bearingDegrees in 0f..360f })
        // Heading roughly south-east down this route, not stuck at zero.
        assertTrue(NavigationReplay.fixes(geometry).first().bearingDegrees > 90f)
    }

    @Test
    fun `the readings are sharp enough for the engine to act on`() {
        // A vague reading decides nothing — it cannot end the trip and cannot confirm a detour.
        // A demonstration in which nothing is decided demonstrates nothing.
        assertTrue(NavigationReplay.fixes(geometry).all { it.accuracyMeters <= 10f })
    }

    @Test
    fun `readings are one second apart in the trip's own time`() {
        val fixes = NavigationReplay.fixes(geometry, startMillis = 1_000L)
        assertEquals(1_000L, fixes.first().atMillis)
        assertTrue(fixes.map { it.atMillis }.zipWithNext().all { it.second - it.first == 1_000L })
    }

    @Test
    fun `a faster trip is a shorter list of readings`() {
        val slow = NavigationReplay.fixes(geometry, speedMetersPerSecond = 4f)
        val fast = NavigationReplay.fixes(geometry, speedMetersPerSecond = 16f)
        assertTrue(fast.size < slow.size)
    }

    @Test
    fun `nothing to walk produces nothing`() {
        assertTrue(NavigationReplay.fixes(emptyList()).isEmpty())
        assertTrue(NavigationReplay.fixes(listOf(geometry.first())).isEmpty())
        assertTrue(NavigationReplay.fixes(listOf(geometry.first(), geometry.first())).isEmpty())
        assertTrue(NavigationReplay.fixes(geometry, speedMetersPerSecond = 0f).isEmpty())
    }

    @Test
    fun `the engine driven by it arrives instead of deciding it has strayed`() {
        // The whole point, end to end: feed the made-up trip to the real engine and it must
        // follow the route, advance its maneuvers and arrive — never ask for a new route.
        val route = com.servacode.directory.core.maps.NavigationRoute(
            geometry = geometry,
            distanceMeters = 1500.0,
            durationSeconds = 180.0,
            maneuvers = listOf(
                com.servacode.directory.core.maps.RouteManeuver(
                    kind = com.servacode.directory.core.maps.ManeuverKind.DEPART,
                    modifier = com.servacode.directory.core.maps.ManeuverModifier.STRAIGHT,
                    point = geometry[0],
                    streetName = "شارع بغداد",
                    distanceMeters = 600.0,
                    durationSeconds = 70.0,
                ),
                com.servacode.directory.core.maps.RouteManeuver(
                    kind = com.servacode.directory.core.maps.ManeuverKind.TURN,
                    modifier = com.servacode.directory.core.maps.ManeuverModifier.LEFT,
                    point = geometry[1],
                    streetName = "شارع الثورة",
                    distanceMeters = 900.0,
                    durationSeconds = 110.0,
                ),
            ),
        )
        val engine = NavigationEngine()
        val fixes = NavigationReplay.fixes(geometry)
        var reroutes = 0
        var arrived = false
        engine.start(route, fixes.first().point, fixes.first().atMillis, fixes.first().accuracyMeters)
        fixes.forEach { fix ->
            val update = engine.update(fix.point, fix.atMillis, fix.accuracyMeters)
            if (update.rerouteRequired) reroutes += 1
            if (update.state is NavigationState.Arrived) arrived = true
        }
        assertEquals("a trip on its own route must never be called a detour", 0, reroutes)
        assertTrue("the trip must reach its destination", arrived)
    }
}
