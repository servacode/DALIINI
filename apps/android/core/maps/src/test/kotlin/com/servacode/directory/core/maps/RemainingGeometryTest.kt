package com.servacode.directory.core.maps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The line shrinks behind the traveller.
 *
 * Drawn whole, a route is a picture of a plan: someone walking it cannot tell what they have
 * covered from what is left. These hold the one rule that makes it a picture of a trip — what
 * is behind is gone, what is ahead starts where they are.
 */
class RemainingGeometryTest {
    private val way = listOf(
        MapPoint(35.950, 39.000),
        MapPoint(35.950, 39.010),
        MapPoint(35.950, 39.020),
        MapPoint(35.950, 39.030),
    )

    @Test fun `at the start the whole way is still ahead`() {
        val remaining = GeoMath.remainingGeometry(way, MapPoint(35.950, 39.000))

        assertEquals(way.size, remaining.size)
        assertEquals(way.last(), remaining.last())
    }

    @Test fun `half way along, what is behind is gone`() {
        val here = MapPoint(35.950, 39.015)

        val remaining = GeoMath.remainingGeometry(way, here)

        assertEquals(here, remaining.first())
        assertEquals(way.last(), remaining.last())
        assertTrue("the covered points are dropped", remaining.size < way.size)
    }

    @Test fun `near the end there is still a line to draw`() {
        val remaining = GeoMath.remainingGeometry(way, MapPoint(35.950, 39.0299))

        assertTrue("a line needs two points", remaining.size >= 2)
        assertEquals(way.last(), remaining.last())
    }
}
