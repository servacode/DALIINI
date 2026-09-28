package com.servacode.directory.connected

import com.servacode.directory.core.auth.AccessTokenStore
import com.servacode.directory.core.auth.RefreshTokenVault
import com.servacode.directory.core.auth.SessionCoordinator
import com.servacode.directory.core.network.AccessTokenInterceptor
import com.servacode.directory.core.network.AuthApiBoundary
import com.servacode.directory.core.network.NetworkModule
import com.servacode.directory.core.network.OwnerApiBoundary
import com.servacode.directory.core.network.PublicApiBoundary
import com.servacode.directory.core.network.PushRegistrationBoundary
import com.servacode.directory.core.network.MaintenanceInterceptor
import com.servacode.directory.core.network.MaintenanceState
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
import java.util.UUID
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
    val maintenanceState = MaintenanceState()
    private val maintenance = MaintenanceInterceptor(maintenanceState)
    private val anonymousHttp = NetworkModule.provideAnonymousHttpClient(base, RequestIdInterceptor(), maintenance)
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
        NetworkModule.provideAuthorizedHttpClient(base, RequestIdInterceptor(), AccessTokenInterceptor(access),
            session, maintenance),
    )
    val public: PublicApiBoundary = NetworkModule.providePublicApiBoundary(anonymous, authorized)
    val owner: OwnerApiBoundary = GeneratedOwnerApi(authorized)
    val push: PushRegistrationBoundary = NetworkModule.providePushRegistrationBoundary(authorized)
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
    manage("e2e_set_otp", "--challenge", challengeId, "--code", code)
}

/**
 * How many push tokens the backend holds as active for [sessionId], read with Django's ORM in
 * the API container. The API never echoes a token back, by design, so there is no other way
 * to see what a sign-out or a revocation did to one.
 */
fun activePushTokens(sessionId: String): Int {
    UUID.fromString(sessionId)
    val output = manage(
        "shell", "-c",
        "from notifications.models import DevicePushToken as T; " +
            "print(T.objects.filter(session_id='$sessionId', active=True).count())",
    )
    return output.trim().lines().last().trim().toInt()
}

/** Runs a Django management command in the API container and returns what it printed. */
private fun manage(vararg args: String): String {
    val container = System.getenv("E2E_API_CONTAINER").orEmpty().ifBlank { "e2e-api" }
    val process = ProcessBuilder(listOf("docker", "exec", container, "uv", "run", "python", "manage.py") + args)
        .redirectErrorStream(true)
        .start()
    val output = process.inputStream.bufferedReader().readText()
    check(process.waitFor(60,
        TimeUnit.SECONDS) && process.exitValue() == 0) { "manage.py ${args.first()} failed: $output" }
    return output
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

/** A small, real JPEG, as a phone camera would hand the app one. */
fun jpeg(width: Int = 64, height: Int = 48): ByteArray {
    val image = java.awt.image.BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_RGB)
    val out = java.io.ByteArrayOutputStream()
    check(javax.imageio.ImageIO.write(image, "jpg", out))
    return out.toByteArray()
}

/** What anyone on the internet gets for [url]: no session, no credentials, no signature. */
class AnonymousFetch(val status: Int, val contentType: String?, val bytes: ByteArray)

fun fetchAnonymously(url: String): AnonymousFetch =
    OkHttpClient().newCall(Request.Builder().url(url).build()).execute().use { response ->
        AnonymousFetch(response.code, response.header("Content-Type"), response.body.bytes())
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
