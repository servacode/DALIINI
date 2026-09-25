package com.servacode.directory.feature.navigation

import com.servacode.directory.core.maps.ManeuverKind
import com.servacode.directory.core.maps.ManeuverModifier
import com.servacode.directory.core.maps.MapPoint
import com.servacode.directory.core.maps.NavigationRoute
import com.servacode.directory.core.maps.RouteManeuver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guidance as a list of sentences, with no speaker in the room.
 *
 * Every one of these is a way guidance used to be wrong: said once and too late, said at the
 * same distance whatever the speed, never said at all because the threshold was stepped over
 * rather than landed on, or said about a road the person had already left.
 */
class NavigationVoicePlannerTest {
    private val turn = RouteManeuver(
        kind = ManeuverKind.TURN,
        modifier = ManeuverModifier.RIGHT,
        point = MapPoint(33.5100, 36.2800),
        streetName = "شارع بغداد",
        distanceMeters = 900.0,
        durationSeconds = 110.0,
    )
    private val route = NavigationRoute(
        geometry = listOf(MapPoint(33.5138, 36.2765), MapPoint(33.5100, 36.2800)),
        distanceMeters = 900.0,
        durationSeconds = 110.0,
        maneuvers = listOf(turn),
    )

    private fun navigating(distanceToManeuver: Double, offRoute: Double = 0.0) =
        NavigationState.Navigating(
            NavigationProgress(
                route = route,
                location = MapPoint(33.5138, 36.2765),
                maneuverIndex = 0,
                remainingDistanceMeters = distanceToManeuver,
                remainingDurationSeconds = 110.0,
                offRouteDistanceMeters = offRoute,
                distanceToManeuverMeters = distanceToManeuver,
            ),
        )

    @Test
    fun `one turn is announced three times, each once`() {
        val planner = NavigationVoicePlanner()
        val stages = listOf(900.0, 700.0, 500.0, 300.0, 200.0, 120.0, 60.0, 30.0, 10.0)
            .flatMap { planner.onState(navigating(it), speedMetersPerSecond = 10f) }
            .map { it.stage }
        assertEquals(listOf(VoiceStage.PREPARE, VoiceStage.APPROACH, VoiceStage.NOW), stages)
    }

    @Test
    fun `a threshold stepped over still fires`() {
        // Two readings a second apart at 70 km/h are nineteen metres apart, so the trigger is
        // never landed on exactly. Guidance that waits for equality says nothing at all.
        val planner = NavigationVoicePlanner()
        val prepare = planner.triggerMeters(VoiceStage.PREPARE, speedMps = 19.4)
        val cues = planner.onState(navigating(prepare + 12.0), 19.4f) +
            planner.onState(navigating(prepare - 7.0), 19.4f)
        assertEquals(listOf(VoiceStage.PREPARE), cues.map { it.stage })
    }

    @Test
    fun `the same distance means different things at different speeds`() {
        val planner = NavigationVoicePlanner()
        val crawling = planner.triggerMeters(VoiceStage.PREPARE, speedMps = 4.0)
        val quick = planner.triggerMeters(VoiceStage.PREPARE, speedMps = 15.0)
        assertTrue("a faster driver must be warned sooner", quick > crawling)
    }

    @Test
    fun `the clamps keep it sensible at both ends`() {
        val planner = NavigationVoicePlanner()
        // Stopped: not told to prepare two kilometres out.
        assertEquals(150.0, planner.triggerMeters(VoiceStage.PREPARE, speedMps = 0.0), 0.001)
        // Flying: not told to prepare beyond what anyone can hold in mind.
        assertEquals(800.0, planner.triggerMeters(VoiceStage.PREPARE, speedMps = 60.0), 0.001)
        assertEquals(120.0, planner.triggerMeters(VoiceStage.NOW, speedMps = 60.0), 0.001)
    }

    @Test
    fun `nothing is said about a road that has been left`() {
        val planner = NavigationVoicePlanner()
        assertTrue(planner.onState(navigating(200.0, offRoute = 90.0), 10f).isEmpty())
    }

    @Test
    fun `leaving the route and coming back does not lose the instruction`() {
        val planner = NavigationVoicePlanner()
        planner.onState(navigating(600.0, offRoute = 90.0), 10f)
        val cues = planner.onState(navigating(120.0), 10f)
        assertEquals(listOf(VoiceStage.APPROACH), cues.map { it.stage })
    }

