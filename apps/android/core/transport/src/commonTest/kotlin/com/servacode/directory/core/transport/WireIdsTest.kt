package com.servacode.directory.core.transport

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Ids leave the app as `UUID.fromString(id).toString()` did on Android. Each expected value is
 * what the JVM returns for the same text.
 */
class WireIdsTest {
    @Test fun `a UUID goes out as its own lower-case text`() {
        assertEquals("11111111-1111-4111-8111-111111111111", uuid("11111111-1111-4111-8111-111111111111"))
        assertEquals("abcdef00-1111-4111-8111-111111111111", uuid("ABCDEF00-1111-4111-8111-111111111111"))
    }

    @Test fun `short and signed groups are read as UUID fromString reads them`() {
        assertEquals("00000001-0002-0003-0004-000000000005", uuid("1-2-3-4-5"))
        assertEquals("00000001-0002-0003-0004-000000000005", uuid("+1-2-3-4-5"))
        assertEquals("00000000-0000-0000-0000-000000000000", uuid("0-0-0-0-0"))
        assertEquals("00000001-0002-0003-0004-123456789012", uuid("1-2-3-4-123456789012"))
    }

    @Test fun `a group longer than its slot keeps its low digits`() {
        assertEquals("23456789-0001-0001-0001-000000000001", uuid("123456789-1-1-1-1"))
        assertEquals("ffffffff-0001-0001-0001-000000000001", uuid("7fffffffffffffff-1-1-1-1"))
        assertEquals("00000001-0002-0003-0004-234567890123", uuid("1-2-3-4-1234567890123"))
    }

    @Test fun `anything else is refused before a request is made`() {
        listOf(
            "not-a-uuid",
            "1--2-3-4",
            "-1-2-3-4",
            "1-2-3-4-5-6",
            "11111111111111111111111111111111",
            "",
            "12345678-1234-1234-1234-1234567890123",
            "ffffffffffffffff-1-1-1-1",
            "g-1-1-1-1",
            "11111111-1111-4111-8111-11111111111 ",
        ).forEach { id -> assertFailsWith<IllegalArgumentException>(id) { uuid(id) } }
    }
}
