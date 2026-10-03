package com.servacode.directory.feature.account

import com.servacode.directory.core.auth.SessionCoordinator
import com.servacode.directory.core.auth.SessionState
import com.servacode.directory.core.datastore.DirectoryPreferencesStore
import com.servacode.directory.core.inject.Inject
import com.servacode.directory.core.model.AccountProfile
import com.servacode.directory.core.model.Province
import com.servacode.directory.core.network.OwnerApiBoundary
import com.servacode.directory.core.network.OwnerUploadPayload
import com.servacode.directory.core.network.PublicApiBoundary
import com.servacode.directory.core.network.SignOut
import kotlinx.coroutines.flow.StateFlow

/**
 * The signed-in account: what it is, what it may say about itself, and how it moves house.
 *
 * A picture is offered now because the contract has an operation for one. It did not when this
 * class was written, and the app did not invent it then.
 */
class AccountRepository @Inject constructor(
    private val api: PublicApiBoundary,
    private val owner: OwnerApiBoundary,
    private val session: SessionCoordinator,
    private val preferences: DirectoryPreferencesStore,
    private val signOut: SignOut,
) {
    val state: StateFlow<SessionState> get() = session.state

    suspend fun profile(): Result<AccountProfile> = runCatching { api.profile() }

    suspend fun provinces(): Result<List<Province>> = runCatching { api.provinces() }

    /**
     * Whether this account has joined as an owner yet.
     *
     * Asked of the owner list rather than of a flag, because the list is the fact: an account
     * that owns a facility has one, and one that has never joined has none. A failure is read
     * as "not yet", which shows the invitation — offering to join to someone who already has
     * is a smaller wrong than hiding their own facilities behind a network hiccup would be.
     */
    suspend fun ownsFacility(): Boolean =
        runCatching { owner.facilities().isNotEmpty() }.getOrDefault(false)

    suspend fun logout() = signOut()

    /**
     * The profile fields the backend accepts. A province chosen here is also what this device
     * browses, so the two never disagree.
     */
    suspend fun update(
        displayName: String,
        provinceId: String?,
        address: String?,
    ): Result<AccountProfile> = runCatching {
        api.updateProfile(displayName = displayName, provinceId = provinceId, address = address).also {
            if (provinceId != null) preferences.selectProvince(provinceId)
        }
    }

    /** Replaces the picture on the account, or takes it away. */
    suspend fun updateImage(payload: OwnerUploadPayload): Result<AccountProfile> =
        runCatching { api.updateProfileImage(payload) }

    suspend fun removeImage(): Result<AccountProfile> = runCatching { api.removeProfileImage() }

    /**
     * Moving the account to another number, in two steps.
     *
     * The code goes to the number being claimed. Confirming it ends every session, this device's
     * included, so the app signs in again afterwards — the phone is how the account signs in.
     */
    suspend fun startPhoneChange(phone: String): Result<String> =
        runCatching { api.startPhoneChange(phone) }

    suspend fun confirmPhoneChange(challengeId: String, code: String): Result<AccountProfile> =
        runCatching { api.confirmPhoneChange(challengeId, code) }

    /** The backend revokes every session on deletion, so this device's session ends too. */
    suspend fun deleteAccount(): Result<Unit> = runCatching {
        api.requestAccountDeletion()
        session.clear()
    }
}
