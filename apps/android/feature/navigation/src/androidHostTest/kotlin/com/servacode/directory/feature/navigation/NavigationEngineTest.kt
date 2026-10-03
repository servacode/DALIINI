package com.servacode.directory.feature.navigation

import com.servacode.directory.core.maps.ManeuverKind
import com.servacode.directory.core.maps.ManeuverModifier
import com.servacode.directory.core.maps.MapPoint
import com.servacode.directory.core.maps.NavigationRoute
import com.servacode.directory.core.maps.RouteManeuver
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What the engine believes, and how much evidence it needs first.
 *
 * Two of these rules were learned from RahalGo's driver navigation, which has driven real roads:
 * a single reading never decides anything, and a reading's own accuracy is part of what it
 * means (`docs/design/RAHALGO-NAVIGATION-AUDIT.md`).
 */
class NavigationEngineTest {
    @Test
    fun `arrival threshold transitions to arrived`() {
        val engine = NavigationEngine(NavigationThresholds(arrivalMeters = 30.0))

        val update = engine.start(route(), MapPoint(35.9509, 39.0100), 1_000L)

        assertTrue(update.state is NavigationState.Arrived)
    }

    @Test
    fun `a vague reading cannot end the trip`() {
        val engine = NavigationEngine(
            NavigationThresholds(arrivalMeters = 30.0, unusableAccuracyMeters = 50f),
        )

        // The point is at the destination, but the phone admits it could be anywhere within
        // 120 m: that is not an arrival, it is a guess.
        val update = engine.start(route(), MapPoint(35.9509, 39.0100), 1_000L, accuracyMeters = 120f)

        assertFalse(update.state is NavigationState.Arrived)
    }

    @Test
    fun `a merely imprecise reading widens the arrival radius rather than being believed exactly`() {
        val engine = NavigationEngine(
            NavigationThresholds(arrivalMeters = 10.0, unusableAccuracyMeters = 60f),
        )

        // Forty metres short of the destination, with forty metres of uncertainty: close enough.
        val update = engine.start(route(), MapPoint(35.9506, 39.0100), 1_000L, accuracyMeters = 40f)

        assertTrue(update.state is NavigationState.Arrived)
    }

    @Test
    fun `leaving the route is believed only after it is confirmed`() {
        val engine = NavigationEngine(
            NavigationThresholds(offRouteMeters = 20.0, offRouteConfirmations = 3),
        )
        engine.start(route(), MapPoint(35.9500, 39.0000), 1_000L)

        val first = engine.update(MapPoint(35.9600, 39.0050), 2_000L)
        val second = engine.update(MapPoint(35.9600, 39.0060), 3_000L)
        val third = engine.update(MapPoint(35.9600, 39.0070), 4_000L)

        assertFalse(first.rerouteRequired)
        assertFalse(second.rerouteRequired)
        assertTrue(third.rerouteRequired)
    }

    @Test
    fun `one bad reading between good ones is not a detour`() {
        val engine = NavigationEngine(
            NavigationThresholds(offRouteMeters = 20.0, offRouteConfirmations = 3),
        )
        engine.start(route(), MapPoint(35.9500, 39.0000), 1_000L)

        engine.update(MapPoint(35.9600, 39.0050), 2_000L)
        engine.update(MapPoint(35.9600, 39.0060), 3_000L)
        // Back on the line: whatever that was, it was not a detour.
        engine.update(MapPoint(35.9505, 39.0050), 4_000L)
        val afterJitter = engine.update(MapPoint(35.9600, 39.0070), 5_000L)

        assertFalse(afterJitter.rerouteRequired)
    }

    @Test
    fun `a reading too vague to place anyone neither confirms nor clears a detour`() {
        val engine = NavigationEngine(
            NavigationThresholds(
                offRouteMeters = 20.0,
                offRouteConfirmations = 2,
                unusableAccuracyMeters = 50f,
            ),
        )
        engine.start(route(), MapPoint(35.9500, 39.0000), 1_000L)

        engine.update(MapPoint(35.9600, 39.0050), 2_000L)
        // Useless reading: it must not count towards the detour, and must not clear it either.
        val vague = engine.update(MapPoint(35.9505, 39.0050), 3_000L, accuracyMeters = 200f)
        val confirming = engine.update(MapPoint(35.9600, 39.0070), 4_000L)

        assertFalse(vague.rerouteRequired)
        assertTrue(confirming.rerouteRequired)
    }

    @Test
    fun `the cooldown still suppresses an immediate second reroute`() {
        val engine = NavigationEngine(
            NavigationThresholds(
                offRouteMeters = 20.0,
                offRouteConfirmations = 1,
                rerouteCooldownMillis = 10_000L,
            ),
        )
        engine.start(route(), MapPoint(35.9500, 39.0000), 1_000L)

        val first = engine.update(MapPoint(35.9600, 39.0050), 2_000L)
        engine.rerouteFailed(2_100L)
        val second = engine.update(MapPoint(35.9600, 39.0060), 3_000L)

        assertTrue(first.rerouteRequired)
        assertFalse(second.rerouteRequired)
    }

    private fun route() = NavigationRoute(
        geometry = listOf(MapPoint(35.9500, 39.0000), MapPoint(35.9510, 39.0100)),
        distanceMeters = 1_000.0,
        durationSeconds = 180.0,
        maneuvers = listOf(
            RouteManeuver(
                kind = ManeuverKind.DEPART,
                modifier = ManeuverModifier.STRAIGHT,
                point = MapPoint(35.9500, 39.0000),
                streetName = null,
                distanceMeters = 1_000.0,
                durationSeconds = 180.0,
            ),
        ),
    )
}
