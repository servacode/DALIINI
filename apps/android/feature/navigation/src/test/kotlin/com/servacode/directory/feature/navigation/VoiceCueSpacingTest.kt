package com.servacode.directory.feature.navigation

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * How much room guidance leaves between two things it says, and why a demonstration is bounded
 * by it.
 *
 * The recorded pack averages 4.31 seconds a sentence and its longest runs to 6.74. On the road
 * there is room for that: the stages are a number of seconds apart, so the gap is ten seconds by
 * car and half a minute on foot whatever the distances work out to. A demonstration played
 * faster than life divides that gap, and divided below the length of a sentence every
 * instruction can only arrive while the last one is still being spoken — which is exactly what
 * a demonstration sounded like, on all three profiles, when its speed was a single constant.
 */
class VoiceCueSpacingTest {
    private val planner = NavigationVoicePlanner()

    /** The three the app offers, at the speeds NavigationReplay walks them at. */
    private val walking = 1.4
    private val motorcycle = 8.3
    private val driving = 11.0

    @Test
    fun `on the road there is room for a sentence at every speed`() {
        listOf(walking, motorcycle, driving).forEach { speed ->
            val gap = planner.tightestCueSeconds(speed)
            assertTrue(
                "at $speed m/s guidance leaves only $gap s between sentences",
                gap >= SPOKEN_SENTENCE_SECONDS,
            )
        }
    }

    @Test
    fun `a slower traveller is given more room, not the same room`() {
        // The stages are seconds, so the distances shrink with speed while the time between them
        // grows: it is the walk that has the longest gap, not the drive.
        assertTrue(planner.tightestCueSeconds(walking) > planner.tightestCueSeconds(motorcycle))
        assertTrue(planner.tightestCueSeconds(driving) >= planner.tightestCueSeconds(motorcycle))
    }

    @Test
    fun `the speed a demonstration may be played at leaves a sentence its time`() {
        // The bound the view model applies, restated here against the planner it reads.
        listOf(walking, motorcycle, driving).forEach { speed ->
            val playback = planner.tightestCueSeconds(speed) / SPOKEN_SENTENCE_SECONDS
            val onScreen = planner.tightestCueSeconds(speed) / playback
            assertTrue(
                "played at ${playback}x a sentence would have $onScreen s",
                onScreen >= SPOKEN_SENTENCE_SECONDS,
            )
        }
    }

    @Test
    fun `a standing traveller is not divided by zero`() {
        assertTrue(planner.tightestCueSeconds(0.0).isInfinite())
        assertTrue(planner.tightestCueSeconds(-1.0).isInfinite())
    }

    private companion object {
        /** The measured average of the pack's 307 recordings, as the view model uses it. */
        const val SPOKEN_SENTENCE_SECONDS = 4.5
    }
}
