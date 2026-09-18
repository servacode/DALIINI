package com.servacode.directory.core.network

import com.servacode.directory.core.auth.SessionTokens
import com.servacode.directory.core.model.AccountSession
import com.servacode.directory.core.model.AuthChallenge

/**
 * Sign-in, registration, recovery and session management, over the generated client.
 *
 * Registration and recovery are three steps each: start (the backend sends a one-time code),
 * verify the code, then complete. The code itself never leaves the screen that typed it.
 */
interface AuthApiBoundary {
    suspend fun login(phone: String, password: String): SessionTokens
    suspend fun registerStart(displayName: String, phone: String, provinceId: String): AuthChallenge
    suspend fun registerVerify(challengeId: String, code: String)
    suspend fun registerComplete(challengeId: String, password: String): SessionTokens
    suspend fun recoveryStart(phone: String): AuthChallenge
    suspend fun recoveryVerify(challengeId: String, code: String)
    suspend fun recoveryReset(challengeId: String, password: String)
    suspend fun logout(sessionId: String)
    suspend fun sessions(): List<AccountSession>
    suspend fun revokeSession(sessionId: String)
}
