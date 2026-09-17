package com.servacode.directory.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
}
