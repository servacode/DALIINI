package com.servacode.directory.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DistanceTextTest {
    private fun assertShown(expected: String, meters: Double) = assertEquals("$meters m", expected, DistanceText.of(meters))

    @Test fun `below a kilometre, whole metres rounded half up`() {
        assertShown("0 م", 0.0)
        assertShown("1 م", 1.0)
        assertShown("499 م", 499.4)
        assertShown("500 م", 499.6)
        assertShown("484 م", 483.9)
        assertShown("999 م", 999.4)
    }

    @Test fun `a distance that rounds to a kilometre is shown in kilometres, not as 1000 metres`() {
        assertShown("1.0 كم", 999.6)
        assertShown("1.0 كم", 1000.0)
    }

    @Test fun `from a kilometre, kilometres with one decimal, rounded half up`() {
        assertShown("1.1 كم", 1094.9)
        assertShown("7.8 كم", 7758.0)
        assertShown("10.0 كم", 9999.96)
        assertShown("125.4 كم", 125_390.0)
    }

    @Test fun `a negative figure is never shown`() {
        assertShown("0 م", -3.0)
    }
}
