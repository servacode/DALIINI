package com.servacode.directory.core.network.api

import com.servacode.directory.core.auth.AccessTokenStore
import com.servacode.directory.core.auth.RefreshTokenVault
import com.servacode.directory.core.auth.SessionCoordinator
import com.servacode.directory.core.auth.SessionState
import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.AppException
import com.servacode.directory.core.network.AccessTokenInterceptor
import com.servacode.directory.core.network.NetworkModule
import com.servacode.directory.core.network.PublicApiBoundary
import com.servacode.directory.core.network.RequestIdInterceptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

private const val SESSION = "55555555-5555-4555-8555-555555555555"
private const val PROFILE = """{"id":"$SESSION","displayName":"مالك","phone":"+963900000001",""" +
    """"provinceId":null,"phoneVerifiedAt":null,"address":"","profileImageUrl":null}"""

/**
 * Refresh as the app wires it: the clients come from [NetworkModule]'s own providers, so this
 * exercises the real interceptors, the authenticator, the separate dispatchers and the
 * coordinator together, against a server that expires the first access token.
 */
class RefreshCoordinationTest {
    private val server = MockWebServer()
    private val refreshes = AtomicInteger()
    private val refreshStatus = AtomicReference(200)
    private val access = MemoryAccess()
    private val vault = MemoryVault()
    private lateinit var session: SessionCoordinator
    private lateinit var publicApi: PublicApiBoundary
    private lateinit var auth: GeneratedAuthApi

    @Before fun start() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = answer(request)
        }
        server.start()
    }

    /** Builds the clients after the test has put the device in the state it needs. */
    private fun wire() {
        val environment = ApiEnvironment(server.url("/").toString(), allowCleartext = true)
        val base = NetworkModule.provideBaseHttpClient()
        val anonymousHttp = NetworkModule.provideAnonymousHttpClient(base, RequestIdInterceptor())
        val anonymous = NetworkModule.provideAnonymousClient(environment, anonymousHttp)
        session = NetworkModule.provideSessionCoordinator(access, vault, anonymous)
        val authorizedHttp = NetworkModule.provideAuthorizedHttpClient(
            base,
            RequestIdInterceptor(),
            AccessTokenInterceptor(access),
            session,
        )
        val authorized = NetworkModule.provideAuthorizedClient(environment, authorizedHttp)
        publicApi = NetworkModule.providePublicApiBoundary(anonymous, authorized)
        auth = GeneratedAuthApi(anonymous, authorized, deviceName = "test")
    }

    @After fun stop() = server.close()

    private fun answer(request: RecordedRequest): MockResponse {
        val path = request.url.encodedPath
        return when {
            path == "/api/v1/auth/refresh/" -> {
                refreshes.incrementAndGet()
                // Slow enough that every concurrent request is already waiting on it.
                Thread.sleep(150)
                when (refreshStatus.get()) {
                    200 -> json(
                        200,
                        """{"accessToken":"fresh","refreshToken":"secret-2","sessionId":"$SESSION",""" +
                            """"expiresAt":"2026-10-19T10:00:00Z"}""",
                    )
                    401 -> envelope(401, "AUTHENTICATION_FAILED")
                    else -> json(503, "")
                }
            }
            path == "/api/v1/auth/login/" -> envelope(401, "AUTHENTICATION_FAILED")
            request.headers["Authorization"] == "Bearer fresh" -> json(200, PROFILE)
            else -> envelope(401, "AUTHENTICATION_FAILED")
        }
    }

    private fun json(code: Int, body: String) =
        MockResponse.Builder().code(code).addHeader("Content-Type", "application/json").body(body).build()

    private fun envelope(code: Int, errorCode: String) =
        json(code, """{"code":"$errorCode","message":"x","details":{},"requestId":"r-$code"}""")

    /** A signed-in device whose access token has just expired. */
    private fun signedInWithExpiredToken() {
        vault.write("v1\n$SESSION\nsecret-1")
        access.set("expired")
        wire()
    }

    @Test fun `concurrent requests on an expired token spend the refresh secret once`() = runBlocking {
        signedInWithExpiredToken()

        val profiles = withTimeout(20_000) {
            withContext(Dispatchers.IO) { List(8) { async { publicApi.profile() } }.awaitAll() }
        }

        assertEquals(8, profiles.size)
        assertEquals(1, refreshes.get())
        assertEquals("fresh", access.get())
        assertEquals("v1\n$SESSION\nsecret-2", vault.read())
        assertEquals(SessionState.SIGNED_IN, session.state.value)
    }

    @Test fun `after process death the first request recovers the access token by refreshing`() = runBlocking {
        vault.write("v1\n$SESSION\nsecret-1")
        wire()

        val profile = withContext(Dispatchers.IO) { publicApi.profile() }

        assertEquals("مالك", profile.name)
        assertEquals(1, refreshes.get())
    }

    @Test fun `a refused refresh ends the session and the request fails as unauthenticated`() = runBlocking {
        signedInWithExpiredToken()
        refreshStatus.set(401)

        val error = withContext(Dispatchers.IO) { runCatching { publicApi.profile() }.exceptionOrNull() }

        assertEquals(AppError.Kind.UNAUTHENTICATED, (error as AppException).error.kind)
        assertNull(access.get())
        assertNull(vault.read())
        assertEquals(SessionState.SIGNED_OUT, session.state.value)
    }

    @Test fun `a refresh that cannot reach the backend keeps the session`() = runBlocking {
        signedInWithExpiredToken()
        refreshStatus.set(503)

        val error = withContext(Dispatchers.IO) { runCatching { publicApi.profile() }.exceptionOrNull() }

        assertTrue(error is AppException)
        assertEquals("v1\n$SESSION\nsecret-1", vault.read())
        assertEquals(SessionState.SIGNED_IN, session.state.value)
    }

    @Test fun `a wrong password is a wrong password, not a refresh`() = runBlocking {
        signedInWithExpiredToken()

        val error = withContext(Dispatchers.IO) {
            runCatching { auth.login("+963900000001", "wrong") }.exceptionOrNull()
        }

        assertEquals("AUTHENTICATION_FAILED", (error as AppException).error.code)
        assertEquals(0, refreshes.get())
    }

    @Test fun `public discovery never carries the user's token`() = runBlocking {
        signedInWithExpiredToken()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse =
                json(200, """{"items":[]}""")
        }

        withContext(Dispatchers.IO) { publicApi.provinces() }

        val request = server.takeRequest()
        assertNull(request.headers["Authorization"])
        assertTrue(request.headers["X-Request-ID"]!!.isNotBlank())
    }
}

private class MemoryAccess : AccessTokenStore {
    private val value = AtomicReference<String?>(null)
    override fun get(): String? = value.get()
    override fun set(value: String?) = this.value.set(value)
}

private class MemoryVault : RefreshTokenVault {
    private val value = AtomicReference<String?>(null)
    override fun read(): String? = value.get()
    override fun write(value: String) = this.value.set(value)
    override fun clear() = value.set(null)
}
