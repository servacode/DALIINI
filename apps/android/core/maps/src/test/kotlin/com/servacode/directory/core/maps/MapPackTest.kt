package com.servacode.directory.core.maps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.cos

/**
 * What a kept map costs.
 *
 * These numbers are somebody's data plan in Syria, so they are asserted rather than trusted: a box
 * that is quietly twice as wide, or a zoom range one step deeper, is a download four times the
 * size and nobody would notice in a screenshot.
 */
class MapPackTest {
    // Raqqa, the province this build opens on.
    private val raqqa = MapPoint(35.9500, 39.0100)

    @Test fun `the box is the asked-for distance in every direction, not in every degree`() {
        val box = mapPackBox(raqqa, radiusKm = 25.0)
        val latitudeKm = (box.north - raqqa.latitude) * 110.574
        val longitudeKm = (box.east - raqqa.longitude) * 110.574 * cos(Math.toRadians(raqqa.latitude))

        assertEquals(25.0, latitudeKm, 0.5)
        assertEquals(25.0, longitudeKm, 0.5)
        // A degree of longitude here is 90 km, so the box is wider in degrees than it is tall.
        assertTrue(
            "longitude span must exceed latitude span at this latitude",
            (box.east - box.west) > (box.north - box.south),
        )
    }

    @Test fun `the province pack stays within what a phone and a data plan can take`() {
        val tiles = packTileCount(mapPackBox(raqqa))
        val megabytes = packEstimatedBytes(mapPackBox(raqqa)) / 1_000_000.0

        // The engine's own default ceiling is 6000 tiles; staying an order below it leaves room
        // for a second province without anyone rebuilding this.
        assertTrue("$tiles tiles is too many", tiles < 1_500)
        assertTrue("$tiles tiles is suspiciously few", tiles > 200)
        assertTrue("$megabytes MB is too much to ask for", megabytes < 45)
    }

    @Test fun `one zoom deeper costs about four times as much, which is why fourteen is the end`() {
        val box = mapPackBox(raqqa)
        val toFourteen = packTileCount(box, PACK_MIN_ZOOM, 14)
        val toFifteen = packTileCount(box, PACK_MIN_ZOOM, 15)
        val ratio = toFifteen.toDouble() / toFourteen

        assertTrue("$ratio is not the fourfold growth the constant's comment claims", ratio > 3.0)
    }

    @Test fun `a tile count is the rectangle of indices the corners fall in`() {
        // At zoom 0 the whole world is one tile, whatever the box.
        assertEquals(1L, packTileCount(mapPackBox(raqqa), minZoom = 0, maxZoom = 0))
        // At zoom 1 the world is four, and Raqqa's box sits inside the north-east one.
        assertEquals(1L, packTileCount(mapPackBox(raqqa), minZoom = 1, maxZoom = 1))
        // A box spanning the prime meridian at zoom 1 touches both halves.
        val london = mapPackBox(MapPoint(51.5, 0.0), radiusKm = 25.0)
        assertEquals(2L, packTileCount(london, minZoom = 1, maxZoom = 1))
    }

    @Test fun `a box near the pole does not swallow the world`() {
        val box = mapPackBox(MapPoint(89.9, 10.0), radiusKm = 25.0)

        assertTrue("north must stay inside the projection", box.north <= 85.06)
        assertTrue("the box must stay a box", box.east >= box.west)
        // Mercator stretches the ground near the pole, so the same 25 km is far more tiles than
        // it is in Syria. What matters is that it stays a rounding error against the whole world,
        // which at this zoom is 268 million tiles.
        assertTrue("a pole is not the whole world", packTileCount(box) < 100_000)
    }

    @Test fun `progress with nothing counted yet says nothing rather than a number it takes back`() {
        val counting = MapPackState.Downloading(completed = 12, required = 0, bytes = 4_000)
        assertNull(counting.fraction)

        val half = MapPackState.Downloading(completed = 300, required = 600, bytes = 9_000)
        assertTrue(abs(half.fraction!! - 0.5f) < 0.01f)

        // An estimate that turns out low must not report more than finished.
        val overshoot = MapPackState.Downloading(completed = 700, required = 600, bytes = 9_000)
        assertEquals(1f, overshoot.fraction!!, 0.001f)
    }

    @Test fun `a refusal is final, whatever the connection`() {
        listOf(MapPackState.Absent, MapPackState.Downloading(0, 100, 0, running = false)).forEach { state ->
            assertEquals(
                MapPackAction.NOTHING,
                mapPackAction(state, unmetered = true, wanted = false, startedByApp = false),
            )
        }
    }

    @Test fun `a pack is fetched on a free connection and never on a paid one`() {
        assertEquals(
            MapPackAction.DOWNLOAD,
            mapPackAction(MapPackState.Absent, unmetered = true, wanted = true, startedByApp = false),
        )
        assertEquals(
            MapPackAction.NOTHING,
            mapPackAction(MapPackState.Absent, unmetered = false, wanted = true, startedByApp = false),
        )
    }

    @Test fun `a stopped download carries on rather than starting over`() {
        val stopped = MapPackState.Downloading(completed = 400, required = 1_000, bytes = 11_000, running = false)
        assertEquals(
            MapPackAction.DOWNLOAD,
            mapPackAction(stopped, unmetered = true, wanted = true, startedByApp = true),
        )
    }

    @Test fun `leaving the wifi stops what the app started and not what the reader asked for`() {
        val running = MapPackState.Downloading(completed = 400, required = 1_000, bytes = 11_000, running = true)
        assertEquals(
            MapPackAction.PAUSE,
            mapPackAction(running, unmetered = false, wanted = true, startedByApp = true),
        )
        assertEquals(
            MapPackAction.NOTHING,
            mapPackAction(running, unmetered = false, wanted = true, startedByApp = false),
        )
    }

    @Test fun `a finished pack is left alone`() {
        assertEquals(
            MapPackAction.NOTHING,
            mapPackAction(
                MapPackState.Ready(tiles = 1_000, bytes = 26_000_000),
                unmetered = true,
                wanted = true,
                startedByApp = true,
            ),
        )
        assertEquals(
            MapPackAction.NOTHING,
            mapPackAction(MapPackState.Unknown, true, wanted = true, startedByApp = false),
        )
    }

    @Test fun `a lost connection is tried again, and a refusal or an oversized pack is not`() {
        assertEquals(
            MapPackAction.DOWNLOAD,
            mapPackAction(MapPackState.Failed(MapPackFailure.CONNECTION), true, wanted = true, startedByApp = true),
        )
        listOf(MapPackFailure.SERVER, MapPackFailure.TILE_LIMIT, MapPackFailure.OTHER).forEach { reason ->
            assertEquals(
                reason.name,
                MapPackAction.NOTHING,
                mapPackAction(MapPackState.Failed(reason), true, wanted = true, startedByApp = true),
            )
        }
    }

    @Test fun `a target's box is the box of its centre`() {
        val target = MapPackTarget(provinceId = "p1", centre = raqqa)
        assertTrue(target.box.sameAs(mapPackBox(raqqa)))
        assertTrue(!target.box.sameAs(mapPackBox(MapPoint(33.5, 36.3))))
    }
}
