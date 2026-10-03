package com.servacode.directory.core.transport

import com.servacode.directory.api.multiplatform.apis.AccountApi
import com.servacode.directory.api.multiplatform.models.PushPlatformEnum
import com.servacode.directory.api.multiplatform.models.PushToken
import com.servacode.directory.api.multiplatform.models.PushTokenRegister
import com.servacode.directory.core.network.PushRegistrationBoundary

/**
 * [PushRegistrationBoundary] over the multiplatform account operations (INT-057): the port of
 * Android's `GeneratedPushRegistration`, on the signed-in client. The token is sent to the
 * backend and nowhere else: never logged, never cached, never shown.
 */
class KtorPushRegistration(private val clients: TransportClients) : PushRegistrationBoundary {
    private val account by lazy { AccountApi(clients.baseUrl, clients.authorized) }

    override suspend fun registerAndroidToken(token: String) {
        callForNoContent {
            val platform = PushPlatformEnum.valueOf(clients.platform.name)
            account.accountPushTokenRegister(PushTokenRegister(platform = platform, token = token))
        }
    }

    override suspend fun deactivateAndroidToken(token: String) {
        callForNoContent { account.accountPushTokenUnregister(PushToken(token = token)) }
    }
}
