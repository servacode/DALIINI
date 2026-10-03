package com.servacode.directory.core.maps

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MapCameraPolicyTest {
    private val province = MapPoint(35.9528, 39.0085)
    private val user = MapPoint(35.95, 39.02)
    private val facility = MapPoint(35.96, 39.01)
    private val asked = mutableListOf<String>()

    private fun source(name: String, answer: MapPoint?): suspend () -> MapPoint? = {
        asked += name
        answer
    }

    @Test fun `with a location the map opens on the user's neighbourhood`() = runTest {
        val camera = MapCameraPolicy.forMap(
            restored = null,
            facility = source("facility", null),
            user = source("user", user),
            province = source("province", province),
        )

        assertEquals(MapCamera(user, MapCameraPolicy.NEARBY_ZOOM), camera)
        assertEquals("the province is not fetched when it is not needed", listOf("facility", "user"), asked)
    }

    @Test fun `without a location the map opens on the province, at city zoom`() = runTest {
        val camera = MapCameraPolicy.forMap(
            restored = null,
            facility = source("facility", null),
            user = source("user", null),
            province = source("province", province),
        )

        assertEquals(MapCamera(province, MapCameraPolicy.PROVINCE_ZOOM), camera)
    }

    @Test fun `opened for a facility, the map centres on it however the user is placed`() = runTest {
        val camera = MapCameraPolicy.forMap(
            restored = null,
            facility = source("facility", facility),
            user = source("user", user),
            province = source("province", province),
        )

        assertEquals(MapCamera(facility, MapCameraPolicy.FACILITY_ZOOM), camera)
        assertEquals(listOf("facility"), asked)
    }

    @Test fun `a camera the user left is restored as it was, bearing included`() = runTest {
        val left = MapCamera(MapPoint(35.97, 39.03), zoom = 13.4, bearing = 27.0)

        val camera = MapCameraPolicy.forMap(
            restored = left,
            facility = source("facility", facility),
            user = source("user", user),
            province = source("province", province),
        )

        assertEquals(left, camera)
        assertEquals("nothing is fetched again on the way back", emptyList<String>(), asked)
    }

    @Test fun `with nothing known the map keeps its default instead of a guess`() = runTest {
        val camera = MapCameraPolicy.forMap(
            restored = null,
            facility = source("facility", null),
            user = source("user", null),
            province = source("province", null),
        )

        assertNull(camera)
    }

    @Test fun `the picker opens where the owner is, close enough to tap precisely`() = runTest {
        val camera = MapCameraPolicy.forPicker(user = source("user", user), province = source("province", province))

        assertEquals(MapCamera(user, MapCameraPolicy.FACILITY_ZOOM), camera)
        assertEquals(listOf("user"), asked)
    }

    @Test fun `without a location the picker opens on the province`() = runTest {
        val camera = MapCameraPolicy.forPicker(user = source("user", null), province = source("province", province))

        assertEquals(MapCamera(province, MapCameraPolicy.PROVINCE_ZOOM), camera)
    }

    @Test fun `every initial zoom is city level or closer, never the world`() {
        val zooms = listOf(
            MapCameraPolicy.FACILITY_ZOOM,
            MapCameraPolicy.NEARBY_ZOOM,
            MapCameraPolicy.PROVINCE_ZOOM,
        )
        zooms.forEach { assertTrue("zoom $it", it in 11.0..17.0) }
    }
}
