package com.servacode.directory.core.network

import com.servacode.directory.core.auth.AccessTokenStore
import okhttp3.Interceptor
import okhttp3.Response
import java.util.UUID
import javax.inject.Inject

class AccessTokenInterceptor @Inject constructor(
    private val accessTokens: AccessTokenStore,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val requestId = original.header("X-Request-ID") ?: UUID.randomUUID().toString()
        val builder = original.newBuilder().header("X-Request-ID", requestId)
        accessTokens.get()?.let { builder.header("Authorization", "Bearer $it") }
        return chain.proceed(builder.build())
    }
}
