package com.servacode.directory.core.transport

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

/**
 * What a request is allowed to say about where somebody is: the port of Android's
 * `CoordinatesTest`.
 *
 * A coordinate travels in a query string, and a query string is written down by every log and
 * proxy it passes. These tests are about the ceiling on what any of them can ever hold.
 */
class CoordinatesTest {
    @Test fun `a position is sent to about ten metres and not to the centimetre`() {
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

    @Test fun `the decimal point is a point whatever the phone's language is`() {
        // An Arabic or German locale writes 35,9513 — which is two query parameters to a
        // server reading a comma-separated anything, and a parse error at best. Common Kotlin
        // formats without a locale at all, so there is no default to switch here.
        val sent = 35.9513463.asCoordinate()
        assertEquals("35.9513", sent)
        assertFalse(',' in sent.orEmpty())
    }

    @Test fun `four places is the number stated once`() {
        // Pinned so nobody raises it back to "just in case": the backend names a province and
        // sorts by distance, and a GPS fix is rarely better than ten metres anyway.
        assertEquals(4, SENT_DECIMALS)
    }

    @Test fun `every value is written as Java's String format writes it`() {
        // Each expected string is what String.format(Locale.US, "%.4f", value) returns on the
        // JVM: the shortest decimal for the double, rounded half-up, with the double's sign.
        val expected = listOf(
            35.95 to "35.9500",
            39.01 to "39.0100",
            0.0 to "0.0000",
            -0.0 to "-0.0000",
            1e-4 to "0.0001",
            5e-5 to "0.0001",
            4.9999e-5 to "0.0000",
            -4e-5 to "-0.0000",
            -5e-5 to "-0.0001",
            179.99995 to "180.0000",
            -179.99995 to "-180.0000",
            9.99995 to "10.0000",
            35.0 to "35.0000",
            1e7 to "10000000.0000",
            1.2345e7 to "12345000.0000",
            0.00015 to "0.0002",
            0.00025 to "0.0003",
            1.00005 to "1.0001",
            0.12345 to "0.1235",
            0.99995 to "1.0000",
            123.45675 to "123.4568",
            1e-10 to "0.0000",
            99999.99995 to "100000.0000",
            1.0e21 to "1000000000000000000000.0000",
            1.23456789e-3 to "0.0012",
            0.1 + 0.2 to "0.3000",
            Double.MIN_VALUE to "0.0000",
            45.12344999999999 to "45.1234",
            12.000049999999 to "12.0000",
            Double.NaN to "NaN",
            Double.POSITIVE_INFINITY to "Infinity",
            Double.NEGATIVE_INFINITY to "-Infinity",
        )
        expected.forEach { (value, text) -> assertEquals(text, value.asCoordinate(), "$value") }
    }
}
