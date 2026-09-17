package com.servacode.directory.core.network

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PushRegistrationCoordinator @Inject constructor(
    private val boundary: PushRegistrationBoundary,
) {
    suspend fun onTokenAvailable(token: String) {
        val normalized = token.trim()
        require(normalized.isNotBlank() && normalized.length <= 4096) { "Invalid push token." }
        boundary.registerAndroidToken(normalized)
    }

    suspend fun beforeLogout(currentToken: String?) {
        val normalized = currentToken?.trim().orEmpty()
        if (normalized.isNotBlank()) boundary.deactivateAndroidToken(normalized)
    }
}

data class PushMessageData(
    val notificationId: String,
    val type: String,
) {
    companion object {
        fun from(data: Map<String, String>): PushMessageData? {
            if (data.keys.any { it !in setOf("notificationId", "type") }) return null
            val notificationId = data["notificationId"]?.trim().orEmpty()
            val type = data["type"]?.trim().orEmpty()
            if (notificationId.isBlank() || type.isBlank()) return null
            return PushMessageData(notificationId, type)
        }
    }
}
