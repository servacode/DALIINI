package com.servacode.directory.core.maps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Which ways there are a decision, measured against the ways Raqqa actually has.
 *
 * Every case here is a real answer from the routing engine over the city's own extract, asked
 * for three alternates between ordinary places in it. They are the reason this filter exists:
 * the engine answers with what it found, and what it found ranges from three genuinely different
 * drives to three walks that differ by three per cent.
 */
class RouteChoicesTest {
    private fun route(minutes: Double, km: Double): NavigationRoute = NavigationRoute(
        geometry = listOf(MapPoint(35.9500, 39.0050), MapPoint(35.9600, 39.0180)),
        distanceMeters = km * 1000.0,
        durationSeconds = minutes * 60.0,
        maneuvers = emptyList(),
    )

    @Test
    fun `three real ways across the city stay three`() {
        // Car, ~9 km: 10.7 min over 9.65 km, 9.6 over 7.56, 9.5 over 6.84.
        val offered = RouteChoices.worthOffering(
            listOf(route(10.7, 9.65), route(9.6, 7.56), route(9.5, 6.84)),
        )
        // The third is six seconds from the second: the same decision twice, so it goes.
        assertEquals(2, offered.size)
        assertEquals(10.7 * 60, offered[0].durationSeconds, 0.01)
        assertEquals(9.6 * 60, offered[1].durationSeconds, 0.01)
    }

    @Test
    fun `a walk offered three times is offered once`() {
        // Pedestrian, ~9 km: 58.9, 60.5 and 60.9 minutes — the same walk, nudged.
        val offered = RouteChoices.worthOffering(
            listOf(route(58.9, 4.97), route(60.5, 5.10), route(60.9, 5.13)),
        )
        assertEquals(1, offered.size)
        assertFalse(RouteChoices.isAChoice(offered))
    }

    @Test
    fun `a way that is simply worse is not an alternative`() {
        // Car, 300 m: 1.4 minutes against 1.9. Different enough, and half a minute worse on a
        // trip of eighty seconds — nobody is choosing that, so it is not put in front of them.
        val offered = RouteChoices.worthOffering(listOf(route(1.4, 0.73), route(1.9, 0.82)))
        assertEquals(1, offered.size)
    }

    @Test
    fun `the engine's own answer is never dropped`() {
        val only = route(4.5, 2.33)
        assertEquals(listOf(only), RouteChoices.worthOffering(listOf(only)))
    }

    @Test
    fun `no more than three are ever offered`() {
        val many = (0..9).map { route(10.0 + it * 5, 5.0 + it) }
        assertTrue(RouteChoices.worthOffering(many).size <= RouteChoices.MAX_OFFERED)
    }

    @Test
    fun `nothing in is nothing out`() {
        assertEquals(emptyList<NavigationRoute>(), RouteChoices.worthOffering(emptyList()))
        assertFalse(RouteChoices.isAChoice(emptyList()))
    }

    @Test
    fun `the way followed by default is the shortest one offered`() {
        // Car, ~9 km: the engine puts 9.65 km first and offers 6.84 km third.
        val offered = listOf(route(10.7, 9.65), route(9.6, 7.56), route(9.5, 6.84))
        assertEquals(2, RouteChoices.preferred(offered))
    }

    @Test
    fun `the engine's order breaks a tie between equal lengths`() {
        val offered = listOf(route(9.0, 5.0), route(8.0, 5.0))
        assertEquals(0, RouteChoices.preferred(offered))
    }

    @Test
    fun `one way there is followed whatever else is asked of it`() {
        assertEquals(0, RouteChoices.preferred(listOf(route(4.5, 2.33))))
        assertEquals(0, RouteChoices.preferred(emptyList()))
    }

    @Test
    fun `a way much slower never reaches the default because it is never offered`() {
        // Car, 2->6 in the measurement: 5.2 minutes over 4.12 km against 7.2 over 3.65. The
        // shorter one is 38% slower, so the filter refuses it and the default cannot pick it.
        val offered = RouteChoices.worthOffering(listOf(route(5.2, 4.12), route(7.2, 3.65)))
        assertEquals(1, offered.size)
        assertEquals(4.12 * 1000, offered[RouteChoices.preferred(offered)].distanceMeters, 0.01)
    }

    @Test
    fun `one way is not a choice and two are`() {
        assertFalse(RouteChoices.isAChoice(listOf(route(5.0, 2.0))))
        assertTrue(RouteChoices.isAChoice(listOf(route(5.0, 2.0), route(7.0, 1.6))))
    }
}
