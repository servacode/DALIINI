package com.servacode.directory.feature.navigation

import com.servacode.directory.core.maps.MapPoint
import com.servacode.directory.core.maps.NavigationRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** What the ongoing notice says while a trip runs with the screen off (DECISION-078). */
class GuidanceNoticeTest {
    private val here = MapPoint(35.9500, 39.0050)
    private val route = NavigationRoute(
        geometry = listOf(here, MapPoint(35.9600, 39.0180)),
        distanceMeters = 1_600.0,
        durationSeconds = 300.0,
        maneuvers = emptyList(),
    )

    private fun progress(meters: Double, seconds: Double) = NavigationProgress(
        route = route,
        location = here,
        maneuverIndex = 0,
        remainingDistanceMeters = meters,
        remainingDurationSeconds = seconds,
        offRouteDistanceMeters = 0.0,
    )

    @Test fun `distance rounds to fifty metres and minutes round up`() {
        val notice = guidanceNotice(NavigationState.Navigating(progress(1_234.0, 61.0)))

        assertEquals(GuidanceNotice(remainingMeters = 1_250, remainingMinutes = 2, rerouting = false), notice)
    }

    @Test fun `readings a few metres apart make the same notice`() {
        // So a reading every second does not redraw the shade every second.
        assertEquals(
            guidanceNotice(NavigationState.Navigating(progress(1_010.0, 200.0))),
            guidanceNotice(NavigationState.Navigating(progress(1_020.0, 199.0))),
        )
    }

    @Test fun `rerouting says so`() {
        val notice = guidanceNotice(NavigationState.Rerouting(progress(800.0, 120.0)))

        assertEquals(true, notice?.rerouting)
    }

    @Test fun `no trip under way, nothing to keep alive`() {
        assertNull(guidanceNotice(NavigationState.Idle))
        assertNull(guidanceNotice(NavigationState.Routing))
        assertNull(guidanceNotice(NavigationState.Arrived(route)))
        assertNull(guidanceNotice(NavigationState.Error("ROUTING_UNAVAILABLE")))
    }
}
