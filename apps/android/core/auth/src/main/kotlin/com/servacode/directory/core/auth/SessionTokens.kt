package com.servacode.directory.core.auth

data class SessionTokens(
    val accessToken: String,
    val refreshToken: String,
)

interface RefreshGateway {
    suspend fun rotate(refreshToken: String): SessionTokens
}
