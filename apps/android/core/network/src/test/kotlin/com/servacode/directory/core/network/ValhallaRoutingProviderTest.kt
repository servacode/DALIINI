package com.servacode.directory.core.network

import com.servacode.directory.core.maps.ManeuverKind
import com.servacode.directory.core.maps.ManeuverModifier
import com.servacode.directory.core.maps.MapPoint
import com.servacode.directory.core.maps.MapProviderConfig
import com.servacode.directory.core.maps.RoutingException
import com.servacode.directory.core.maps.RoutingFailure
import com.servacode.directory.core.maps.RoutingProfile
import com.servacode.directory.core.observability.NoOpObservability
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * The Valhalla adapter against canned answers in the shape the 3.9.0 route API documents.
 *
 * Two of these tests exist because Valhalla is not OSRM in ways that are silent when wrong: its
 * geometry is packed at six decimal places rather than five, and its `length` is kilometres
 * rather than metres. A route decoded with the wrong divisor still draws a line, and a distance
 * read with the wrong unit still shows a number — neither fails loudly on its own.
 */
class ValhallaRoutingProviderTest {
    private val server = MockWebServer()
    private lateinit var provider: ValhallaRoutingProvider

    /** Three points in Damascus, encoded as Valhalla packs them. */
    private val shape = "oso|~@gpcedAnlFwyE~rNonT"
    private val expected = listOf(
        MapPoint(33.5138, 36.2765),
        MapPoint(33.5100, 36.2800),
        MapPoint(33.5020, 36.2910),
    )
    private val origin = MapPoint(33.5138, 36.2765)
    private val destination = MapPoint(33.5020, 36.2910)

    @Before fun start() {
        server.start()
        provider = providerFor(server.url("/").toString())
    }

    @After fun stop() = server.close()

    private fun providerFor(base: String, client: OkHttpClient = OkHttpClient()) =
        ValhallaRoutingProvider(
            client = client,
            config = MapProviderConfig(
                routingBaseUrl = base,
                geocodingBaseUrl = "https://geocoding.invalid/",
                geocodingUserAgent = "test",
            ),
            observability = NoOpObservability,
        )

    private fun respond(body: String, code: Int = 200) =
        server.enqueue(
            MockResponse.Builder()
                .code(code)
                .addHeader("Content-Type", "application/json")
                .body(body)
                .build(),
        )

    private fun trip(
        shape: String = this.shape,
        length: Double = 2.5,
        time: Double = 480.0,
        maneuvers: String = """{"type":1,"street_names":["شارع بغداد"],"length":2.5,"time":480,"begin_shape_index":0}""",
    ) = """
        {"trip":{"units":"kilometers","summary":{"time":$time,"length":$length},
          "legs":[{"shape":"$shape","maneuvers":[$maneuvers]}]}}
    """

    private fun lastRequestBody() =
        Json.parseToJsonElement(server.takeRequest().body!!.utf8()).jsonObject

    // A — the three modes reach the engine as three different costing models.

    @Test fun `walking asks for the pedestrian costing`() = runTest {
        respond(trip())
        provider.route(origin, destination, RoutingProfile.WALKING)
        assertEquals("pedestrian", lastRequestBody()["costing"]!!.jsonPrimitive.content)
    }

    @Test fun `a motorcycle asks for the motorcycle costing, not a scooter`() = runTest {
        respond(trip())
        provider.route(origin, destination, RoutingProfile.MOTORCYCLE)
        assertEquals("motorcycle", lastRequestBody()["costing"]!!.jsonPrimitive.content)
    }

    @Test fun `driving asks for the auto costing`() = runTest {
        respond(trip())
        provider.route(origin, destination, RoutingProfile.DRIVING)
        assertEquals("auto", lastRequestBody()["costing"]!!.jsonPrimitive.content)
    }

    @Test fun `both points go out in the order they were given`() = runTest {
        respond(trip())
        provider.route(origin, destination, RoutingProfile.DRIVING)
        val locations = lastRequestBody()["locations"]!!.jsonArray
        assertEquals(2, locations.size)
        assertEquals(33.5138, locations[0].jsonObject["lat"]!!.jsonPrimitive.content.toDouble(), 1e-9)
        assertEquals(36.2910, locations[1].jsonObject["lon"]!!.jsonPrimitive.content.toDouble(), 1e-9)
    }

    // B — geometry, at six decimal places.

    @Test fun `the shape is decoded at six decimal places`() = runTest {
        respond(trip())
        val route = provider.route(origin, destination)
        assertEquals(expected.size, route.geometry.size)
        expected.forEachIndexed { index, point ->
            assertEquals(point.latitude, route.geometry[index].latitude, 1e-6)
            assertEquals(point.longitude, route.geometry[index].longitude, 1e-6)
        }
    }

    @Test fun `a five-place reading would have landed somewhere else entirely`() {
        // Not a test of the adapter so much as of why it has its own decoder: the same bytes
        // read at OSRM's precision put this route ten degrees off the coast of Somalia.
        val decoded = decodePolyline6(shape)
        assertEquals(33.5138, decoded.first().latitude, 1e-6)
        assertTrue("a five-place divisor would give ten times this", decoded.first().latitude < 40.0)
    }

    // C — units.

    @Test fun `kilometres from the engine become metres in the domain`() = runTest {
        respond(trip(length = 2.5, time = 480.0))
        val route = provider.route(origin, destination)
        assertEquals(2500.0, route.distanceMeters, 0.001)
        assertEquals(480.0, route.durationSeconds, 0.001)
    }

