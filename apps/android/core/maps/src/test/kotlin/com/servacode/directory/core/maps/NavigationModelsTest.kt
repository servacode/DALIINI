package com.servacode.directory.core.maps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class NavigationModelsTest {
    @Test
    fun `distance is zero for same point`() {
        val point = MapPoint(35.95, 39.01)
        assertEquals(0.0, GeoMath.distanceMeters(point, point), 0.001)
    }

    @Test
    fun `distance to polyline detects nearby point`() {
        val geometry = listOf(MapPoint(35.95, 39.00), MapPoint(35.95, 39.02))
        val distance = GeoMath.distanceToPolylineMeters(MapPoint(35.9501, 39.01), geometry)
        assertTrue(distance < 20.0)
    }

    @Test
    fun `coordinate validation rejects invalid latitude`() {
        assertThrows(IllegalArgumentException::class.java) {
            MapPoint(91.0, 39.0).requireValid()
        }
    }

    @Test
    fun `provider policy rejects demo and non https endpoints`() {
        assertThrows(ProviderConfigurationException::class.java) {
            ProviderEndpointPolicy.requireConfiguredHttps("http://routing.example.test")
        }
        assertThrows(ProviderConfigurationException::class.java) {
            ProviderEndpointPolicy.requireConfiguredHttps("https://router.project-osrm.org/")
        }
    }
}
