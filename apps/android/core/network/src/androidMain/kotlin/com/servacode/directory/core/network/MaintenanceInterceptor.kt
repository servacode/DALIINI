package com.servacode.directory.core.network

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Reads maintenance out of every response, on both the anonymous and the signed-in client.
 *
 * It never changes the response and never throws: the error envelope still reaches
 * `ApiErrorMapper`, so a screen that was loading fails its request normally and the shell
 * decides what to show. The body is peeked, not consumed.
 */
class MaintenanceInterceptor @Inject constructor(
    private val state: MaintenanceState,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.code == HTTP_SERVICE_UNAVAILABLE) {
            detect(response)?.let(state::enter)
        } else if (response.isSuccessful && state.active && !isPlatformStatus(response)) {
            // The status endpoint answers 200 during maintenance too; its body decides, and the
            // probe that asked reads it.
            state.clear()
        }
        return response
    }

    private fun isPlatformStatus(response: Response): Boolean =
        response.request.url.encodedPath.endsWith(PLATFORM_STATUS_SUFFIX)

    private fun detect(response: Response): MaintenanceStatus.Active? {
        val body = runCatching { response.peekBody(MAX_ENVELOPE_BYTES).string() }.getOrNull()
        return MaintenanceEnvelope.parse(
            body = body,
            retryAfterHeader = response.header(RETRY_AFTER_HEADER),
            requestIdHeader = response.header(REQUEST_ID_HEADER),
        )
    }

    companion object {
        const val HTTP_SERVICE_UNAVAILABLE = 503
        const val RETRY_AFTER_HEADER = "Retry-After"
        private const val REQUEST_ID_HEADER = "X-Request-ID"
        private const val MAX_ENVELOPE_BYTES = 64L * 1024
        private const val PLATFORM_STATUS_SUFFIX = "/platform/status/"
    }
}
