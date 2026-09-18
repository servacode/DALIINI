package com.servacode.directory.connected

import com.servacode.directory.core.network.OkHttpRealtimeStream
import com.servacode.directory.core.network.RealtimeConfig
import com.servacode.directory.core.network.RealtimeSignal
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The socket protocol from the app's side: authentication after connecting (never a token in
 * the URL), refusal of a bad token, and a clean reconnect. Revocation on the socket is covered
 * in [SessionConnectedTest], events in [OwnerConnectedTest].
 */
class RealtimeConnectedTest {
    @Test fun `a valid access token authenticates on the socket`() {
        val token = Citizen.device.access.get()!!

        assertEquals(true, socketAuthenticates(token))
    }

    @Test fun `a token the backend cannot verify is refused, and the socket stays usable for public events`() {
        assertEquals(false, socketAuthenticates("not.a.token"))
    }

    @Test fun `the app's stream connects, closes and connects again`() = runBlocking {
        val raqqa = Citizen.device.public.provinces().first { it.nameEn == "Raqqa" }.id
        val stream = OkHttpRealtimeStream(
            OkHttpClient(),
            RealtimeConfig(socketUrl(), allowCleartext = true),
            Citizen.device.access,
        )

        repeat(2) {
            val first = withTimeout(15_000) { stream.events(raqqa).first() }
            assertEquals(RealtimeSignal.Connected, first)
        }
    }
}
