package com.servacode.directory.feature.home

import com.servacode.directory.core.database.Loaded
import com.servacode.directory.core.database.PublicCache
import com.servacode.directory.core.database.cacheFirst
import com.servacode.directory.core.datastore.DirectoryPreferencesStore
import com.servacode.directory.core.location.LocationProvider
import com.servacode.directory.core.location.LocationResult
import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.AppException
import com.servacode.directory.core.model.HomeSnapshot
import com.servacode.directory.core.model.Province
import com.servacode.directory.core.network.PublicApiBoundary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

sealed interface HomeLoad {
    data object ProvinceRequired : HomeLoad
    data class Snapshot(val loaded: Loaded<HomeSnapshot>) : HomeLoad
}

class HomeRepository @Inject constructor(
    private val cache: PublicCache,
    private val api: PublicApiBoundary,
    private val preferences: DirectoryPreferencesStore,
    private val locationProvider: LocationProvider,
) {
    /**
     * The cached snapshot first, then the backend's. Location is resolved only after the cache
     * is on screen, and only if the user allowed it; without it the home is province-wide.
     */
    fun load(): Flow<HomeLoad> = flow {
        val provinceId = preferences.values.first().selectedProvinceId
        if (provinceId == null) {
            emit(HomeLoad.ProvinceRequired)
            return@flow
        }
        emitAll(
            cacheFirst(
                read = { cache.home(provinceId) },
                fetch = {
                    val province = servedProvince(provinceId)
                        ?: throw AppException(AppError(AppError.Kind.NOT_FOUND, code = PROVINCE_NOT_SERVED))
                    val fix = when (val location = locationProvider.current()) {
                        is LocationResult.Available -> location.fix
                        else -> locationProvider.lastKnown()
                    }
                    api.home(province, fix?.latitude, fix?.longitude)
                },
                write = { cache.putHome(it) },
            ).map { loaded ->
                val error = (loaded as? Loaded.Failed)?.error ?: (loaded as? Loaded.Stale)?.error
                if (error?.code == PROVINCE_NOT_SERVED) HomeLoad.ProvinceRequired
                else HomeLoad.Snapshot(loaded)
            },
        )
    }

    /** The selected province, if the backend still serves it. */
    private suspend fun servedProvince(id: String): Province? =
        cache.provinces().firstOrNull { it.id == id }
            ?: api.provinces().also { cache.putProvinces(it) }.firstOrNull { it.id == id }

    companion object {
        /** The selected province is no longer served, so the user must choose again. */
        const val PROVINCE_NOT_SERVED = "PROVINCE_NOT_SERVED"
    }
}
