package com.servacode.directory.core.auth

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SessionCoordinator(
    private val accessTokens: AccessTokenStore,
    private val refreshTokens: RefreshTokenVault,
    private val gateway: RefreshGateway,
) {
    private val refreshMutex = Mutex()

    suspend fun refreshAfterUnauthorized(failedAccessToken: String?): String? = refreshMutex.withLock {
        val current = accessTokens.get()
        if (current != null && current != failedAccessToken) return@withLock current
        val refresh = refreshTokens.read() ?: return@withLock null
        runCatching { gateway.rotate(refresh) }
            .onSuccess { tokens ->
                refreshTokens.write(tokens.refreshToken)
                accessTokens.set(tokens.accessToken)
            }
            .getOrElse {
                clear()
                return@withLock null
            }
        accessTokens.get()
    }

    fun establish(tokens: SessionTokens) {
        refreshTokens.write(tokens.refreshToken)
        accessTokens.set(tokens.accessToken)
    }

    fun clear() {
        accessTokens.set(null)
        refreshTokens.clear()
    }
}
