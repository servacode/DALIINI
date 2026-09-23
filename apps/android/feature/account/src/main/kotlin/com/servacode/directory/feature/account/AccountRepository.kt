package com.servacode.directory.feature.account

import com.servacode.directory.core.auth.SessionCoordinator
import com.servacode.directory.core.auth.SessionState
import com.servacode.directory.core.datastore.DirectoryPreferencesStore
import com.servacode.directory.core.model.AccountProfile
import com.servacode.directory.core.model.Province
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
    private val session: SessionCoordinator,
    private val preferences: DirectoryPreferencesStore,
    private val signOut: SignOut,
) {
    val state: StateFlow<SessionState> get() = session.state

    suspend fun profile(): Result<AccountProfile> = runCatching { api.profile() }

    suspend fun provinces(): Result<List<Province>> = runCatching { api.provinces() }

    suspend fun logout() = signOut()

    /** Moves the account to another province and browses it from now on. */
    suspend fun changeProvince(provinceId: String): Result<AccountProfile> = runCatching {
        api.updateProfile(provinceId = provinceId).also { preferences.selectProvince(provinceId) }
    }

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
