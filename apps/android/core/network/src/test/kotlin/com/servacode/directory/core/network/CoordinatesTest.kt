package com.servacode.directory.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

/**
 * What a request is allowed to say about where somebody is.
 *
 * A coordinate travels in a query string, and a query string is written down by every log and
 * proxy it passes. These tests are about the ceiling on what any of them can ever hold.
 */
class CoordinatesTest {
    @Test fun `a position is sent to about ten metres, not to the centimetre`() {
        // What the phone reported during a real session: seven places, which locates a person
        // inside a room.
        assertEquals("35.9513", 35.9513463.asCoordinate())
        assertEquals("39.0117", 39.0116519.asCoordinate())
    }

    @Test fun `no position sends nothing at all`() {
        assertNull((null as Double?).asCoordinate())
    }

    @Test fun `it rounds rather than truncates`() {
        assertEquals("35.0002", 35.00015.asCoordinate())
        assertEquals("-12.3457", (-12.34567).asCoordinate())
    }

    @Test fun `south and west stay negative`() {
        assertEquals("-33.8688", (-33.8688197).asCoordinate())
    }

    @Test fun `the decimal point is a point, whatever the phone's language is`() {
        // An Arabic or German locale writes 35,9513 — which is two query parameters to a
        // server reading a comma-separated anything, and a parse error at best.
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("ar-SY"))
            assertEquals("35.9513", 35.9513463.asCoordinate())
            Locale.setDefault(Locale.GERMANY)
            assertEquals("35.9513", 35.9513463.asCoordinate())
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test fun `four places is the number, stated once`() {
        // Pinned so nobody raises it back to "just in case": the backend names a province and
        // sorts by distance, and a GPS fix is rarely better than ten metres anyway.
        assertEquals(4, SENT_DECIMALS)
    }
}
