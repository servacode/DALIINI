package com.servacode.directory.core.network

import com.servacode.directory.core.auth.AccessTokenStore
import com.servacode.directory.core.auth.SessionCoordinator
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class RefreshAuthenticator(
    private val accessTokens: AccessTokenStore,
    private val sessionCoordinator: SessionCoordinator,
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2) return null
        val failed = accessTokens.get()
        val refreshed = runBlocking { sessionCoordinator.refreshAfterUnauthorized(failed) } ?: return null
        return response.request.newBuilder()
            .header("Authorization", "Bearer $refreshed")
            .build()
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count += 1
            prior = prior.priorResponse
        }
        return count
    }
}
