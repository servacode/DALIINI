package com.servacode.directory.feature.map

import com.servacode.directory.core.maps.MapCamera
import com.servacode.directory.core.maps.MapPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** INT-093: what brings the same map back after a facility detail, without reloading it. */
class MapUiStateTest {
    private val viewport = MapViewport(west = 38.98, south = 35.93, east = 39.04, north = 35.98)

    @Test fun `a new area is loaded`() {
        assertTrue(MapUiState().needsLoad(viewport))
        assertTrue(MapUiState(loadedViewport = viewport).needsLoad(viewport.copy(east = 39.1)))
    }

    @Test fun `back from a detail, the same area is not loaded again`() {
        val state = MapUiState(loadedViewport = viewport)
        // Bounds computed again for the restored camera may differ in the last digits.
        val recomputed = viewport.copy(west = viewport.west + 1e-9, north = viewport.north - 1e-9)

        assertFalse(state.needsLoad(viewport))
        assertFalse(state.needsLoad(recomputed))
    }

    @Test fun `the camera survives as saved state, bearing included`() {
        val camera = MapCamera(MapPoint(35.9528, 39.0085), zoom = 14.5, bearing = 33.0)

        assertEquals(camera, camera.toSaved().toCamera())
    }

    @Test fun `a malformed saved camera is ignored rather than trusted`() {
        assertNull(doubleArrayOf(1.0, 2.0).toCamera())
    }
}
