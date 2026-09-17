package com.servacode.directory.core.network

/**
 * Implemented by the generated P10 Kotlin API client. The mobile app never stores a provider secret.
 */
interface PushRegistrationBoundary {
    suspend fun registerAndroidToken(token: String)
    suspend fun deactivateAndroidToken(token: String)
}

object UnboundPushRegistrationBoundary : PushRegistrationBoundary {
    private fun unavailable(): Nothing = throw GeneratedClientRequiredException()
    override suspend fun registerAndroidToken(token: String): Unit = unavailable()
    override suspend fun deactivateAndroidToken(token: String): Unit = unavailable()
}
