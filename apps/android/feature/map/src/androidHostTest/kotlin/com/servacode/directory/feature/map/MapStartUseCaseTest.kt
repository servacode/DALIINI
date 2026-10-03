package com.servacode.directory.feature.map

import com.servacode.directory.core.maps.MapCamera
import com.servacode.directory.core.maps.MapCameraPolicy
import com.servacode.directory.core.maps.MapPoint
import com.servacode.directory.core.model.FacilityDetail
import com.servacode.directory.core.model.GeoPoint
import com.servacode.directory.core.model.Province
import com.servacode.directory.core.testing.FakeLocation
import com.servacode.directory.core.testing.FakePreferences
import com.servacode.directory.core.testing.FakePublicCache
import com.servacode.directory.core.testing.ScriptedPublicApi
import com.servacode.directory.core.testing.facility
import com.servacode.directory.core.testing.fix
import com.servacode.directory.core.testing.offline
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** INT-092 and INT-094: where the public map opens, from the sources the app really has. */
class MapStartUseCaseTest {
    private val raqqa = Province("raqqa-id", "الرقة", mapCenter = GeoPoint(35.9528, 39.0085))
    private val api = ScriptedPublicApi().apply { provincesAnswer = { listOf(raqqa) } }
    private val cache = FakePublicCache()

    private fun start(location: FakeLocation = FakeLocation(), province: String? = raqqa.id) =
        MapStartUseCase(MapRepository(api, cache, FakePreferences(province), location))

    @Test fun `with a location the map opens on the user's neighbourhood`() = runTest {
        val camera = start(FakeLocation(fix(35.95, 39.02)))(focusFacilityId = null, restored = null)

        assertEquals(MapCamera(MapPoint(35.95, 39.02), MapCameraPolicy.NEARBY_ZOOM), camera)
        assertEquals("the province list is not fetched", emptyList<String>(), api.calls)
    }

    @Test fun `without a location the map opens on the selected province`() = runTest {
        val camera = start()(focusFacilityId = null, restored = null)

        assertEquals(MapCamera(MapPoint(35.9528, 39.0085), MapCameraPolicy.PROVINCE_ZOOM), camera)
    }

    @Test fun `offline, the cached province list still gives the centre`() = runTest {
        api.provincesAnswer = { throw offline }
        cache.provinces = listOf(raqqa)

        val camera = start()(focusFacilityId = null, restored = null)

        assertEquals(MapCamera(MapPoint(35.9528, 39.0085), MapCameraPolicy.PROVINCE_ZOOM), camera)
    }

    @Test fun `a province without a centre leaves the map's default rather than a guess`() = runTest {
        api.provincesAnswer = { listOf(raqqa.copy(mapCenter = null)) }

        assertNull(start()(focusFacilityId = null, restored = null))
    }

    @Test fun `opened for a facility, it centres on the facility the detail screen cached`() = runTest {
        cache.details["f1"] = FacilityDetail(facility("f1"), latitude = 35.96, longitude = 39.01)

        val camera = start(FakeLocation(fix(35.95, 39.02)))(focusFacilityId = "f1", restored = null)

        assertEquals(MapCamera(MapPoint(35.96, 39.01), MapCameraPolicy.FACILITY_ZOOM), camera)
        assertEquals("nothing is fetched for a facility already cached", emptyList<String>(), api.calls)
    }

    @Test fun `a facility not cached is read from the backend`() = runTest {
        api.facilityAnswer = { FacilityDetail(facility(it), latitude = 35.97, longitude = 39.0) }

        val camera = start()(focusFacilityId = "f2", restored = null)

        assertEquals(MapCamera(MapPoint(35.97, 39.0), MapCameraPolicy.FACILITY_ZOOM), camera)
        assertEquals(listOf("facility:f2"), api.calls)
    }

    @Test fun `a facility without coordinates falls back to the user, then the province`() = runTest {
        cache.details["f3"] = FacilityDetail(facility("f3"))

        val camera = start()(focusFacilityId = "f3", restored = null)

        assertEquals(MapCamera(MapPoint(35.9528, 39.0085), MapCameraPolicy.PROVINCE_ZOOM), camera)
    }

    @Test fun `a camera the map was left at wins, and nothing is asked again`() = runTest {
        val left = MapCamera(MapPoint(35.99, 39.05), zoom = 13.2, bearing = 40.0)

        val camera = start(FakeLocation(fix(35.95, 39.02)))(focusFacilityId = "f1", restored = left)

        assertEquals(left, camera)
        assertEquals(emptyList<String>(), api.calls)
    }
}
