package com.servacode.directory.feature.auth

import com.servacode.directory.core.auth.SessionCoordinator
import com.servacode.directory.core.auth.SessionState
import com.servacode.directory.core.inject.Inject
import com.servacode.directory.core.model.AccountSession
import com.servacode.directory.core.model.AuthChallenge
import com.servacode.directory.core.network.AuthApiBoundary
import com.servacode.directory.core.network.SignOut
import kotlinx.coroutines.flow.StateFlow

/**
 * Sign-in, registration, recovery and sign-out.
 *
 * A successful sign-in or registration hands the session to [SessionCoordinator], which keeps
 * the access token in memory and the refresh material in the Keystore vault. Passwords and
 * one-time codes pass through here and are never stored.
 */
class AuthRepository @Inject constructor(
    private val api: AuthApiBoundary,
    private val session: SessionCoordinator,
) {
    val state: StateFlow<SessionState> get() = session.state

    suspend fun login(phone: String, password: String): Result<Unit> = runCatching {
        session.establish(api.login(phone.trim(), password))
    }

    /** The number and where the account will live. The name comes after the code is proved. */
    suspend fun startRegistration(phone: String, provinceId: String): Result<AuthChallenge> =
        runCatching { api.registerStart(phone.trim(), provinceId) }

    suspend fun verifyRegistration(challengeId: String, code: String): Result<Unit> =
        runCatching { api.registerVerify(challengeId, code.trim()) }

    suspend fun completeRegistration(
        challengeId: String,
        displayName: String,
        password: String,
    ): Result<Unit> = runCatching {
        session.establish(api.registerComplete(challengeId, displayName.trim(), password))
    }

    suspend fun startRecovery(phone: String): Result<AuthChallenge> = runCatching { api.recoveryStart(phone.trim()) }

    suspend fun verifyRecovery(challengeId: String, code: String): Result<Unit> =
        runCatching { api.recoveryVerify(challengeId, code.trim()) }

    suspend fun resetPassword(challengeId: String, password: String): Result<Unit> =
        runCatching { api.recoveryReset(challengeId, password) }

    suspend fun sessions(): Result<List<AccountSession>> = runCatching { api.sessions() }

    /** See [SignOut]: revoke if reachable, clear the device always. */
    suspend fun logout() = SignOut(api, session)()
}
