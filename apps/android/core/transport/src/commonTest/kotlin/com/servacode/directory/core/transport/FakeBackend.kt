package com.servacode.directory.core.transport

import com.servacode.directory.core.auth.AccessTokenStore
import com.servacode.directory.core.auth.MemoryAccessTokenStore
import com.servacode.directory.core.auth.RefreshTokenVault
import com.servacode.directory.core.auth.SessionCoordinator
import com.servacode.directory.core.network.MaintenanceState
import com.servacode.directory.core.network.api.ApiEnvironment
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal const val BASE_URL = "https://api.example.test/"

/** One request as the backend received it, body and its content type included. */
internal class Recorded(
    val method: String,
    val url: Url,
    val headers: Headers,
    val contentType: String?,
    val body: ByteArray,
) {
    /** The body as text, for JSON. */
    val text: String get() = body.decodeToString()

    /** The body one byte to one character, for multipart bodies that carry binary parts. */
    val latin1: String get() = body.joinToString("") { (it.toInt() and BYTE_MASK).toChar().toString() }
}

private const val BYTE_MASK = 0xFF

/** An answer the backend gives: a status and a JSON body, or no body for 204. */
internal class Answer(val status: Int, val body: String = "", val headers: Map<String, String> = emptyMap())

/**
 * The backend these tests talk to, on Ktor's mock engine: the role MockWebServer plays in the
 * Android tests. It answers queued [Answer]s in order, or [dispatcher] when one is set, and keeps
 * every request it received.
 */
internal class FakeBackend {
    private val lock = Mutex()
    private val queued = ArrayDeque<Answer>()
    private val received = mutableListOf<Recorded>()

    /** Decides each answer instead of the queue; it may suspend. */
    var dispatcher: (suspend (Recorded) -> Answer)? = null

    /** Thrown instead of answering, as a transport failure is. */
    var failure: Throwable? = null

    val engine = MockEngine { request -> answer(request) }

    fun enqueue(status: Int = 200, body: String = "", headers: Map<String, String> = emptyMap()) {
        queued.addLast(Answer(status, body, headers))
    }

    /** Every request so far, in the order received. */
    suspend fun requests(): List<Recorded> = lock.withLock { received.toList() }

    /** The [index]th request; the Android tests' `takeRequest`, by position. */
    suspend fun request(index: Int): Recorded = requests()[index]

    private suspend fun MockRequestHandleScope.answer(request: HttpRequestData): HttpResponseData {
        val recorded = Recorded(
            method = request.method.value,
            url = request.url,
            headers = request.headers,
            contentType = request.body.contentType?.toString(),
            body = request.body.toByteArray(),
        )
        lock.withLock { received += recorded }
        failure?.let { throw it }
        val answer = dispatcher?.invoke(recorded) ?: lock.withLock { queued.removeFirst() }
        val headers = buildMap {
            if (answer.body.isNotEmpty()) put(HttpHeaders.ContentType, "application/json")
            putAll(answer.headers)
        }
        return respond(
            content = answer.body,
            status = HttpStatusCode.fromValue(answer.status),
            headers = headersOf(*headers.map { (name, value) -> name to listOf(value) }.toTypedArray()),
        )
    }
}

/** A refresh vault in memory, empty unless a test puts material in it. */
internal class MemoryVault(private var value: String? = null) : RefreshTokenVault {
    override fun read(): String? = value

    override fun write(value: String) {
        this.value = value
    }

    override fun clear() {
        value = null
    }
}

/**
 * The transport as the app wires it, over [backend]: both clients, a real [SessionCoordinator]
 * with the Ktor refresh gateway, and the maintenance state.
 */
internal class Wiring(
    backend: FakeBackend,
    environment: ApiEnvironment = ApiEnvironment(BASE_URL),
    val accessTokens: AccessTokenStore = MemoryAccessTokenStore(),
    val vault: MemoryVault = MemoryVault(),
    val maintenance: MaintenanceState = MaintenanceState(),
    platform: ClientPlatform = ClientPlatform.ANDROID,
) {
    val session: SessionCoordinator

    val clients = TransportClients(environment, backend.engine, platform, accessTokens, maintenance) { session }

    init {
        session = SessionCoordinator(accessTokens, vault, KtorRefreshGateway(clients))
    }
}
