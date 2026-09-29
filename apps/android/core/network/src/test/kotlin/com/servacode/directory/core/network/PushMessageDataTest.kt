package com.servacode.directory.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PushMessageDataTest {
    @Test
    fun `accepts identifier-only payload`() {
        val parsed = PushMessageData.from(
            mapOf("notificationId" to "notification-1", "type" to "application.approved"),
        )
        assertEquals("notification-1", parsed?.notificationId)
    }

    @Test
    fun `rejects unexpected sensitive text fields`() {
        assertNull(
            PushMessageData.from(
                mapOf(
                    "notificationId" to "notification-1",
                    "type" to "application.approved",
                    "body" to "private content",
                ),
            ),
        )
    }

    @Test
    fun `a duty gap nudge carries its day and place, and needs no notification id`() {
        val parsed = PushMessageData.from(
            mapOf("type" to "duty.gap_nudge", "gapDate" to "2026-09-30", "provinceId" to "p-1", "facilityId" to "f-1"),
        )!!
        assertTrue(parsed.isDutyGap)
        assertEquals(java.time.LocalDate.of(2026, 9, 30), parsed.date)
        assertEquals("p-1", parsed.provinceId)
        assertEquals("f-1", parsed.facilityId)
        assertNull(parsed.notificationId)
    }

    @Test
    fun `a malformed date or id is dropped, not the push`() {
        val parsed = PushMessageData.from(
            mapOf("type" to "duty_gap", "date" to "next week", "provinceId" to "<script>"),
        )!!
        assertTrue(parsed.isDutyGap)
        assertNull(parsed.date)
        assertNull(parsed.provinceId)
    }

    @Test
    fun `an ordinary notice still needs its id, and content is still refused`() {
        assertNull(PushMessageData.from(mapOf("type" to "facility.application.approved")))
        assertNull(PushMessageData.from(mapOf("type" to "DUTY_GAP", "title" to "لديك فراغ")))
    }
}
