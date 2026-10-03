package com.servacode.directory.core.maps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The one part of drawing a route that fails silently when it is wrong.
 *
 * GeoJSON is longitude first and everything else in this app is latitude first. Swapped, a route
 * through Damascus still draws a perfectly good line — somewhere in the Indian Ocean — and no
 * exception is thrown to say so.
 */
class GeoJsonTest {
    private val damascus = MapPoint(latitude = 33.5138, longitude = 36.2765)
    private val nearby = MapPoint(latitude = 33.5020, longitude = 36.2910)

    @Test
    fun `a point is written longitude first`() {
        assertEquals(
            """{"type":"Feature","properties":{},"geometry":{"type":"Point","coordinates":[36.2765,33.5138]}}""",
            GeoJson.point(damascus),
        )
    }

    @Test
    fun `a line keeps its order and its orientation`() {
        assertEquals(
            """{"type":"Feature","properties":{},"geometry":{"type":"LineString","coordinates":""" +
                """[[36.2765,33.5138],[36.291,33.502]]}}""",
            GeoJson.lineString(listOf(damascus, nearby)),
        )
    }

    @Test
    fun `the longitude is the one that can exceed ninety`() {
        // A swap is invisible in Syria — both numbers are plausible — so this is the check that
        // catches it: the first number of a pair is the one allowed past ninety.
        val written = GeoJson.lineString(listOf(MapPoint(latitude = 10.0, longitude = 120.0)))
        assertTrue(written, written.contains("[120.0,10.0]"))
    }

    @Test
    fun `nothing to draw is an empty collection, not an empty string`() {
        // A source fed nothing keeps whatever it last drew, so "empty" has to be said explicitly.
        assertEquals("""{"type":"FeatureCollection","features":[]}""", GeoJson.EMPTY)
        assertTrue(GeoJson.lineString(emptyList()).contains(""""coordinates":[]"""))
    }
}
