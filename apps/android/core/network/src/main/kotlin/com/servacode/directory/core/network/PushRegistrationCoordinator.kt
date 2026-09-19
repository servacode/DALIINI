package com.servacode.directory.core.network

import com.servacode.directory.core.auth.SessionCoordinator
import com.servacode.directory.core.auth.SessionState
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps the backend told which push token reaches this device.
 *
 * The provider hands a token at start-up and whenever it rotates one; the backend needs it only
 * for a signed-in session, and ties it to that session, so a token that arrives while signed
 * out is held here and registered when a session starts. Ending a session needs no call from
 * the app: the backend stops pushing to that session's tokens itself.
 */
@Singleton
class PushRegistrationCoordinator @Inject constructor(
    private val boundary: PushRegistrationBoundary,
    private val session: SessionCoordinator,
) {
    private val latest = AtomicReference<String?>(null)

    suspend fun onTokenAvailable(token: String) {
        val normalized = token.trim()
        require(normalized.isNotBlank() && normalized.length <= 4096) { "Invalid push token." }
        latest.set(normalized)
        if (session.state.value == SessionState.SIGNED_IN) boundary.registerAndroidToken(normalized)
    }

    /** Called when a session starts: the device's current token now belongs to it. */
    suspend fun onSignedIn() {
        latest.get()?.let { boundary.registerAndroidToken(it) }
    }

    /** The user turned notifications off for this device. */
    suspend fun unregister() {
        latest.get()?.let { boundary.deactivateAndroidToken(it) }
    }
}

data class PushMessageData(
    val notificationId: String,
    val type: String,
) {
    companion object {
        fun from(data: Map<String, String>): PushMessageData? {
            if (data.keys.any { it !in setOf("notificationId", "type") }) return null
            val notificationId = data["notificationId"]?.trim().orEmpty()
            val type = data["type"]?.trim().orEmpty()
            if (notificationId.isBlank() || type.isBlank()) return null
            return PushMessageData(notificationId, type)
        }
    }
}
