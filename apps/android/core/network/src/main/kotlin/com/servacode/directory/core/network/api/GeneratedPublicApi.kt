package com.servacode.directory.core.network.api

import com.servacode.directory.api.apis.AccountApi
import com.servacode.directory.api.apis.PublicDiscoveryApi
import com.servacode.directory.api.apis.PublicTaxonomyApi
import com.servacode.directory.api.apis.RatingsApi
import com.servacode.directory.api.models.DeletionRequest
import com.servacode.directory.api.models.PatchedProfilePatch
import com.servacode.directory.api.models.RatingWrite
import com.servacode.directory.core.model.AccountProfile
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
import java.util.UUID

/**
 * [PublicApiBoundary] over the generated client.
 *
 * Discovery goes out without the user's token: public data does not depend on who asks, and
 * a stale token would otherwise turn a public list into a 401. Account and rating calls use
 * the signed-in client.
 */
class GeneratedPublicApi(anonymous: GeneratedClient, authorized: GeneratedClient) : PublicApiBoundary {
    private val discovery by lazy { anonymous.create<PublicDiscoveryApi>() }
    private val taxonomy by lazy { anonymous.create<PublicTaxonomyApi>() }
    private val account by lazy { authorized.create<AccountApi>() }
    private val ratings by lazy { authorized.create<RatingsApi>() }

    override suspend fun provinces(): List<Province> =
        call { taxonomy.publicProvincesList() }.items.map { it.toDomain() }

    override suspend fun categories(provinceId: String): List<Category> =
        call { taxonomy.publicProvinceCategoriesList(UUID.fromString(provinceId)) }
            .items.map { it.toDomain() }

    override suspend fun home(province: Province, latitude: Double?, longitude: Double?): HomeSnapshot =
        call {
            discovery.publicHomeRetrieve(
                provinceId = province.id,
                latitude = latitude?.toString(),
                longitude = longitude?.toString(),
            )
        }.toDomain(province)

    override suspend fun search(
        provinceId: String,
        query: String,
        latitude: Double?,
        longitude: Double?,
        cursor: String?,
    ): Page<FacilitySummary> = call {
        discovery.publicSearchList(
            provinceId = provinceId,
            q = query,
            cursor = cursor,
            latitude = latitude?.toString(),
            longitude = longitude?.toString(),
        )
    }.toDomain()

    override suspend fun directory(query: DirectoryQuery, cursor: String?): Page<FacilitySummary> = call {
        discovery.publicFacilitiesList(
            categoryId = query.categoryId,
            provinceId = query.provinceId,
            cursor = cursor,
            // The backend treats only the literal "true" as a filter; absent means no filter.
            openNow = if (query.openNow) "true" else null,
            dutyNow = if (query.dutyNow) "true" else null,
            search = query.search?.takeIf { it.isNotBlank() },
            latitude = query.latitude?.toString(),
            longitude = query.longitude?.toString(),
            limit = query.pageSize,
        )
    }.toDomain()

    override suspend fun mapFacilities(
        provinceId: String,
        west: Double,
        south: Double,
        east: Double,
        north: Double,
    ): List<PublicMapFacility> = call {
        discovery.publicMapFacilitiesList(provinceId = provinceId, bbox = "$west,$south,$east,$north")
    }.items.map { it.toDomain() }

    override suspend fun facility(id: String): FacilityDetail =
        call { discovery.publicFacilityRetrieve(UUID.fromString(id)) }.toDomain()

    override suspend fun profile(): AccountProfile = call { account.accountProfileRetrieve() }.toDomain()

    override suspend fun updateProfile(displayName: String?, provinceId: String?): AccountProfile = call {
        account.accountProfileUpdate(
            PatchedProfilePatch(displayName = displayName, provinceId = provinceId?.let(UUID::fromString)),
        )
    }.toDomain()

    override suspend fun requestAccountDeletion() {
        call { account.accountDeletionRequestCreate(DeletionRequest(confirm = true)) }
    }

    override suspend fun ratings(): List<UserRating> =
        call { account.accountRatingsList() }.items.map { it.toDomain() }

    override suspend fun upsertRating(facilityId: String, stars: Int): Int =
        call { ratings.facilityRatingUpsert(UUID.fromString(facilityId), RatingWrite(stars)) }.stars

    override suspend fun deleteRating(facilityId: String) {
        callForNoContent { ratings.facilityRatingDelete(UUID.fromString(facilityId)) }
    }
}
