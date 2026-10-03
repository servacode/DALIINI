package com.servacode.directory.core.network

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RealtimeEventDeduplicatorTest {
    @Test
    fun `same event is delivered once`() {
        val deduplicator = RealtimeEventDeduplicator()
        val event = RealtimeEnvelope(
            version = 1,
            name = "public.facility.changed",
            scope = RealtimeScope("province", "province-1"),
            resourceId = "facility-1",
            occurredAt = "2026-09-17T15:00:00Z",
        )
        assertTrue(deduplicator.shouldDeliver(event))
        assertFalse(deduplicator.shouldDeliver(event))
    }
}