    @Test
    fun `recomputing is announced once, not once per reading`() {
        val planner = NavigationVoicePlanner()
        val rerouting = NavigationState.Rerouting(navigating(200.0).progress)
        val said = (1..4).flatMap { planner.onState(rerouting, 10f) }
        assertEquals(1, said.size)
        assertEquals(VoiceCueKind.REROUTE, said.single().kind)
    }

    @Test
    fun `arriving is announced once`() {
        val planner = NavigationVoicePlanner()
        val arrived = NavigationState.Arrived(route)
        val said = (1..3).flatMap { planner.onState(arrived, 0f) }
        assertEquals(1, said.size)
        assertEquals(VoiceCueKind.ARRIVE, said.single().kind)
    }

    @Test
    fun `the arrival maneuver keeps quiet so arrival is announced by its owner alone`() {
        val planner = NavigationVoicePlanner()
        val arriveRoute = route.copy(
            maneuvers = listOf(
                turn.copy(kind = ManeuverKind.ARRIVE, modifier = ManeuverModifier.UNKNOWN),
            ),
        )
        val state = NavigationState.Navigating(
            NavigationProgress(
                route = arriveRoute,
                location = MapPoint(33.5138, 36.2765),
                maneuverIndex = 0,
                remainingDistanceMeters = 40.0,
                remainingDurationSeconds = 10.0,
                offRouteDistanceMeters = 0.0,
                distanceToManeuverMeters = 40.0,
            ),
        )
        assertTrue(planner.onState(state, 10f).isEmpty())
    }

    @Test
    fun `a cue carries what the sentence needs and the last one carries no distance`() {
        val planner = NavigationVoicePlanner()
        val prepare = planner.onState(navigating(300.0), 10f).single()
        // The words are chosen by NavigationWords from these three, so these three are the test:
        // how far, which turn, and which street.
        assertEquals(300.0, prepare.distanceMeters!!, 0.5)
        val phrase = ManeuverPhrases.of(prepare.maneuver!!)
        assertEquals(ManeuverPhraseKind.TURN_RIGHT, phrase.kind)
        assertEquals("شارع بغداد", phrase.street)
        planner.onState(navigating(120.0), 10f)
        val now = planner.onState(navigating(30.0), 10f).single()
        assertEquals(VoiceStage.NOW, now.stage)
        // At the turn the distance is not said, so it is not carried.
        assertNull(now.distanceMeters)
    }

    @Test
    fun `a distance is rounded the way it is said, not the way it is measured`() {
        assertEquals(SpokenDistance.Metres(500), spokenDistance(483.0))
        assertEquals(SpokenDistance.Metres(50), spokenDistance(12.0))
        assertEquals(SpokenDistance.Kilometres(halves = 2), spokenDistance(1020.0))
        assertEquals(SpokenDistance.Kilometres(halves = 3), spokenDistance(1480.0))
    }

    @Test
    fun `a second turn is announced in full, not carried over from the first`() {
        val planner = NavigationVoicePlanner()
        listOf(500.0, 300.0, 120.0, 20.0).forEach { planner.onState(navigating(it), 10f) }
        val second = turn.copy(
            modifier = ManeuverModifier.LEFT,
            point = MapPoint(33.5050, 36.2860),
            streetName = "شارع الثورة",
        )
        val nextRoute = route.copy(maneuvers = listOf(second))
        val state = { distance: Double ->
            NavigationState.Navigating(
                NavigationProgress(
                    route = nextRoute,
                    location = MapPoint(33.5100, 36.2800),
                    maneuverIndex = 0,
                    remainingDistanceMeters = distance,
                    remainingDurationSeconds = 60.0,
                    offRouteDistanceMeters = 0.0,
                    distanceToManeuverMeters = distance,
                ),
            )
        }
        val stages = listOf(500.0, 300.0, 120.0, 40.0)
            .flatMap { planner.onState(state(it), 10f) }
            .map { it.stage }
        assertEquals(listOf(VoiceStage.PREPARE, VoiceStage.APPROACH, VoiceStage.NOW), stages)
    }
}
