package com.servacode.directory.core.network

import com.servacode.directory.core.auth.SessionCoordinator
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

/**
 * Ends the session on this device, whatever the network does.
 *
 * The backend is asked to revoke the session first, so its refresh secret and access token
 * stop working at once. If it cannot be reached the session still ends here: the access
 * token leaves memory, the refresh material leaves the Keystore vault, and the backend lets
 * the secret expire. Public cached data is left alone; nothing private was ever cached.
 */
class SignOut @Inject constructor(
    private val api: AuthApiBoundary,
    private val session: SessionCoordinator,
) {
    suspend operator fun invoke() {
        val sessionId = session.sessionId()
        try {
            if (sessionId != null) api.logout(sessionId)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // Revocation is best effort; clearing below is not.
        } finally {
            session.clear()
        }
    }
}
