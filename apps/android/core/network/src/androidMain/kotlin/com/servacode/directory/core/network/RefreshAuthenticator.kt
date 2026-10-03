package com.servacode.directory.core.network

import com.servacode.directory.core.auth.SessionCoordinator
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * Answers a 401 by refreshing once and retrying the request once.
 *
 * The token reported as failed is the one the request actually carried, not whatever the
 * store holds now: when several requests fail on the same expired token, the ones that
 * arrive after the first refresh see a newer token in the store and reuse it instead of
 * spending the secret again.
 */
class RefreshAuthenticator(
    private val sessionCoordinator: SessionCoordinator,
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.priorResponse != null) return null
        val failed = response.request.header("Authorization")?.removePrefix("Bearer ")
        val refreshed = runBlocking { sessionCoordinator.refreshAfterUnauthorized(failed) } ?: return null
        return response.request.newBuilder()
            .header("Authorization", "Bearer $refreshed")
            .build()
    }
}
