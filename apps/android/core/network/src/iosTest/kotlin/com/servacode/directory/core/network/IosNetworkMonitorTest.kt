package com.servacode.directory.core.network

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IosNetworkMonitorTest {
    @Test
    fun `wi-fi at home is online and unmetered`() {
        val reading = PathReading(satisfied = true, expensive = false, constrained = false)

        assertTrue(reading.online)
        assertTrue(reading.unmetered)
    }

    @Test
    fun `mobile data is online but metered`() {
        val reading = PathReading(satisfied = true, expensive = true, constrained = false)

        assertTrue(reading.online)
        assertFalse(reading.unmetered)
    }

    @Test
    fun `low data mode is metered even on wi-fi`() {
        assertFalse(PathReading(satisfied = true, expensive = false, constrained = true).unmetered)
    }

    @Test
    fun `no route out is neither online nor unmetered`() {
        val reading = PathReading(satisfied = false, expensive = false, constrained = false)

        assertFalse(reading.online)
        assertFalse(reading.unmetered)
    }

    @Test
    fun `the monitor answers as soon as it is collected`() = runBlocking {
        // Whatever the simulator's connection is, the first reading comes without waiting for
        // a change.
        withTimeout(10_000) {
            IosNetworkMonitor().online.first()
            IosNetworkMonitor().unmetered.first()
        }
        Unit
    }
}
