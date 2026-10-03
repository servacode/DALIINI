package com.servacode.directory.feature.account

import com.servacode.directory.core.auth.SessionState
import com.servacode.directory.core.inject.Inject
import com.servacode.directory.core.model.AccountProfile
import com.servacode.directory.core.model.Province
import com.servacode.directory.core.network.OwnerUploadPayload
import kotlinx.coroutines.flow.StateFlow

class AccountUseCase @Inject constructor(
    private val repository: AccountRepository,
) {
    val session: StateFlow<SessionState> get() = repository.state
    suspend operator fun invoke(): Result<AccountProfile> = repository.profile()
    suspend fun provinces(): Result<List<Province>> = repository.provinces()

    /** Whether the profile shows "منشآتي" or the invitation to join. */
    suspend fun ownsFacility(): Boolean = repository.ownsFacility()

    /** The fields the backend accepts, and no others. */
    suspend fun update(
        displayName: String,
        provinceId: String?,
        address: String?,
    ): Result<AccountProfile> = repository.update(displayName, provinceId, address)

    suspend fun updateImage(payload: OwnerUploadPayload): Result<AccountProfile> =
        repository.updateImage(payload)

    suspend fun removeImage(): Result<AccountProfile> = repository.removeImage()

    suspend fun startPhoneChange(phone: String): Result<String> = repository.startPhoneChange(phone)

    suspend fun confirmPhoneChange(challengeId: String, code: String): Result<AccountProfile> =
        repository.confirmPhoneChange(challengeId, code)
    suspend fun logout() = repository.logout()
}

class DeleteAccountUseCase @Inject constructor(
    private val repository: AccountRepository,
) {
    suspend operator fun invoke(): Result<Unit> = repository.deleteAccount()
}
