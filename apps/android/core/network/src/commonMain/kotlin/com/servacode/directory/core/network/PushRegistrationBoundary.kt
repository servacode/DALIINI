package com.servacode.directory.core.network

/**
 * Registers the device's FCM token with the backend. The mobile app never stores a provider
 * secret. Bound to the generated account operations by `GeneratedPushRegistration`.
 */
interface PushRegistrationBoundary {
    suspend fun registerAndroidToken(token: String)
    suspend fun deactivateAndroidToken(token: String)
}

