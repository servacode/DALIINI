package com.servacode.directory.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RealtimeConfigTest {
    @Test
    fun `requires configured wss without query token`() {
        assertEquals(
            "wss://api.example.test/ws/events/",
            RealtimeConfig("wss://api.example.test/ws/events/").requireConfiguredWebSocketUrl(),
        )
        assertThrows(IllegalArgumentException::class.java) {
            RealtimeConfig("ws://api.example.test/ws/events/").requireConfiguredWebSocketUrl()
        }
        assertThrows(IllegalArgumentException::class.java) {
            RealtimeConfig("wss://api.example.test/ws/events/?token=secret")
                .requireConfiguredWebSocketUrl()
        }
    }
}
