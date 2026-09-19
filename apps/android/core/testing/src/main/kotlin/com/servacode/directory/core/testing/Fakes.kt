package com.servacode.directory.core.testing

import com.servacode.directory.core.database.PublicCache
import com.servacode.directory.core.datastore.DirectoryPreferences
import com.servacode.directory.core.datastore.DirectoryPreferencesStore
import com.servacode.directory.core.datastore.LocationPreference
import com.servacode.directory.core.location.LocationFix
import com.servacode.directory.core.location.LocationProvider
import com.servacode.directory.core.location.LocationResult
import com.servacode.directory.core.model.AccountProfile
import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.AppException
import com.servacode.directory.core.model.Category
import com.servacode.directory.core.model.FacilityDetail
import com.servacode.directory.core.model.FacilitySummary
import com.servacode.directory.core.model.HomeSnapshot
import com.servacode.directory.core.model.Page
import com.servacode.directory.core.model.Province
import com.servacode.directory.core.model.PublicMapFacility
import com.servacode.directory.core.model.UserRating
import com.servacode.directory.core.network.DirectoryQuery
import com.servacode.directory.core.network.PublicApiBoundary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow

/**
 * The public cache with the Room source's semantics: provinces are replaced, a directory's
 * first page replaces the list and later pages append at their offset.
 */
class FakePublicCache : PublicCache {
    var provinces = listOf<Province>()
    val homes = mutableMapOf<String, HomeSnapshot>()
    val details = mutableMapOf<String, FacilityDetail>()
    private val rows = mutableMapOf<Pair<String, String>, MutableMap<Int, FacilitySummary>>()
    val writes = mutableListOf<String>()

    override suspend fun provinces(): List<Province> = provinces
    override suspend fun putProvinces(values: List<Province>) {
        writes += "provinces"
        provinces = values
    }

    override suspend fun home(provinceId: String): HomeSnapshot? = homes[provinceId]
    override suspend fun putHome(value: HomeSnapshot) {
        writes += "home:${value.province.id}"
        homes[value.province.id] = value
    }

    override suspend fun directory(provinceId: String, categoryId: String): List<FacilitySummary> =
        rows[provinceId to categoryId].orEmpty().toSortedMap().values.toList()

    override suspend fun putDirectoryPage(
        values: List<FacilitySummary>,
        provinceId: String,
        categoryId: String,
        offset: Int,
    ) {
        writes += "directory:$provinceId:$categoryId@$offset"
        val list = rows.getOrPut(provinceId to categoryId) { mutableMapOf() }
        if (offset == 0) list.clear()
        values.forEachIndexed { index, value -> list[offset + index] = value }
    }

    override suspend fun facility(id: String): FacilityDetail? = details[id]
    override suspend fun putFacility(value: FacilityDetail, provinceId: String) {
        writes += "facility:${value.summary.id}"
        details[value.summary.id] = value
    }
}

class FakePreferences(
    selectedProvinceId: String? = null,
    welcomeCompleted: Boolean = false,
) : DirectoryPreferencesStore {
    private val state = MutableStateFlow(
        DirectoryPreferences(selectedProvinceId = selectedProvinceId, welcomeCompleted = welcomeCompleted),
    )
    override val values: Flow<DirectoryPreferences> = state

    override suspend fun selectProvince(id: String) {
        state.value = state.value.copy(selectedProvinceId = id)
    }

    override suspend fun setLocationPreference(value: LocationPreference) {
        state.value = state.value.copy(locationPreference = value)
    }

    override suspend fun setWelcomeCompleted() {
        state.value = state.value.copy(welcomeCompleted = true)
    }
}

/** Location as the user left it: [fix] when they allowed it, nothing otherwise. */
class FakeLocation(private val fix: LocationFix? = null) : LocationProvider {
    override suspend fun current(timeoutMillis: Long): LocationResult =
        fix?.let { LocationResult.Available(it) } ?: LocationResult.PermissionDenied

    override fun lastKnown(): LocationFix? = fix
    override fun updates(minTimeMillis: Long): Flow<LocationResult> = emptyFlow()
}

fun fix(latitude: Double, longitude: Double) = LocationFix(
    latitude = latitude,
    longitude = longitude,
    accuracyMeters = 10f,
    precise = true,
    capturedAtEpochMillis = 0,
)

fun facility(id: String, categoryId: String = "pharmacy") = FacilitySummary(
    id = id,
    nameAr = "صيدلية $id",
    category = Category(id = categoryId, nameAr = "صيدلية"),
)

val offline = AppException(AppError(AppError.Kind.OFFLINE))

/**
 * A [PublicApiBoundary] whose answers each test sets, and which records what was asked. An
 * unset answer fails as if offline, so a test states every call it expects to succeed.
 */
class ScriptedPublicApi : PublicApiBoundary {
    var provincesAnswer: () -> List<Province> = { throw offline }
    var homeAnswer: (Province) -> HomeSnapshot = { throw offline }
    var directoryAnswer: (DirectoryQuery, String?) -> Page<FacilitySummary> = { _, _ -> throw offline }
    var searchAnswer: (String, String?) -> Page<FacilitySummary> = { _, _ -> throw offline }
    var facilityAnswer: (String) -> FacilityDetail = { throw offline }
    var ratingsAnswer: () -> List<UserRating> = { throw offline }
    var upsertAnswer: (String, Int) -> Int = { _, stars -> stars }
    val calls = mutableListOf<String>()

    override suspend fun provinces(): List<Province> = provincesAnswer().also { calls += "provinces" }
    override suspend fun categories(provinceId: String): List<Category> = throw offline
    override suspend fun home(province: Province, latitude: Double?, longitude: Double?): HomeSnapshot {
        calls += "home:${province.id}:$latitude:$longitude"
        return homeAnswer(province)
    }

    override suspend fun search(
        provinceId: String,
        query: String,
        latitude: Double?,
        longitude: Double?,
        cursor: String?,
    ): Page<FacilitySummary> {
        calls += "search:$query:$cursor"
        return searchAnswer(query, cursor)
    }

    override suspend fun directory(query: DirectoryQuery, cursor: String?): Page<FacilitySummary> {
        calls += "directory:${query.categoryId}:$cursor"
        return directoryAnswer(query, cursor)
    }

    override suspend fun mapFacilities(
        provinceId: String,
        west: Double,
        south: Double,
        east: Double,
        north: Double,
    ): List<PublicMapFacility> = throw offline

    override suspend fun facility(id: String): FacilityDetail = facilityAnswer(id).also { calls += "facility:$id" }
    override suspend fun profile(): AccountProfile = throw offline
    override suspend fun updateProfile(displayName: String?, provinceId: String?): AccountProfile = throw offline
    override suspend fun requestAccountDeletion() = throw offline
    override suspend fun ratings(): List<UserRating> = ratingsAnswer().also { calls += "ratings" }
    override suspend fun upsertRating(facilityId: String, stars: Int): Int {
        calls += "rate:$facilityId:$stars"
        return upsertAnswer(facilityId, stars)
    }

    override suspend fun deleteRating(facilityId: String) {
        calls += "unrate:$facilityId"
    }
}
