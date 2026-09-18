package com.servacode.directory.feature.province

import com.servacode.directory.core.database.Loaded
import com.servacode.directory.core.database.PublicCache
import com.servacode.directory.core.database.cacheFirst
import com.servacode.directory.core.datastore.DirectoryPreferencesStore
import com.servacode.directory.core.model.Province
import com.servacode.directory.core.network.PublicApiBoundary
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** The provinces the backend serves. The app never keeps a list of its own. */
class ProvinceRepository @Inject constructor(
    private val cache: PublicCache,
    private val api: PublicApiBoundary,
    private val preferences: DirectoryPreferencesStore,
) {
    fun provinces(): Flow<Loaded<List<Province>>> = cacheFirst(
        read = { cache.provinces().takeIf { it.isNotEmpty() } },
        fetch = { api.provinces() },
        write = { cache.putProvinces(it) },
    )

    suspend fun select(id: String) = preferences.selectProvince(id)
}
