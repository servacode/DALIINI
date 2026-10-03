package com.servacode.directory.feature.navigation

import com.servacode.directory.core.maps.MapPoint
import com.servacode.directory.core.maps.RoutingProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A demonstration travels the way the reader said they would travel.
 *
 * Walking a route at thirty kilometres an hour demonstrates a motorcycle: guidance times itself
 * by speed, so the voice would speak where it never would on foot, and the trip shown would not
 * be the trip asked for.
 */
class NavigationReplaySpeedTest {
    private val way = listOf(MapPoint(35.950, 39.000), MapPoint(35.950, 39.010))

    @Test fun `walking is a walk, and driving is faster than a motorcycle in a neighbourhood`() {
        val walking = NavigationReplay.speedFor(RoutingProfile.WALKING)
        val motorcycle = NavigationReplay.speedFor(RoutingProfile.MOTORCYCLE)
        val driving = NavigationReplay.speedFor(RoutingProfile.DRIVING)

        assertTrue("a walk is metres a second, not tens of them", walking < 2f)
        assertTrue(motorcycle > walking)
        assertTrue(driving > motorcycle)
    }

    @Test fun `a walk takes more readings over the same way than a drive`() {
        val onFoot = NavigationReplay.fixes(way, NavigationReplay.speedFor(RoutingProfile.WALKING))
        val byCar = NavigationReplay.fixes(way, NavigationReplay.speedFor(RoutingProfile.DRIVING))

        assertTrue("a walk is reported for longer", onFoot.size > byCar.size)
        // Both end where the way ends: a trip that stops short never arrives, and arrival is
        // the one thing a demonstration is run to see.
        assertEquals(way.last(), onFoot.last().point)
        assertEquals(way.last(), byCar.last().point)
    }
}
