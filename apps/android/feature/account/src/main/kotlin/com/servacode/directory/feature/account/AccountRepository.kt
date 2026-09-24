package com.servacode.directory.feature.account

import com.servacode.directory.core.auth.SessionCoordinator
import com.servacode.directory.core.auth.SessionState
import com.servacode.directory.core.datastore.DirectoryPreferencesStore
import com.servacode.directory.core.model.AccountProfile
import com.servacode.directory.core.model.Province
import com.servacode.directory.core.network.OwnerApiBoundary
import com.servacode.directory.core.network.PublicApiBoundary
import com.servacode.directory.core.network.SignOut
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * The signed-in account. A profile image is not offered: the contract has no operation to
 * upload one (INT-017), and the app does not invent one.
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
    suspend fun update(displayName: String, provinceId: String?): Result<AccountProfile> = runCatching {
        api.updateProfile(displayName = displayName, provinceId = provinceId).also {
            if (provinceId != null) preferences.selectProvince(provinceId)
        }
    }

    /** The backend revokes every session on deletion, so this device's session ends too. */
    suspend fun deleteAccount(): Result<Unit> = runCatching {
        api.requestAccountDeletion()
        session.clear()
    }
}
