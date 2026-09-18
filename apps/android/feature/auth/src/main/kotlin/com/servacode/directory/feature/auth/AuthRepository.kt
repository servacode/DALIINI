package com.servacode.directory.feature.auth

import com.servacode.directory.core.auth.SessionCoordinator
import com.servacode.directory.core.auth.SessionState
import com.servacode.directory.core.model.AccountSession
import com.servacode.directory.core.model.AuthChallenge
import com.servacode.directory.core.network.AuthApiBoundary
import com.servacode.directory.core.network.SignOut
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

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

    suspend fun startRegistration(displayName: String, phone: String, provinceId: String): Result<AuthChallenge> =
        runCatching { api.registerStart(displayName.trim(), phone.trim(), provinceId) }

    suspend fun verifyRegistration(challengeId: String, code: String): Result<Unit> =
        runCatching { api.registerVerify(challengeId, code.trim()) }

    suspend fun completeRegistration(challengeId: String, password: String): Result<Unit> = runCatching {
        session.establish(api.registerComplete(challengeId, password))
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
