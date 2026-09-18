package com.servacode.directory.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RealtimeConfigTest {
    @Test
    fun `requires configured wss without query token`() {
        assertEquals(
            "wss://api.example.test/ws/v1/directory/",
            RealtimeConfig("wss://api.example.test/ws/v1/directory/").requireConfiguredWebSocketUrl(),
        )
        assertThrows(IllegalArgumentException::class.java) {
            RealtimeConfig("ws://api.example.test/ws/v1/directory/").requireConfiguredWebSocketUrl()
        }
        assertThrows(IllegalArgumentException::class.java) {
            RealtimeConfig("wss://api.example.test/ws/v1/directory/?token=secret")
                .requireConfiguredWebSocketUrl()
        }
    }

    @Test
    fun `allows cleartext only when the flavor does`() {
        assertEquals(
            "ws://10.0.2.2:8000/ws/v1/directory/",
            RealtimeConfig("ws://10.0.2.2:8000/ws/v1/directory/", allowCleartext = true)
                .requireConfiguredWebSocketUrl(),
        )
        assertThrows(IllegalStateException::class.java) {
            RealtimeConfig("wss://api.<ROOT_DOMAIN>/ws/v1/directory/").requireConfiguredWebSocketUrl()
        }
    }
}
