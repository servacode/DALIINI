package com.servacode.directory.core.auth

data class SessionTokens(
    val accessToken: String,
    val refreshToken: String,
    /** Needed to revoke this session on logout; kept with the refresh secret. */
    val sessionId: String,
)

interface RefreshGateway {
    /**
     * Exchanges a refresh secret for a new pair.
     *
     * Throws [RefreshRejectedException] when the backend refused the secret, which ends the
     * session. Any other failure — offline, timeout, server error — is transient and leaves
     * the session in place.
     */
    suspend fun rotate(refreshToken: String): SessionTokens
}

/** The backend refused the refresh secret: expired, revoked, replayed or unknown. */
class RefreshRejectedException : Exception("The refresh secret was refused.")

/**
 * What the Keystore vault holds: the refresh secret and the session it belongs to.
 *
 * Encoded as `v1`, the session id and the secret on separate lines. Neither a UUID nor the
 * backend's url-safe secret can contain a line break.
 */
internal data class RefreshMaterial(val sessionId: String, val refreshToken: String) {
    fun encode(): String = "$VERSION\n$sessionId\n$refreshToken"

    companion object {
        private const val VERSION = "v1"

        fun decode(value: String): RefreshMaterial? {
            val parts = value.split('\n')
            if (parts.size != 3 || parts[0] != VERSION) return null
            if (parts[1].isBlank() || parts[2].isBlank()) return null
            return RefreshMaterial(sessionId = parts[1], refreshToken = parts[2])
        }
    }
}
