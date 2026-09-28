package com.servacode.directory.core.network.api

import com.servacode.directory.api.apis.PublicFacilitiesApi
import com.servacode.directory.api.models.FacilityReportRequest
import com.servacode.directory.core.model.FacilityReportReason
import com.servacode.directory.api.apis.AccountApi
import com.servacode.directory.api.apis.ContentApi
import com.servacode.directory.api.apis.AdsApi
import com.servacode.directory.api.apis.PublicDiscoveryApi
import com.servacode.directory.api.apis.PublicTaxonomyApi
import com.servacode.directory.api.apis.RatingsApi
import com.servacode.directory.api.models.ChallengeVerify
import com.servacode.directory.api.models.DeletionRequest
import com.servacode.directory.api.models.FavoriteWrite
import com.servacode.directory.api.models.PasswordChange
import com.servacode.directory.api.models.PatchedProfilePatch
import com.servacode.directory.api.models.PhoneChangeStart
import com.servacode.directory.api.models.RatingWrite
import com.servacode.directory.core.network.OwnerUploadPayload
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import com.servacode.directory.core.model.AccountProfile
import com.servacode.directory.core.model.Category
import com.servacode.directory.core.model.FacilityDetail
import com.servacode.directory.core.model.FacilitySummary
import com.servacode.directory.core.model.HomeAd
import com.servacode.directory.core.model.HomeSnapshot
import com.servacode.directory.core.model.InboxPage
import com.servacode.directory.core.model.LegalPage
import com.servacode.directory.core.model.LegalPageKey
import com.servacode.directory.core.model.Page
import com.servacode.directory.core.model.Province
import com.servacode.directory.core.model.PublicMapFacility
import com.servacode.directory.core.model.ResolvedPlace
import com.servacode.directory.core.model.UserRating
import com.servacode.directory.core.network.DirectoryQuery
import com.servacode.directory.core.network.DirectorySort
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
    private val adverts by lazy { anonymous.create<AdsApi>() }
    private val account by lazy { authorized.create<AccountApi>() }
    private val content by lazy { anonymous.create<ContentApi>() }
    private val ratings by lazy { authorized.create<RatingsApi>() }

    // Through the signed-in client so a report is attributed when there is a session; with none
    // it goes out without a token, which the endpoint accepts.
    private val reports by lazy { authorized.create<PublicFacilitiesApi>() }

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

    override suspend fun ads(provinceId: String): List<HomeAd> =
        call { adverts.publicAdsList(provinceId = provinceId) }.items.map { it.toDomain() }

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
            dutyToday = if (query.dutyToday) "true" else null,
            sort = when (query.sort) {
                DirectorySort.NEAREST -> "nearest"
                DirectorySort.NAME -> "name"
                null -> null
            },
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
        categoryId: String?,
        openNow: Boolean,
        dutyNow: Boolean,
    ): List<PublicMapFacility> = call {
        discovery.publicMapFacilitiesList(
            provinceId = provinceId,
            bbox = "$west,$south,$east,$north",
            categoryId = categoryId,
            // The backend treats only the literal "true" as a filter; absent means no filter.
            openNow = if (openNow) "true" else null,
            dutyNow = if (dutyNow) "true" else null,
        )
    }.items.map { it.toDomain() }

    override suspend fun facility(id: String): FacilityDetail =
        call { discovery.publicFacilityRetrieve(UUID.fromString(id)) }.toDomain()

    override suspend fun reportFacility(facilityId: String, reason: FacilityReportReason, note: String?) {
        call {
            reports.publicFacilityReportCreate(
                facilityId = UUID.fromString(facilityId),
                facilityReportRequest = FacilityReportRequest(
                    reason = reason.toWire(),
                    note = note?.trim()?.takeIf { it.isNotEmpty() },
                ),
            )
        }
    }

    override suspend fun profile(): AccountProfile = call { account.accountProfileRetrieve() }.toDomain()

    override suspend fun updateProfile(
        displayName: String?,
        provinceId: String?,
        address: String?,
    ): AccountProfile = call {
        account.accountProfileUpdate(
            PatchedProfilePatch(
                displayName = displayName,
                provinceId = provinceId?.let(UUID::fromString),
                address = address,
            ),
        )
    }.toDomain()

    override suspend fun updateProfileImage(payload: OwnerUploadPayload): AccountProfile =
        call { account.accountProfileImageUpdate(payload.toImagePart()) }.toDomain()

    override suspend fun removeProfileImage(): AccountProfile =
        call { account.accountProfileImageDelete() }.toDomain()

    override suspend fun startPhoneChange(phone: String): String =
        call { account.accountPhoneChangeStart(PhoneChangeStart(phone = phone)) }.challengeId.toString()

    override suspend fun confirmPhoneChange(challengeId: String, code: String): AccountProfile = call {
        account.accountPhoneChangeConfirm(
            ChallengeVerify(challengeId = UUID.fromString(challengeId), code = code),
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

    override suspend fun resolvePlace(latitude: Double, longitude: Double): ResolvedPlace =
        call { taxonomy.publicLocationResolve(latitude = latitude, longitude = longitude) }.toDomain()

    override suspend fun favorites(cursor: String?): Page<FacilitySummary> =
        call { account.accountFavoritesList(cursor = cursor) }.toDomain()

    override suspend fun addFavorite(facilityId: String): Boolean =
        call { account.accountFavoriteAdd(FavoriteWrite(UUID.fromString(facilityId))) }.isFavorite

    override suspend fun removeFavorite(facilityId: String): Boolean =
        call { account.accountFavoriteRemove(UUID.fromString(facilityId)) }.isFavorite

    override suspend fun inbox(cursor: String?): InboxPage =
        call { account.accountNotificationsList(cursor = cursor) }.toDomain()

    override suspend fun unreadMessageCount(): Int =
        call { account.accountNotificationsUnreadCount() }.unreadCount

    override suspend fun markMessageRead(messageId: String): Int =
        call { account.accountNotificationMarkRead(UUID.fromString(messageId)) }.unreadCount

    override suspend fun markAllMessagesRead() {
        call { account.accountNotificationsMarkAllRead() }
    }

    override suspend fun legalPages(): List<LegalPage> =
        call { content.publicLegalDocumentsList() }.items.map { it.toDomain() }

    override suspend fun legalPage(key: LegalPageKey): LegalPage =
        call { content.publicLegalDocumentRetrieve(key.name) }.toDomain()

    override suspend fun changePassword(currentPassword: String, newPassword: String) {
        callForNoContent {
            account.accountPasswordChange(
                PasswordChange(currentPassword = currentPassword, newPassword = newPassword),
            )
        }
    }
}

/** The picture as a multipart part, named `file` the way the endpoint expects it. */
private fun OwnerUploadPayload.toImagePart(): MultipartBody.Part = MultipartBody.Part.createFormData(
    name = "file",
    filename = fileName,
    body = bytes.toRequestBody(mediaType.toMediaTypeOrNull()),
)
