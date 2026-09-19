package com.servacode.directory.connected

import com.servacode.directory.core.auth.RefreshRejectedException
import com.servacode.directory.core.auth.SessionState
import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.AppException
import com.servacode.directory.core.network.SignOut
import com.servacode.directory.core.network.api.ApiEnvironment
import com.servacode.directory.core.network.api.GeneratedClient
import com.servacode.directory.core.network.api.GeneratedRefreshGateway
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

/**
 * Sessions against the live backend. Every test that ends a session uses the owner account,
 * so the citizen session other suites share is never revoked under them.
 */
class SessionConnectedTest {
    private fun owner() = Device().signIn(Accounts.OWNER_PHONE, Accounts.OWNER_PASSWORD)

    private fun failure(block: suspend () -> Any?): AppError = runBlocking {
        val thrown = withContext(Dispatchers.IO) { runCatching { block() }.exceptionOrNull() }
        (thrown as AppException).error
    }

    /** A request made with nothing but a bearer token, as anyone holding it could. */
    private fun profileStatusWith(token: String): Int =
        OkHttpClient().newCall(
            Request.Builder()
                .url(Device.baseUrl() + "api/v1/account/profile/")
                .header("Authorization", "Bearer $token")
                .build(),
        ).execute().use { it.code }

    @Test fun `a wrong password is refused as such, and nothing is refreshed`() {
        val device = Device()

        val error = failure { device.auth.login(Accounts.OWNER_PHONE, "WrongPass!") }

        assertEquals(AppError.Kind.UNAUTHENTICATED, error.kind)
        assertEquals("AUTHENTICATION_FAILED", error.code)
        assertEquals(0, device.refreshCalls.get())
        assertEquals(SessionState.SIGNED_OUT, device.session.state.value)
    }

    @Test fun `an expired token is refreshed once for concurrent requests and the secret rotates`() = runBlocking {
        val device = owner()
        val before = device.vault.read()
        val sessionId = device.session.sessionId()
        device.expireAccessToken()

        val profiles = withContext(Dispatchers.IO) { List(6) { async { device.public.profile() } }.awaitAll() }

        assertEquals(6, profiles.size)
        assertEquals(Accounts.OWNER_PHONE, profiles.first().phone)
        assertEquals(1, device.refreshCalls.get())
        assertNotEquals(before, device.vault.read())
        assertEquals(sessionId, device.session.sessionId())
    }

    @Test fun `a replayed refresh secret ends the session and the device signs out`() = runBlocking {
        val device = owner()
        val stolen = device.vault.read()!!.split('\n')[2]
        device.expireAccessToken()
        withContext(Dispatchers.IO) { device.public.profile() }
        // Past the backend's thirty-second grace for requests racing on one secret.
        Thread.sleep(31_000)

        val attacker = GeneratedRefreshGateway(
            GeneratedClient(ApiEnvironment(Device.baseUrl(), allowCleartext = true), OkHttpClient()),
        )
        val replay = withContext(Dispatchers.IO) { runCatching { attacker.rotate(stolen) }.exceptionOrNull() }
        assertTrue(replay is RefreshRejectedException)

        device.expireAccessToken()
        val error = failure { device.public.profile() }

        assertEquals(AppError.Kind.UNAUTHENTICATED, error.kind)
        assertEquals(SessionState.SIGNED_OUT, device.session.state.value)
        assertNull(device.vault.read())
        assertNull(device.access.get())
    }

    @Test fun `revoking a session elsewhere ends it at once, over REST, on the socket and for push`() = runBlocking {
        val phone = owner()
        val laptop = owner()
        val phoneToken = phone.access.get()!!
        assertEquals(true, socketAuthenticates(phoneToken))
        val phoneSession = phone.session.sessionId()!!
        val laptopSession = laptop.session.sessionId()!!
        withContext(Dispatchers.IO) {
            phone.push.registerAndroidToken("e2e-push-phone-" + UUID.randomUUID())
            laptop.push.registerAndroidToken("e2e-push-laptop-" + UUID.randomUUID())
        }
        assertEquals(1, activePushTokens(phoneSession))
        assertEquals(1, activePushTokens(laptopSession))

        withContext(Dispatchers.IO) { laptop.auth.revokeSession(phoneSession) }

        // The access token is still within its fifteen minutes, and it no longer works.
        assertEquals(401, withContext(Dispatchers.IO) { profileStatusWith(phoneToken) })
        assertEquals(false, socketAuthenticates(phoneToken))
        val error = failure { phone.public.profile() }
        assertEquals(AppError.Kind.UNAUTHENTICATED, error.kind)
        assertEquals(SessionState.SIGNED_OUT, phone.session.state.value)
        // The other device is untouched.
        assertEquals(Accounts.OWNER_PHONE, withContext(Dispatchers.IO) { laptop.public.profile() }.phone)
        // A revoked session receives no more pushes; the other device still does.
        assertEquals(0, activePushTokens(phoneSession))
        assertEquals(1, activePushTokens(laptopSession))
    }

    @Test fun `signing out revokes the session on the backend, stops its pushes and clears the device`() = runBlocking {
        val device = owner()
        val token = device.access.get()!!
        val session = device.session.sessionId()!!
        val pushToken = "e2e-push-" + UUID.randomUUID()
        withContext(Dispatchers.IO) {
            // Registering is idempotent, and so is letting a token go.
            device.push.registerAndroidToken(pushToken)
            device.push.registerAndroidToken(pushToken)
            assertEquals(1, activePushTokens(session))
            device.push.deactivateAndroidToken(pushToken)
            device.push.deactivateAndroidToken(pushToken)
            assertEquals(0, activePushTokens(session))
            device.push.registerAndroidToken(pushToken)
        }
        assertEquals(1, activePushTokens(session))

        withContext(Dispatchers.IO) { SignOut(device.auth, device.session)() }

        assertNull(device.access.get())
        assertNull(device.vault.read())
        assertEquals(SessionState.SIGNED_OUT, device.session.state.value)
        assertEquals(401, withContext(Dispatchers.IO) { profileStatusWith(token) })
        assertEquals(0, activePushTokens(session))
    }
}
