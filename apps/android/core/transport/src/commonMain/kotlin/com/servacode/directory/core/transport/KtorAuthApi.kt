package com.servacode.directory.core.transport

import com.servacode.directory.api.multiplatform.apis.AuthApi
import com.servacode.directory.api.multiplatform.models.ChallengeVerify
import com.servacode.directory.api.multiplatform.models.Login
import com.servacode.directory.api.multiplatform.models.LogoutRequest
import com.servacode.directory.api.multiplatform.models.RecoveryReset
import com.servacode.directory.api.multiplatform.models.RecoveryStart
import com.servacode.directory.api.multiplatform.models.Refresh
import com.servacode.directory.api.multiplatform.models.RegisterComplete
import com.servacode.directory.api.multiplatform.models.RegisterStart
import com.servacode.directory.core.auth.RefreshGateway
import com.servacode.directory.core.auth.RefreshRejectedException
import com.servacode.directory.core.auth.SessionTokens
import com.servacode.directory.core.model.AccountSession
import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.AppException
import com.servacode.directory.core.model.AuthChallenge
import com.servacode.directory.core.network.AuthApiBoundary

/**
 * [AuthApiBoundary] over the multiplatform client: the port of Android's `GeneratedAuthApi`.
 *
 * Two clients, on purpose. Sign-in, registration, recovery and refresh go through the anonymous
 * client, which never refreshes: a wrong password must come back as a wrong password, not
 * trigger a refresh and a retry. Logout and session management need the caller's access token
 * and go through the signed-in client.
 */
class KtorAuthApi(
    private val clients: TransportClients,
    private val deviceName: String,
) : AuthApiBoundary {
    private val open by lazy { AuthApi(clients.baseUrl, clients.anonymous) }
    private val signedIn by lazy { AuthApi(clients.baseUrl, clients.authorized) }

    override suspend fun login(phone: String, password: String): SessionTokens = call {
        open.authLogin(
            Login(phone = phone, password = password, platform = clients.platform.wire, deviceName = deviceName),
        )
    }.toDomain()

    override suspend fun registerStart(phone: String, provinceId: String): AuthChallenge =
        call {
            open.authRegisterStart(
                RegisterStart(phone = phone, provinceId = uuid(provinceId)),
            )
        }.toDomain()

    override suspend fun registerVerify(challengeId: String, code: String) {
        val verified = call {
            open.authRegisterVerify(ChallengeVerify(challengeId = uuid(challengeId), code = code))
        }
        if (!verified.verified) throw AppException(AppError(AppError.Kind.VALIDATION, code = "VALIDATION_ERROR"))
    }

    override suspend fun registerComplete(
        challengeId: String,
        displayName: String,
        password: String,
    ): SessionTokens = call {
        open.authRegisterComplete(
            RegisterComplete(
                challengeId = uuid(challengeId),
                displayName = displayName,
                password = password,
                platform = clients.platform.wire,
                deviceName = deviceName,
            ),
        )
    }.toDomain()

    override suspend fun recoveryStart(phone: String): AuthChallenge =
        call { open.authRecoveryStart(RecoveryStart(phone = phone)) }.toDomain()

    override suspend fun recoveryVerify(challengeId: String, code: String) {
        val verified = call {
            open.authRecoveryVerify(ChallengeVerify(challengeId = uuid(challengeId), code = code))
        }
        if (!verified.verified) throw AppException(AppError(AppError.Kind.VALIDATION, code = "VALIDATION_ERROR"))
    }

    override suspend fun recoveryReset(challengeId: String, password: String) {
        callForNoContent {
            open.authRecoveryReset(RecoveryReset(challengeId = uuid(challengeId), password = password))
        }
    }

    override suspend fun logout(sessionId: String) {
        callForNoContent { signedIn.authLogout(LogoutRequest(sessionId = uuid(sessionId))) }
    }

    override suspend fun sessions(): List<AccountSession> =
        call { signedIn.authSessionsList() }.items.map { it.toDomain() }

    override suspend fun revokeSession(sessionId: String) {
        callForNoContent { signedIn.authSessionRevoke(uuid(sessionId)) }
    }
}

/**
 * Rotates the refresh secret through the generated `authRefresh`, on the anonymous client: the
 * port of Android's `GeneratedRefreshGateway`.
 *
 * A 400 or 401 means the backend refused the secret, and the session ends. Anything else —
 * no network, a timeout, a 5xx — is transient: the secret was not spent, so it is kept.
 */
class KtorRefreshGateway(clients: TransportClients) : RefreshGateway {
    private val auth by lazy { AuthApi(clients.baseUrl, clients.anonymous) }

    override suspend fun rotate(refreshToken: String): SessionTokens = try {
        call { auth.authRefresh(Refresh(refreshToken = refreshToken)) }.toDomain()
    } catch (failure: AppException) {
        when (failure.error.kind) {
            AppError.Kind.UNAUTHENTICATED, AppError.Kind.VALIDATION -> throw RefreshRejectedException()
            else -> throw failure
        }
    }
}
