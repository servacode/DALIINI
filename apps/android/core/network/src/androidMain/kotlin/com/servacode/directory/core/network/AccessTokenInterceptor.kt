package com.servacode.directory.core.network

import com.servacode.directory.core.auth.AccessTokenStore
import okhttp3.Interceptor
import okhttp3.Response
import java.util.UUID
import javax.inject.Inject

/** Adds the in-memory access token. Only the signed-in client carries this interceptor. */
class AccessTokenInterceptor @Inject constructor(
    private val accessTokens: AccessTokenStore,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = accessTokens.get() ?: return chain.proceed(chain.request())
        return chain.proceed(chain.request().newBuilder().header("Authorization", "Bearer $token").build())
    }
}

/**
 * Gives every request an `X-Request-ID`, keeping one the caller already set.
 *
 * The backend echoes it into its envelope and logs, which is what lets a support report be
 * matched to a server-side trace.
 */
class RequestIdInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        if (original.header(HEADER) != null) return chain.proceed(original)
        return chain.proceed(original.newBuilder().header(HEADER, UUID.randomUUID().toString()).build())
    }

    private companion object {
        const val HEADER = "X-Request-ID"
    }
}
