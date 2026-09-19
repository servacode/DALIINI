package com.servacode.directory.core.maps

import com.servacode.directory.core.maps.MapLifecycleSteps.Level
import org.junit.Assert.assertEquals
import org.junit.Test

class MapLifecycleStepsTest {
    private val calls = mutableListOf<String>()
    private val steps = MapLifecycleSteps(
        object : MapViewCalls {
            override fun create() { calls += "create" }
            override fun start() { calls += "start" }
            override fun resume() { calls += "resume" }
            override fun pause() { calls += "pause" }
            override fun stop() { calls += "stop" }
            override fun destroy() { calls += "destroy" }
        },
    )

    @Test fun `a view added to a resumed screen is brought up in order`() {
        steps.moveTo(Level.RESUMED)

        assertEquals(listOf("create", "start", "resume"), calls)
    }

    @Test fun `leaving a resumed screen pauses and stops the view before destroying it`() {
        steps.moveTo(Level.RESUMED)
        calls.clear()

        steps.destroy()

        assertEquals(listOf("pause", "stop", "destroy"), calls)
    }

    @Test fun `the view is destroyed once when the screen and the composable both end`() {
        steps.moveTo(Level.RESUMED)
        steps.moveTo(Level.DESTROYED)
        steps.destroy()

        assertEquals(1, calls.count { it == "destroy" })
        assertEquals(listOf("create", "start", "resume", "pause", "stop", "destroy"), calls)
    }

    @Test fun `going to the background and back pauses, stops, starts and resumes`() {
        steps.moveTo(Level.RESUMED)
        calls.clear()

        steps.moveTo(Level.CREATED)
        steps.moveTo(Level.RESUMED)

        assertEquals(listOf("pause", "stop", "start", "resume"), calls)
    }

    @Test fun `nothing is called after the view is destroyed`() {
        steps.moveTo(Level.STARTED)
        steps.destroy()
        calls.clear()

        steps.moveTo(Level.RESUMED)

        assertEquals(emptyList<String>(), calls)
    }

    @Test fun `a view that was never created is not created just to be destroyed`() {
        steps.destroy()

        assertEquals(emptyList<String>(), calls)
        assertEquals(Level.DESTROYED, steps.level)
    }
}
