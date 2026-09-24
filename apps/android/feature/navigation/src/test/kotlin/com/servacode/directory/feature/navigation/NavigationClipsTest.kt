package com.servacode.directory.feature.navigation

import com.servacode.directory.core.maps.ManeuverKind
import com.servacode.directory.core.maps.ManeuverModifier
import com.servacode.directory.core.maps.MapPoint
import com.servacode.directory.core.maps.RouteManeuver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Every clip this app can ask for must be a file that exists.
 *
 * A missing recording is not a crash and not a test failure anywhere else — it is silence on a
 * junction, which is the one failure nobody notices until they are in the car. So the names are
 * checked against the resources on disk rather than against a list written beside them.
 */
class NavigationClipsTest {
    /**
     * The recordings on disk.
     *
     * These tests are run from two working directories — the module's own, by the Android unit
     * test task, and `jvm-verification`, by the platform-free harness — so the directory is
     * looked for from both rather than assumed from one.
     */
    private val raw = listOf(
        "src/main/res/raw",
        "../feature/navigation/src/main/res/raw",
        "apps/android/feature/navigation/src/main/res/raw",
    ).map(::File).firstOrNull { it.isDirectory } ?: File("src/main/res/raw")

    private fun maneuver(
        kind: ManeuverKind,
        modifier: ManeuverModifier = ManeuverModifier.STRAIGHT,
        exit: Int? = null,
    ) = RouteManeuver(
        kind = kind,
        modifier = modifier,
        point = MapPoint(33.51, 36.27),
        streetName = "شارع الثورة",
        distanceMeters = 300.0,
        durationSeconds = 40.0,
        roundaboutExit = exit,
    )

    private fun cue(
        stage: VoiceStage,
        maneuver: RouteManeuver,
        distance: Double? = 300.0,
    ) = VoiceCue(
        stage = stage,
        text = "",
        key = "k",
        kind = VoiceCueKind.MANEUVER,
        maneuver = maneuver,
        distanceMeters = distance,
    )

    @Test
    fun `every turn this app can announce has a recording, at every stage`() {
        assertTrue("the voice pack is not where the test looks: ${raw.absolutePath}", raw.isDirectory)
        val kinds = listOf(
            maneuver(ManeuverKind.TURN, ManeuverModifier.LEFT),
            maneuver(ManeuverKind.TURN, ManeuverModifier.RIGHT),
            maneuver(ManeuverKind.TURN, ManeuverModifier.SLIGHT_LEFT),
            maneuver(ManeuverKind.TURN, ManeuverModifier.SLIGHT_RIGHT),
            maneuver(ManeuverKind.TURN, ManeuverModifier.SHARP_LEFT),
            maneuver(ManeuverKind.TURN, ManeuverModifier.SHARP_RIGHT),
            maneuver(ManeuverKind.UTURN, ManeuverModifier.UTURN),
            maneuver(ManeuverKind.CONTINUE),
            maneuver(ManeuverKind.MERGE, ManeuverModifier.LEFT),
            maneuver(ManeuverKind.MERGE, ManeuverModifier.RIGHT),
            maneuver(ManeuverKind.MERGE, ManeuverModifier.STRAIGHT),
            maneuver(ManeuverKind.FORK, ManeuverModifier.LEFT),
            maneuver(ManeuverKind.FORK, ManeuverModifier.RIGHT),
            maneuver(ManeuverKind.FORK, ManeuverModifier.STRAIGHT),
            maneuver(ManeuverKind.DEPART),
        ) + (1..12).map { maneuver(ManeuverKind.ROUNDABOUT, exit = it) } +
            maneuver(ManeuverKind.ROUNDABOUT, exit = 99)

        val missing = mutableListOf<String>()
        kinds.forEach { turn ->
            listOf(VoiceStage.PREPARE, VoiceStage.APPROACH, VoiceStage.NOW).forEach { stage ->
                val distance = if (stage == VoiceStage.NOW) null else 300.0
                val clip = NavigationClips.clipFor(cue(stage, turn, distance))
                assertNotNull("no clip for ${turn.kind}/${turn.modifier} at $stage", clip)
                if (!File(raw, "$clip.mp3").isFile) missing += "$clip.mp3"
            }
        }
        assertEquals("recordings referenced but not shipped", emptyList<String>(), missing)
    }

    @Test
    fun `every distance the planner can say snaps to a recording that exists`() {
        val planner = NavigationVoicePlanner()
        val turn = maneuver(ManeuverKind.TURN, ManeuverModifier.RIGHT)
        // Every trigger the planner can produce, across the whole speed range it clamps to.
        val distances = (0..70).flatMap { speed ->
            listOf(VoiceStage.PREPARE, VoiceStage.APPROACH)
                .map { planner.triggerMeters(it, speed.toDouble()) }
        }
        val missing = distances.mapNotNull { metres ->
            val clip = NavigationClips.clipFor(cue(VoiceStage.PREPARE, turn, metres))
            if (clip != null && File(raw, "$clip.mp3").isFile) null else "$metres -> $clip"
        }
        assertEquals("distances with no recording", emptyList<String>(), missing)
    }

    @Test
    fun `each event has its own recording`() {
        listOf(
            VoiceCueKind.REROUTE to "nav_recalculating_route",
            VoiceCueKind.ARRIVE to "nav_arrived",
            VoiceCueKind.REROUTE_FAILED to "nav_reroute_failed",
            VoiceCueKind.STARTED to "nav_navigation_started",
        ).forEach { (kind, expected) ->
            val clip = NavigationClips.clipFor(VoiceCue(VoiceStage.EVENT, "", "k", kind))
            assertEquals(expected, clip)
            assertTrue("$expected.mp3 is not shipped", File(raw, "$expected.mp3").isFile)
        }
    }

    @Test
    fun `a distance is snapped to the nearest recording, not to the nearest fifty`() {
        // The pack is not evenly spaced: it jumps 300, 400, 500, 700.
        assertEquals(300, NavigationClips.nearest(320.0))
        assertEquals(400, NavigationClips.nearest(380.0))
        assertEquals(500, NavigationClips.nearest(483.0))
        assertEquals(700, NavigationClips.nearest(640.0))
        assertEquals(700, NavigationClips.nearest(900.0))
        assertEquals(50, NavigationClips.nearest(10.0))
    }

    @Test
    fun `a turn nobody can name is not given a sentence`() {
        val clip = NavigationClips.clipFor(
            cue(VoiceStage.APPROACH, maneuver(ManeuverKind.UNKNOWN, ManeuverModifier.UNKNOWN)),
        )
        assertNull(clip)
    }
}
