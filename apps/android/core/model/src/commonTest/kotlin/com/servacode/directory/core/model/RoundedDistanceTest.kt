package com.servacode.directory.core.model

import kotlin.test.assertEquals
import kotlin.test.Test

/**
 * How far it is, rounded the way it is shown.
 *
 * The unit — metres or kilometres — is a word and lives in the design system's resources, so what
 * is asserted here is the number and the choice of unit.
 */
class RoundedDistanceTest {
    private fun assertMetres(expected: Long, meters: Double) =
        assertEquals(RoundedDistance.Metres(expected), roundedDistance(meters), "$meters m")

    private fun assertKilometres(whole: Long, tenth: Long, meters: Double) =
        assertEquals(RoundedDistance.Kilometres(whole, tenth), roundedDistance(meters), "$meters m")

    @Test fun `below a kilometre whole metres rounded half up`() {
        assertMetres(0, 0.0)
        assertMetres(1, 1.0)
        assertMetres(499, 499.4)
        assertMetres(500, 499.6)
        assertMetres(484, 483.9)
        assertMetres(999, 999.4)
    }

    @Test fun `a distance that rounds to a kilometre is shown in kilometres and not as 1000 metres`() {
        assertKilometres(1, 0, 999.6)
        assertKilometres(1, 0, 1000.0)
    }

    @Test fun `from a kilometre kilometres with one decimal rounded half up`() {
        assertKilometres(1, 1, 1094.9)
        assertKilometres(7, 8, 7758.0)
        assertKilometres(10, 0, 9999.96)
        assertKilometres(125, 4, 125_390.0)
    }

    @Test fun `a negative figure is never shown`() {
        assertMetres(0, -3.0)
    }
}
