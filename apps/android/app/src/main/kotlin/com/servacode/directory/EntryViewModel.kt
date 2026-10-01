package com.servacode.directory

import androidx.lifecycle.ViewModel
import com.servacode.directory.core.database.PublicCache
import com.servacode.directory.core.datastore.DirectoryPreferencesStore
import com.servacode.directory.core.model.Province
import com.servacode.directory.core.network.PublicApiBoundary
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** What an App Link needs that the navigation graph does not have: the provinces, by code. */
@HiltViewModel
class EntryViewModel @Inject constructor(
    private val cache: PublicCache,
    private val api: PublicApiBoundary,
    private val preferences: DirectoryPreferencesStore,
) : ViewModel() {
    /**
     * Chooses the province a `/{code}` link names, as if the reader had picked it. False when no
     * served province has that code — the app then opens Home as it was.
     */
    suspend fun selectProvince(code: String): Boolean {
        val province = find(code, runCatching { cache.provinces() }.getOrDefault(emptyList()))
            // A cache from before provinces carried their code, or no cache yet: ask once.
            ?: runCatching { api.provinces().also { cache.putProvinces(it) } }.getOrNull()?.let { find(code, it) }
            ?: return false
        preferences.selectProvince(province.id)
        return true
    }

    private fun find(code: String, provinces: List<Province>): Province? =
        provinces.firstOrNull { it.code.equals(code, ignoreCase = true) }
}
