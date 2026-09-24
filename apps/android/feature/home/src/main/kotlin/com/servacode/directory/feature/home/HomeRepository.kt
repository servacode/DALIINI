package com.servacode.directory.feature.home

import com.servacode.directory.core.database.Loaded
import com.servacode.directory.core.database.PublicCache
import com.servacode.directory.core.database.cacheFirst
import com.servacode.directory.core.datastore.DirectoryPreferencesStore
import com.servacode.directory.core.location.LocationProvider
import com.servacode.directory.core.location.LocationResult
import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.AppException
import com.servacode.directory.core.model.FacilitySummary
import com.servacode.directory.core.model.HomeSnapshot
import com.servacode.directory.core.model.Page
import com.servacode.directory.core.model.Province
import com.servacode.directory.core.network.LocationNameResolver
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

/**
 * Where the app believes the user is, and which province its lists are therefore scoped by.
 *
 * The province comes first from what the platform resolved for the device's own position, and
 * only then from what the user once chose. That is what keeps a normal user from ever meeting
 * a province picker: the app knows where they are, says so, and they can still change it.
 */
data class HomePlace(
    val label: String?,
    val provinceId: String?,
    /** True when the province was resolved from a position rather than read from storage. */
    val fromLocation: Boolean = false,
)

class HomeRepository @Inject constructor(
    private val cache: PublicCache,
    private val api: PublicApiBoundary,
    private val preferences: DirectoryPreferencesStore,
    private val locationProvider: LocationProvider,
    private val places: LocationNameResolver,
) {
    /**
     * The place the header shows, resolved in the order the product requires: the device's own
     * position when it is allowed and known, then the last place resolved for this device, then
     * the province the user chose. Only when none of those exists does the app have to ask.
     *
     * A refusal of the location permission is not a dead end here — it simply means the second
     * and third answers are the ones that apply.
     */
    suspend fun place(): HomePlace {
        val stored = preferences.values.first()
        val fix = locationProvider.lastKnown()
            ?: (locationProvider.current() as? LocationResult.Available)?.fix
        if (fix != null) {
            val resolved = places.resolve(fix.latitude, fix.longitude)
            val label = resolved?.label
            if (label != null) {
                val provinceId = resolved.province?.id
                preferences.rememberPlace(label, provinceId)
                return HomePlace(label = label, provinceId = provinceId, fromLocation = true)
            }
        }
        if (stored.placeLabel != null) {
            return HomePlace(label = stored.placeLabel, provinceId = stored.placeProvinceId)
        }
        return HomePlace(label = null, provinceId = stored.selectedProvinceId)
    }

    /**
     * The list under the chips, from the backend's own directory query.
     *
     * Never cached: what is open at this minute, or on tonight's roster, is a view of the
     * moment. The category list already treats its own filtered views the same way.
     */
    suspend fun filtered(
        provinceId: String,
        categoryId: String?,
        filters: HomeFilters,
        cursor: String? = null,
    ): Page<FacilitySummary> {
        val fix = locationProvider.lastKnown()
        return api.directory(filters.query(provinceId, categoryId, fix), cursor)
    }

    /** Whether a position is known at all, which is what decides if "nearest" can be offered. */
    fun hasLocation(): Boolean = locationProvider.lastKnown() != null

    /**
     * How many messages the account has not read, or zero when nobody is signed in.
     *
     * A failure is zero rather than an error: the badge is an aside, and Home must not refuse
     * to load because a count could not be fetched.
     */
    suspend fun unreadMessages(): Int = runCatching { api.unreadMessageCount() }.getOrDefault(0)
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
