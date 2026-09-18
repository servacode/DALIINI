package com.servacode.directory.core.network.api

/**
 * Where the app reaches the backend, as the build flavor configured it.
 *
 * `allowCleartext` is true only in the local flavor, which talks to a development server on
 * the emulator's host loopback. Every other flavor must use TLS, and no flavor may start
 * with a placeholder such as `<ROOT_DOMAIN>` still in the address: the app refuses to make a
 * request rather than send it somewhere undefined.
 */
data class ApiEnvironment(
    val baseUrl: String,
    val allowCleartext: Boolean = false,
) {
    /** The base URL with a trailing slash, or an exception naming what is wrong. */
    fun requireConfiguredBaseUrl(): String {
        val value = baseUrl.trim()
        if (value.isEmpty() || '<' in value || '>' in value) {
            throw ApiNotConfiguredException("API base URL is not configured.")
        }
        val secure = value.startsWith("https://")
        if (!secure && !(allowCleartext && value.startsWith("http://"))) {
            throw ApiNotConfiguredException("API base URL must use HTTPS.")
        }
        if ('?' in value || '#' in value) {
            throw ApiNotConfiguredException("API base URL must not carry a query or fragment.")
        }
        return if (value.endsWith("/")) value else "$value/"
    }
}

/** The build has no usable backend address; see [ApiEnvironment.requireConfiguredBaseUrl]. */
class ApiNotConfiguredException(message: String) : IllegalStateException(message)

/** How this install names itself in the session list the user sees. */
data class ClientIdentity(val deviceName: String)
