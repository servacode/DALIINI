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

    @Test
    fun `provider policy allows a routing engine on this machine`() {
        // The engine we host ourselves is reached over the loopback in development, where
        // there is no certificate to present and nothing in flight leaves the machine.
        listOf(
            "http://localhost:8002/",
            "http://127.0.0.1:8002/",
            "http://10.0.2.2:8002/",
        ).forEach { assertEquals(it, ProviderEndpointPolicy.requireConfiguredHttps(it)) }
    }

    @Test
    fun `the loopback allowance does not open the door to anywhere else`() {
        listOf(
            "http://routing.example.test/",
            "http://192.168.1.10:8002/",
            "http://localhost.example.test/",
        ).forEach {
            assertThrows(ProviderConfigurationException::class.java) {
                ProviderEndpointPolicy.requireConfiguredHttps(it)
            }
        }
    }

    @Test
    fun `a placeholder endpoint is refused whatever its scheme`() {
        assertThrows(ProviderConfigurationException::class.java) {
            ProviderEndpointPolicy.requireConfiguredHttps("https://<ROUTING_PROVIDER_HOST>/")
        }
    }

    @Test
    fun `every travel mode OSRM cannot tell apart still reaches it as a road profile`() {
        // OSRM compiles one profile into its graph and has no motorcycle at all, so the legacy
        // adapter reads the same network as a car. Valhalla is what makes the three distinct.
        assertEquals("driving", RoutingProfile.DRIVING.wireName)
        assertEquals("walking", RoutingProfile.WALKING.wireName)
        assertEquals("driving", RoutingProfile.MOTORCYCLE.wireName)
    }
}
