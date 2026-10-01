package com.servacode.directory.feature.navigation

import com.servacode.directory.core.maps.GeoMath
import com.servacode.directory.core.maps.ManeuverKind
import com.servacode.directory.core.maps.ManeuverModifier
import com.servacode.directory.core.maps.MapPoint
import com.servacode.directory.core.maps.NavigationRoute
import com.servacode.directory.core.maps.RouteManeuver
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * What the screen says is left of a trip, at the moment nobody has moved on it.
 *
 * The day the route chooser arrived this was visible on the device: the chip offering the way
 * said 10.10 km and the summary above the map said 6.3, on a trip not yet begun. The difference
 * was the whole first leg. The index names the turn to announce next — the departure is behind
 * you the instant you set off, so announcing it would be absurd — but the road between the kerb
 * and that turn is still entirely ahead, and the sum of the legs from that index does not
 * contain it. What is left is that road plus everything after the turn.
 */
class NavigationStartProgressTest {
    private val kerb = MapPoint(35.9500, 39.0050)
    private val turn = MapPoint(35.9600, 39.0180)
    private val shop = MapPoint(35.9650, 39.0300)

    /** Legs as long as the ground between the points, so the arithmetic can be checked. */
    private val firstLeg = GeoMath.distanceMeters(kerb, turn)
    private val secondLeg = GeoMath.distanceMeters(turn, shop)

    private val route = NavigationRoute(
        geometry = listOf(kerb, turn, shop),
        distanceMeters = firstLeg + secondLeg,
        durationSeconds = 600.0,
        maneuvers = listOf(
            RouteManeuver(
                kind = ManeuverKind.DEPART,
                modifier = ManeuverModifier.STRAIGHT,
                point = kerb,
                streetName = "شارع الفرات",
                distanceMeters = firstLeg,
                durationSeconds = 240.0,
            ),
            RouteManeuver(
                kind = ManeuverKind.TURN,
                modifier = ManeuverModifier.RIGHT,
                point = turn,
                streetName = "شارع تل أبيض",
                distanceMeters = secondLeg,
                durationSeconds = 360.0,
            ),
        ),
    )

    private fun progressAt(update: NavigationUpdate) =
        (update.state as NavigationState.Navigating).progress

    @Test
    fun `a trip that has not begun has all of itself left`() {
        val engine = NavigationEngine()

        val progress = progressAt(engine.start(route, kerb, nowMillis = 0L))

        assertEquals(firstLeg + secondLeg, progress.remainingDistanceMeters, 1.0)
        assertEquals(600.0, progress.remainingDurationSeconds, 1.0)
    }

    @Test
    fun `the turn to announce is the first turn, not the departure behind you`() {
        val engine = NavigationEngine()

        val progress = progressAt(engine.start(route, kerb, nowMillis = 0L))

        assertEquals(1, progress.maneuverIndex)
        assertEquals(firstLeg, progress.distanceToManeuverMeters, 1.0)
    }

    @Test
    fun `reaching the turn leaves the road after it`() {
        val engine = NavigationEngine()
        engine.start(route, kerb, nowMillis = 0L)

        val progress = progressAt(engine.update(turn, nowMillis = 240_000L))

        assertEquals(secondLeg, progress.remainingDistanceMeters, 1.0)
        assertEquals(360.0, progress.remainingDurationSeconds, 1.0)
    }

    @Test
    fun `half way along the first leg, half of it is left`() {
        val engine = NavigationEngine()
        engine.start(route, kerb, nowMillis = 0L)
        val halfway = MapPoint(
            (kerb.latitude + turn.latitude) / 2,
            (kerb.longitude + turn.longitude) / 2,
        )

        val progress = progressAt(engine.update(halfway, nowMillis = 120_000L))

        assertEquals(firstLeg / 2 + secondLeg, progress.remainingDistanceMeters, 30.0)
    }

    @Test
    fun `a route recomputed mid-trip also starts with all of itself left`() {
        val engine = NavigationEngine()
        engine.start(route, kerb, nowMillis = 0L)

        val progress = progressAt(engine.applyReroute(route, kerb, nowMillis = 30_000L))

        assertEquals(firstLeg + secondLeg, progress.remainingDistanceMeters, 1.0)
    }
}
