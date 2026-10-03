package com.servacode.directory.core.network

import kotlinx.coroutines.flow.Flow

class RealtimeDisconnectedException(message: String) : IllegalStateException(message)

interface RealtimeStream {
    fun events(provinceId: String): Flow<RealtimeSignal>
}

/**
 * The backend's socket is `/ws/v1/directory/`. `allowCleartext` mirrors the API setting: only
 * the local flavor, talking to a development server, may use `ws://`.
 */
data class RealtimeConfig(val webSocketUrl: String, val allowCleartext: Boolean = false) {
    fun requireConfiguredWebSocketUrl(): String {
        val value = webSocketUrl.trim()
        if (value.isBlank() || '<' in value || '>' in value) {
            throw IllegalStateException("Realtime WebSocket URL is not configured.")
        }
        require(value.startsWith("wss://") || (allowCleartext && value.startsWith("ws://"))) {
            "Realtime WebSocket URL must use WSS."
        }
        require('?' !in value) { "Realtime WebSocket URL must not contain query parameters." }
        return value
    }
}
