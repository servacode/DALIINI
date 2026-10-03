package com.servacode.directory.core.auth

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class SessionState { SIGNED_IN, SIGNED_OUT }

/**
 * Owns the session: the access token in memory, the refresh material in the Keystore vault,
 * and the one refresh that may run at a time.
 *
 * The access token is never persisted. After process death it is recovered by refreshing,
 * which the first request that gets a 401 triggers.
 */
class SessionCoordinator(
    private val accessTokens: AccessTokenStore,
    private val refreshTokens: RefreshTokenVault,
    private val gateway: RefreshGateway,
) {
    private val refreshMutex = Mutex()
    private val _state = MutableStateFlow(
        if (refreshTokens.read()?.let(RefreshMaterial::decode) != null) {
            SessionState.SIGNED_IN
        } else {
            SessionState.SIGNED_OUT
        },
    )

    /** Signed out as soon as the session is cleared, so screens can return to sign-in. */
    val state: StateFlow<SessionState> = _state.asStateFlow()

    /**
     * Called after a 401 with the access token that failed. Returns the token to retry with,
     * or null when there is none.
     *
     * Concurrent callers queue on one mutex; whoever arrives after a successful refresh sees
     * that the token already changed and reuses it, so one expiry spends the secret once.
     */
    suspend fun refreshAfterUnauthorized(failedAccessToken: String?): String? = refreshMutex.withLock {
        val current = accessTokens.get()
        if (current != null && current != failedAccessToken) return@withLock current
        val material = refreshTokens.read()?.let(RefreshMaterial::decode) ?: return@withLock null
        try {
            val tokens = gateway.rotate(material.refreshToken)
            store(tokens)
            tokens.accessToken
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: RefreshRejectedException) {
            clear()
            null
        } catch (_: Exception) {
            // Offline or a server fault. The secret was not spent, so the session stays and
            // the next request can try again.
            null
        }
    }

    fun establish(tokens: SessionTokens) {
        store(tokens)
    }

    fun sessionId(): String? = refreshTokens.read()?.let(RefreshMaterial::decode)?.sessionId

    fun clear() {
        accessTokens.set(null)
        refreshTokens.clear()
        _state.value = SessionState.SIGNED_OUT
    }

    private fun store(tokens: SessionTokens) {
        refreshTokens.write(RefreshMaterial(tokens.sessionId, tokens.refreshToken).encode())
        accessTokens.set(tokens.accessToken)
        _state.value = SessionState.SIGNED_IN
    }
}
