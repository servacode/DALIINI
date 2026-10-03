package com.servacode.directory.core.transport

import com.servacode.directory.core.auth.SessionState
import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.AppException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val SESSION = "55555555-5555-4555-8555-555555555555"
private const val PROFILE = """{"id":"$SESSION","displayName":"مالك","phone":"+963900000001",""" +
    """"provinceId":null,"phoneVerifiedAt":null,"address":"","profileImageUrl":null}"""
private const val CONCURRENT_REQUESTS = 8

/**
 * Refresh as the transport wires it: the port of Android's `RefreshCoordinationTest`. The real
 * [TransportClients], the real [com.servacode.directory.core.auth.SessionCoordinator] and the
 * Ktor refresh gateway together, against a backend that expires the first access token.
 */
@OptIn(ExperimentalAtomicApi::class)
class RefreshCoordinationTest {
    private val backend = FakeBackend()
    private val refreshes = AtomicInt(0)
    private val unauthorized = AtomicInt(0)
    private var refreshStatus = 200

    /** When set, the refresh is answered only once this many requests have been refused. */
    private var refreshWaitsFor: Int? = null
    private val everyRequestRefused = CompletableDeferred<Unit>()

    private lateinit var wiring: Wiring
    private lateinit var publicApi: KtorPublicApi
    private lateinit var auth: KtorAuthApi

    /** Builds the transport after the test has put the device in the state it needs. */
    private fun wire(vault: MemoryVault, accessToken: String?) {
        wiring = Wiring(backend, vault = vault)
        wiring.accessTokens.set(accessToken)
        publicApi = KtorPublicApi(wiring.clients)
        auth = KtorAuthApi(wiring.clients, deviceName = "test")
        backend.dispatcher = ::answer
    }

    private suspend fun answer(request: Recorded): Answer {
        val path = request.url.encodedPath
        return when {
            path == "/api/v1/auth/refresh/" -> {
                refreshes.incrementAndFetch()
                // Held until every concurrent request is already waiting on it.
                if (refreshWaitsFor != null) everyRequestRefused.await()
                when (refreshStatus) {
                    200 -> Answer(
                        200,
                        """{"accessToken":"fresh","refreshToken":"secret-2","sessionId":"$SESSION",""" +
                            """"expiresAt":"2026-10-19T10:00:00Z"}""",
                    )
                    401 -> envelope(401, "AUTHENTICATION_FAILED")
                    else -> Answer(503)
                }
            }
            path == "/api/v1/auth/login/" -> envelope(401, "AUTHENTICATION_FAILED")
            request.headers["Authorization"] == "Bearer fresh" -> Answer(200, PROFILE)
            else -> {
                if (unauthorized.incrementAndFetch() == refreshWaitsFor) everyRequestRefused.complete(Unit)
                envelope(401, "AUTHENTICATION_FAILED")
            }
        }
    }

    private fun envelope(code: Int, errorCode: String) =
        Answer(code, """{"code":"$errorCode","message":"x","details":{},"requestId":"r-$code"}""")

    /** A signed-in device whose access token has just expired. */
    private fun signedInWithExpiredToken() = wire(MemoryVault("v1\n$SESSION\nsecret-1"), accessToken = "expired")

    @Test fun `concurrent requests on an expired token spend the refresh secret once`() = runTest {
        refreshWaitsFor = CONCURRENT_REQUESTS
        signedInWithExpiredToken()

        val profiles = withContext(Dispatchers.Default) {
            coroutineScope { List(CONCURRENT_REQUESTS) { async { publicApi.profile() } }.awaitAll() }
        }

        assertEquals(CONCURRENT_REQUESTS, profiles.size)
        assertEquals(1, refreshes.load())
        assertEquals("fresh", wiring.accessTokens.get())
        assertEquals("v1\n$SESSION\nsecret-2", wiring.vault.read())
        assertEquals(SessionState.SIGNED_IN, wiring.session.state.value)
        // Every request was sent again, once, with the fresh token.
        val retried = backend.requests().count { it.headers["Authorization"] == "Bearer fresh" }
        assertEquals(CONCURRENT_REQUESTS, retried)
    }

    @Test fun `after process death the first request recovers the access token by refreshing`() = runTest {
        wire(MemoryVault("v1\n$SESSION\nsecret-1"), accessToken = null)

        val profile = publicApi.profile()

        assertEquals("مالك", profile.name)
        assertEquals(1, refreshes.load())
        // The refresh went out with the stored secret and no access token.
        val refresh = backend.requests().single { it.url.encodedPath == "/api/v1/auth/refresh/" }
        assertEquals("""{"refreshToken":"secret-1"}""", refresh.text)
        assertNull(refresh.headers["Authorization"])
    }

    @Test fun `a refused refresh ends the session and the request fails as unauthenticated`() = runTest {
        signedInWithExpiredToken()
        refreshStatus = 401

        val error = assertFailsWith<AppException> { publicApi.profile() }

        assertEquals(AppError.Kind.UNAUTHENTICATED, error.error.kind)
        assertNull(wiring.accessTokens.get())
        assertNull(wiring.vault.read())
        assertEquals(SessionState.SIGNED_OUT, wiring.session.state.value)
    }

    @Test fun `a refresh that cannot reach the backend keeps the session`() = runTest {
        signedInWithExpiredToken()
        refreshStatus = 503

        assertFailsWith<AppException> { publicApi.profile() }

        assertEquals("v1\n$SESSION\nsecret-1", wiring.vault.read())
        assertEquals(SessionState.SIGNED_IN, wiring.session.state.value)
    }

    @Test fun `a wrong password is a wrong password and not a refresh`() = runTest {
        signedInWithExpiredToken()

        val error = assertFailsWith<AppException> { auth.login("+963900000001", "wrong") }

        assertEquals("AUTHENTICATION_FAILED", error.error.code)
        assertEquals(0, refreshes.load())
        val login = backend.requests().single()
        assertNull(login.headers["Authorization"])
        assertEquals(
            """{"phone":"+963900000001","password":"wrong","platform":"ANDROID","deviceName":"test"}""",
            login.text,
        )
    }

    @Test fun `public discovery never carries the user's token`() = runTest {
        signedInWithExpiredToken()
        backend.dispatcher = { Answer(200, """{"items":[]}""") }

        publicApi.provinces()

        val request = backend.requests().single()
        assertNull(request.headers["Authorization"])
        assertTrue(request.headers["X-Request-ID"].orEmpty().isNotBlank())
    }
}
