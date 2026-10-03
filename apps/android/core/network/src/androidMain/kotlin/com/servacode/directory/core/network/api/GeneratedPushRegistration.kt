package com.servacode.directory.core.network.api

import com.servacode.directory.api.apis.AccountApi
import com.servacode.directory.api.models.PushPlatformEnum
import com.servacode.directory.api.models.PushToken
import com.servacode.directory.api.models.PushTokenRegister
import com.servacode.directory.core.network.PushRegistrationBoundary

/**
 * [PushRegistrationBoundary] over the generated account operations (INT-057). The token is
 * sent to the backend and nowhere else: never logged, never cached, never shown.
 */
class GeneratedPushRegistration(authorized: GeneratedClient) : PushRegistrationBoundary {
    private val account by lazy { authorized.create<AccountApi>() }

    override suspend fun registerAndroidToken(token: String) {
        callForNoContent {
            account.accountPushTokenRegister(PushTokenRegister(platform = PushPlatformEnum.ANDROID, token = token))
        }
    }

    override suspend fun deactivateAndroidToken(token: String) {
        callForNoContent { account.accountPushTokenUnregister(PushToken(token = token)) }
    }
}
