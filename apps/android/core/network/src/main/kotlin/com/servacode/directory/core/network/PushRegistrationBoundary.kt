package com.servacode.directory.core.network

/**
 * Registers the device's FCM token with the backend. The mobile app never stores a provider
 * secret.
 *
 * Not bound: the contract has no push-registration operation, so there is nothing in the
 * generated client to call (see the register entry for push registration).
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
