package com.servacode.directory.core.network

import com.servacode.directory.core.auth.AccessTokenStore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import javax.inject.Inject
import javax.inject.Singleton

class RealtimeDisconnectedException(message: String) : IllegalStateException(message)

interface RealtimeStream {
    fun events(provinceId: String): Flow<RealtimeSignal>
}

@Singleton
class OkHttpRealtimeStream @Inject constructor(
    private val client: OkHttpClient,
    private val config: RealtimeConfig,
    private val accessTokenStore: AccessTokenStore,
) : RealtimeStream {
    private val json = Json { ignoreUnknownKeys = true }

    override fun events(provinceId: String): Flow<RealtimeSignal> = callbackFlow {
        require(provinceId.isNotBlank()) { "Province id is required." }
        val endpoint = config.requireConfiguredWebSocketUrl()
        val request = Request.Builder().url(endpoint).build()
        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                trySend(RealtimeSignal.Connected)
                webSocket.send("""{"action":"subscribeProvince","provinceId":"$provinceId"}""")
                accessTokenStore.get()?.let { token ->
                    webSocket.send(
                        json.encodeToString(
                            kotlinx.serialization.json.JsonObject.serializer(),
                            kotlinx.serialization.json.buildJsonObject {
                                put("action", kotlinx.serialization.json.JsonPrimitive("authenticate"))
                                put("accessToken", kotlinx.serialization.json.JsonPrimitive(token))
                            },
                        ),
                    )
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val root = runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull() ?: return
                when (root["type"]?.jsonPrimitive?.content) {
                    "auth" -> {
                        if (root["ok"]?.jsonPrimitive?.booleanOrNull == true) {
                            webSocket.send("""{"action":"subscribeUser"}""")
                        }
                    }
                    "event" -> {
                        val event = runCatching { json.decodeFromString<RealtimeEnvelope>(text) }
                            .getOrNull() ?: return
                        trySend(RealtimeSignal.Event(event))
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                close(RealtimeDisconnectedException(t.message ?: "Realtime connection failed."))
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                close(RealtimeDisconnectedException("Realtime connection closed: $code"))
            }
        }
        val socket = client.newWebSocket(request, listener)
        awaitClose { socket.close(1000, "foreground-stop") }
    }
}

data class RealtimeConfig(val webSocketUrl: String) {
    fun requireConfiguredWebSocketUrl(): String {
        val value = webSocketUrl.trim()
        if (value.isBlank() || '<' in value || '>' in value) {
            throw IllegalStateException("Realtime WebSocket URL is not configured.")
        }
        require(value.startsWith("wss://")) { "Realtime WebSocket URL must use WSS." }
        require('?' !in value) { "Realtime WebSocket URL must not contain query parameters." }
        return value
    }
}
