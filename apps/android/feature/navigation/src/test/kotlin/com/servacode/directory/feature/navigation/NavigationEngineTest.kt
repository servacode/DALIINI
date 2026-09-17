package com.servacode.directory.feature.navigation

import com.servacode.directory.core.maps.ManeuverKind
import com.servacode.directory.core.maps.ManeuverModifier
import com.servacode.directory.core.maps.MapPoint
import com.servacode.directory.core.maps.NavigationRoute
import com.servacode.directory.core.maps.RouteManeuver
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationEngineTest {
    @Test
    fun `arrival threshold transitions to arrived`() {
        val engine = NavigationEngine(NavigationThresholds(arrivalMeters = 30.0))
        val route = route()
        val update = engine.start(route, MapPoint(35.9509, 39.0100), 1_000L)
        assertTrue(update.state is NavigationState.Arrived)
    }

    @Test
    fun `off route triggers reroute then cooldown suppresses immediate retry`() {
        val engine = NavigationEngine(
            NavigationThresholds(offRouteMeters = 20.0, rerouteCooldownMillis = 10_000L),
        )
        val route = route()
        engine.start(route, MapPoint(35.9500, 39.0000), 1_000L)
        val first = engine.update(MapPoint(35.9600, 39.0050), 2_000L)
        assertTrue(first.rerouteRequired)
        engine.rerouteFailed(2_100L)
        val second = engine.update(MapPoint(35.9600, 39.0060), 3_000L)
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
