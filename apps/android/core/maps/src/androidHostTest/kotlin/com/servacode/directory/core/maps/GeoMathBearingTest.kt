package com.servacode.directory.core.maps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Where a made-up trip starts.
 *
 * A demonstration that begins fifty metres from the destination shows nothing: no turn, and no
 * room for the voice, whose earliest warning is a hundred and fifty metres out. So the start is
 * put up the road the traveller is coming from — which is this arithmetic, and it is worth being
 * exact about, because a bearing computed the wrong way round starts the trip on the far side of
 * the city.
 */
class GeoMathBearingTest {
    private val raqqa = MapPoint(35.9528, 39.0085)

    @Test fun `north is zero and east is ninety`() {
        val north = MapPoint(raqqa.latitude + 0.05, raqqa.longitude)
        val east = MapPoint(raqqa.latitude, raqqa.longitude + 0.05)

        assertEquals(0.0, GeoMath.bearingDegrees(raqqa, north), 0.5)
        assertEquals(90.0, GeoMath.bearingDegrees(raqqa, east), 0.5)
        assertEquals(180.0, GeoMath.bearingDegrees(north, raqqa), 0.5)
    }

    @Test fun `a point at a bearing is that far away, in that direction`() {
        val start = GeoMath.pointAtBearing(raqqa, bearingDegrees = 45.0, meters = 2_500.0)

        assertEquals(2_500.0, GeoMath.distanceMeters(raqqa, start), 5.0)
        assertEquals(45.0, GeoMath.bearingDegrees(raqqa, start), 0.5)
    }

    @Test fun `the demonstration starts up the road the traveller is coming from`() {
        // The reader is a few dozen metres away — the trip nobody can watch.
        val reader = MapPoint(35.9532, 39.0090)
        val away = GeoMath.bearingDegrees(raqqa, reader)

        val start = GeoMath.pointAtBearing(raqqa, away, 2_500.0)

        assertEquals(2_500.0, GeoMath.distanceMeters(raqqa, start), 5.0)
        // On the same side as the reader, not the opposite one.
        assertTrue(
            "the start must lie beyond the reader, not behind the destination",
            GeoMath.distanceMeters(reader, start) < GeoMath.distanceMeters(raqqa, start),
        )
    }

    @Test fun `it stays a valid point at the edges of the world`() {
        val far = GeoMath.pointAtBearing(MapPoint(89.9, 179.9), bearingDegrees = 30.0, meters = 40_000.0)

        far.requireValid()
        assertTrue(far.longitude >= -180.0 && far.longitude <= 180.0)
        assertTrue(far.latitude >= -90.0 && far.latitude <= 90.0)
    }
}