    @Test fun `a maneuver's own length is converted too`() = runTest {
        respond(
            trip(
                maneuvers = """{"type":15,"street_names":["شارع بغداد"],"length":0.4,"time":60,"begin_shape_index":1}""",
            ),
        )
        val route = provider.route(origin, destination)
        assertEquals(400.0, route.maneuvers.single().distanceMeters, 0.001)
    }

    // D — maneuvers, whose direction Valhalla keeps inside the type.

    @Test fun `the numeric maneuver type carries both the kind and the direction`() = runTest {
        respond(
            trip(
                maneuvers = listOf(
                    """{"type":1,"length":0.1,"time":10,"begin_shape_index":0}""",
                    """{"type":15,"street_names":["شارع الثورة"],"length":0.4,"time":60,"begin_shape_index":1}""",
                    """{"type":26,"length":0.1,"time":15,"begin_shape_index":1,"roundabout_exit_count":2}""",
                    """{"type":4,"length":0,"time":0,"begin_shape_index":2}""",
                ).joinToString(","),
            ),
        )
        val maneuvers = provider.route(origin, destination).maneuvers
        assertEquals(4, maneuvers.size)
        assertEquals(ManeuverKind.DEPART, maneuvers[0].kind)
        assertEquals(ManeuverKind.TURN, maneuvers[1].kind)
        assertEquals(ManeuverModifier.LEFT, maneuvers[1].modifier)
        assertEquals("شارع الثورة", maneuvers[1].streetName)
        assertEquals(ManeuverKind.ROUNDABOUT, maneuvers[2].kind)
        assertEquals(2, maneuvers[2].roundaboutExit)
        assertEquals(ManeuverKind.ARRIVE, maneuvers[3].kind)
    }

    @Test fun `a right turn is not the same maneuver as a left one`() {
        assertEquals(ManeuverModifier.RIGHT, 10.maneuverModifier())
        assertEquals(ManeuverModifier.LEFT, 15.maneuverModifier())
        assertEquals(ManeuverModifier.SLIGHT_RIGHT, 9.maneuverModifier())
        assertEquals(ManeuverModifier.SHARP_LEFT, 14.maneuverModifier())
        assertEquals(ManeuverKind.UTURN, 12.maneuverKind())
    }

    // E — every refusal in its own kind.

    @Test fun `no engine listening is unreachable, not a missing route`() = runTest {
        server.close()
        assertFails(RoutingFailure.UNREACHABLE) { provider.route(origin, destination) }
    }

    @Test fun `a request that never answers is unreachable`() = runTest {
        val impatient = OkHttpClient.Builder()
            .callTimeout(250, TimeUnit.MILLISECONDS)
            .build()
        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .headersDelay(3, TimeUnit.SECONDS)
                .body(trip())
                .build(),
        )
        val slow = providerFor(server.url("/").toString(), impatient)
        assertFails(RoutingFailure.UNREACHABLE) { slow.route(origin, destination) }
    }

    @Test fun `no path found is its own answer`() = runTest {
        respond("""{"error_code":442,"error":"No path could be found for input"}""", code = 400)
        assertFails(RoutingFailure.NO_ROUTE) { provider.route(origin, destination) }
    }

    @Test fun `nothing routable near the point is not the same as no path`() = runTest {
        respond("""{"error_code":171,"error":"No suitable edges near location"}""", code = 400)
        assertFails(RoutingFailure.UNROUTABLE_POINT) { provider.route(origin, destination) }
    }

    @Test fun `beyond the engine's limit says so`() = runTest {
        respond("""{"error_code":154,"error":"Path distance exceeds the max distance limit"}""", code = 400)
        assertFails(RoutingFailure.TOO_FAR) { provider.route(origin, destination) }
    }

    @Test fun `an engine that fails on its own side is not the user's fault`() = runTest {
        respond("""{"error":"Failed to parse TripPath"}""", code = 500)
        assertFails(RoutingFailure.ENGINE_ERROR) { provider.route(origin, destination) }
    }

    @Test fun `a body that is not the documented shape is malformed`() = runTest {
        respond("<html>not json at all</html>")
        assertFails(RoutingFailure.MALFORMED) { provider.route(origin, destination) }
    }

    @Test fun `a route with no geometry is refused rather than drawn`() = runTest {
        respond(trip(shape = ""))
        assertFails(RoutingFailure.MALFORMED) { provider.route(origin, destination) }
    }

    @Test fun `a single-point geometry is not a route`() = runTest {
        respond(trip(shape = "oso|~@gpcedA"))
        assertFails(RoutingFailure.MALFORMED) { provider.route(origin, destination) }
    }

    @Test fun `an impossible coordinate never reaches the engine`() = runTest {
        assertFails(RoutingFailure.INVALID_POINTS) {
            provider.route(MapPoint(91.0, 36.0), destination)
        }
        assertEquals(0, server.requestCount)
    }

    @Test fun `an endpoint still carrying its placeholder is a configuration failure`() = runTest {
        val unconfigured = providerFor("https://<ROUTING_PROVIDER_HOST>/")
        assertFails(RoutingFailure.NOT_CONFIGURED) { unconfigured.route(origin, destination) }
    }

    @Test fun `a costing the engine does not have is a configuration failure`() = runTest {
        respond("""{"error_code":125,"error":"No costing method found"}""", code = 400)
        assertFails(RoutingFailure.NOT_CONFIGURED) { provider.route(origin, destination) }
    }

    private suspend fun assertFails(expected: RoutingFailure, block: suspend () -> Unit) {
        try {
            block()
            fail("expected $expected")
        } catch (e: RoutingException) {
            assertEquals(expected, e.failure)
        }
    }
}
