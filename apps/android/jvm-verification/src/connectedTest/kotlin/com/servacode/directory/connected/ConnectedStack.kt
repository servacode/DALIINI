package com.servacode.directory.connected

import com.servacode.directory.core.auth.AccessTokenStore
import com.servacode.directory.core.auth.RefreshTokenVault
import com.servacode.directory.core.auth.SessionCoordinator
import com.servacode.directory.core.network.AccessTokenInterceptor
import com.servacode.directory.core.network.AuthApiBoundary
import com.servacode.directory.core.network.NetworkModule
import com.servacode.directory.core.network.OwnerApiBoundary
import com.servacode.directory.core.network.PublicApiBoundary
import com.servacode.directory.core.network.RequestIdInterceptor
import com.servacode.directory.core.network.api.ApiEnvironment
import com.servacode.directory.core.network.api.GeneratedClient
import com.servacode.directory.core.network.api.GeneratedOwnerApi
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/** The fixture accounts `seed_e2e_mobile_fixtures` creates. */
object Accounts {
    const val CITIZEN_PHONE = "+963900777001"
    const val CITIZEN_PASSWORD = "CitizenPass123!"
    const val OWNER_PHONE = "+963900777002"
    const val OWNER_PASSWORD = "OwnerMobile123!"
    const val REGISTRANT_PHONE = "+963900777100"
}

/**
 * One device: its own memory token store, its own vault, and the clients built by the app's
 * own [NetworkModule] providers against the live backend. Only the device-bound pieces are
 * test doubles: the Keystore vault and memory store would behave the same.
 */
class Device {
    val access = MemoryAccess()
    val vault = MemoryVault()
    /** Every call to the refresh endpoint this device made. */
    val refreshCalls = AtomicInteger()

    private val environment = ApiEnvironment(baseUrl(), allowCleartext = true)
    private val base = NetworkModule.provideBaseHttpClient()
    private val anonymousHttp = NetworkModule.provideAnonymousHttpClient(base, RequestIdInterceptor())
        .newBuilder()
        .addInterceptor(Interceptor { chain ->
            if (chain.request().url.encodedPath == "/api/v1/auth/refresh/") refreshCalls.incrementAndGet()
            chain.proceed(chain.request())
        })
        .build()
    private val anonymous = GeneratedClient(environment, anonymousHttp)
    val session: SessionCoordinator = NetworkModule.provideSessionCoordinator(access, vault, anonymous)
    private val authorized = NetworkModule.provideAuthorizedClient(
        environment,
        NetworkModule.provideAuthorizedHttpClient(base, RequestIdInterceptor(), AccessTokenInterceptor(access), session),
    )
    val public: PublicApiBoundary = NetworkModule.providePublicApiBoundary(anonymous, authorized)
    val owner: OwnerApiBoundary = GeneratedOwnerApi(authorized)
    val auth: AuthApiBoundary = NetworkModule.provideAuthApiBoundary(
        anonymous,
        authorized,
        com.servacode.directory.core.network.api.ClientIdentity("connected test"),
    )

    fun signIn(phone: String, password: String): Device = apply {
        runBlocking { session.establish(auth.login(phone, password)) }
    }

    /** What a server sees after the access token's fifteen minutes: a token it cannot verify. */
    fun expireAccessToken() = access.set("expired.access.token")

    companion object {
        fun baseUrl(): String = System.getenv("DIRECTORY_API_BASE_URL").orEmpty().ifBlank {
            error("DIRECTORY_API_BASE_URL is not set; run scripts/e2e-android.sh")
        }
    }
}

/** One signed-in citizen shared by the suites that only need a session, to spare the login throttle. */
object Citizen {
    val device: Device by lazy { Device().signIn(Accounts.CITIZEN_PHONE, Accounts.CITIZEN_PASSWORD) }
}

/**
 * Sets a known code on an OTP challenge through the backend's test-only command.
 *
 * The development OTP provider delivers nothing, by design, so a connected registration has
 * no other way to learn a code. The command refuses to run with a real provider.
 */
fun setOtp(challengeId: String, code: String) {
    val container = System.getenv("E2E_API_CONTAINER").orEmpty().ifBlank { "e2e-api" }
    val process = ProcessBuilder(
        "docker", "exec", container, "uv", "run", "python", "manage.py", "e2e_set_otp",
        "--challenge", challengeId, "--code", code,
    ).redirectErrorStream(true).start()
    val output = process.inputStream.bufferedReader().readText()
    check(process.waitFor(60, TimeUnit.SECONDS) && process.exitValue() == 0) { "e2e_set_otp failed: $output" }
}

class MemoryAccess : AccessTokenStore {
    private val value = AtomicReference<String?>(null)
    override fun get(): String? = value.get()
    override fun set(value: String?) = this.value.set(value)
}

class MemoryVault : RefreshTokenVault {
    private val value = AtomicReference<String?>(null)
    override fun read(): String? = value.get()
    override fun write(value: String) = this.value.set(value)
    override fun clear() = value.set(null)
}

/** The backend's socket, on the same host as the API. */
fun socketUrl(): String = Device.baseUrl().replaceFirst("http", "ws") + "ws/v1/directory/"

/**
 * Opens a raw socket, authenticates with [token] and reports the backend's answer: true, false,
 * or null when no answer came. Used to check the backend side of the protocol directly.
 */
fun socketAuthenticates(token: String): Boolean? {
    val replies = java.util.concurrent.LinkedBlockingQueue<String>()
    val socket = OkHttpClient().newWebSocket(
        Request.Builder().url(socketUrl()).build(),
        object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.send("""{"action":"authenticate","accessToken":"$token"}""")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                replies.offer(text)
            }
        },
    )
    try {
        val reply = replies.poll(10, TimeUnit.SECONDS) ?: return null
        return when {
            reply.contains("\"ok\": true") || reply.contains("\"ok\":true") -> true
            reply.contains("\"ok\": false") || reply.contains("\"ok\":false") -> false
            else -> null
        }
    } finally {
        socket.close(1000, "done")
    }
}
