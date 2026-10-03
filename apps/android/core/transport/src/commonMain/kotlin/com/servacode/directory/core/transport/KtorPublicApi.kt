package com.servacode.directory.core.transport

import com.servacode.directory.api.multiplatform.apis.AccountApi
import com.servacode.directory.api.multiplatform.apis.AdsApi
import com.servacode.directory.api.multiplatform.apis.ContentApi
import com.servacode.directory.api.multiplatform.apis.PublicDiscoveryApi
import com.servacode.directory.api.multiplatform.apis.PublicFacilitiesApi
import com.servacode.directory.api.multiplatform.apis.PublicTaxonomyApi
import com.servacode.directory.api.multiplatform.apis.RatingsApi
import com.servacode.directory.api.multiplatform.models.ChallengeVerify
import com.servacode.directory.api.multiplatform.models.DeletionRequest
import com.servacode.directory.api.multiplatform.models.FacilityReportRequest
import com.servacode.directory.api.multiplatform.models.FavoriteWrite
import com.servacode.directory.api.multiplatform.models.PasswordChange
import com.servacode.directory.api.multiplatform.models.PatchedNotificationPreferences
import com.servacode.directory.api.multiplatform.models.PatchedProfilePatch
import com.servacode.directory.api.multiplatform.models.PhoneChangeStart
import com.servacode.directory.api.multiplatform.models.RatingWrite
import com.servacode.directory.core.model.AccountProfile
import com.servacode.directory.core.model.AppRelease
import com.servacode.directory.core.model.Category
import com.servacode.directory.core.model.CategoryTags
import com.servacode.directory.core.model.DutyDay
import com.servacode.directory.core.model.EmergencyNumber
import com.servacode.directory.core.model.FacilityDetail
import com.servacode.directory.core.model.FacilityReportReason
import com.servacode.directory.core.model.FacilitySummary
import com.servacode.directory.core.model.HomeAd
import com.servacode.directory.core.model.HomeSnapshot
import com.servacode.directory.core.model.InboxPage
import com.servacode.directory.core.model.LegalPage
import com.servacode.directory.core.model.LegalPageKey
import com.servacode.directory.core.model.NotificationSwitches
import com.servacode.directory.core.model.Page
import com.servacode.directory.core.model.Province
import com.servacode.directory.core.model.PublicMapFacility
import com.servacode.directory.core.model.ResolvedPlace
import com.servacode.directory.core.model.UserRating
import com.servacode.directory.core.network.DirectoryQuery
import com.servacode.directory.core.network.DirectorySort
import com.servacode.directory.core.network.OwnerUploadPayload
import com.servacode.directory.core.network.PublicApiBoundary

/**
 * [PublicApiBoundary] over the multiplatform client: the port of Android's `GeneratedPublicApi`,
 * request for request.
 *
 * Discovery goes out without the user's token: public data does not depend on who asks, and
 * a stale token would otherwise turn a public list into a 401. Account and rating calls use
 * the signed-in client.
 *
 * Each generated API is built on first use, inside `call {}`, so a build without a configured
 * address fails each request with a typed error instead of failing when this is constructed.
 */
class KtorPublicApi(private val clients: TransportClients) : PublicApiBoundary {
    private val discovery by lazy { PublicDiscoveryApi(clients.baseUrl, clients.anonymous) }
    private val taxonomy by lazy { PublicTaxonomyApi(clients.baseUrl, clients.anonymous) }
    private val adverts by lazy { AdsApi(clients.baseUrl, clients.anonymous) }
    private val account by lazy { AccountApi(clients.baseUrl, clients.authorized) }
    private val content by lazy { ContentApi(clients.baseUrl, clients.anonymous) }
    private val ratings by lazy { RatingsApi(clients.baseUrl, clients.authorized) }

    // Through the signed-in client so a report is attributed when there is a session; with none
    // it goes out without a token, which the endpoint accepts.
    private val reports by lazy { PublicFacilitiesApi(clients.baseUrl, clients.authorized) }

    override suspend fun provinces(): List<Province> =
        call { taxonomy.publicProvincesList() }.items.map { it.toDomain() }

    override suspend fun categories(provinceId: String): List<Category> =
        call { taxonomy.publicProvinceCategoriesList(uuid(provinceId)) }
            .items.map { it.toDomain() }

    override suspend fun categoryTags(categoryId: String): CategoryTags =
        call { taxonomy.publicCategoryTagsRetrieve(uuid(categoryId)) }.toDomain()

    override suspend fun home(province: Province, latitude: Double?, longitude: Double?): HomeSnapshot =
        call {
            discovery.publicHomeRetrieve(
                provinceId = province.id,
                latitude = latitude.asCoordinate(),
                longitude = longitude.asCoordinate(),
            )
        }.toDomain(province)

    override suspend fun appRelease(): AppRelease {
        val platform = ContentApi.PlatformPublicAppReleaseRetrieve.valueOf(clients.platform.name)
        return call { content.publicAppReleaseRetrieve(platform) }.toDomain()
    }

    override suspend fun emergencyNumbers(provinceId: String?): List<EmergencyNumber> =
        call { content.publicEmergencyNumbersList(provinceId = provinceId) }.items
            .sortedBy { it.sortOrder }
            .map { it.toDomain() }

    override suspend fun dutyRoster(provinceId: String, date: String?, days: Int): List<DutyDay> =
        call { discovery.publicDutyByDateList(provinceId = provinceId, date = date, days = days.coerceIn(1, 7)) }
            .days.map { it.toDomain() }

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
            latitude = latitude.asCoordinate(),
            longitude = longitude.asCoordinate(),
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
            latitude = query.latitude.asCoordinate(),
            longitude = query.longitude.asCoordinate(),
            limit = query.pageSize,
            // Integer keys on the wire; the domain keeps every id as a String.
            specialtyId = query.specialtyId?.toInt(),
            serviceTagId = query.serviceTagId?.toInt(),
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
        call { discovery.publicFacilityRetrieve(uuid(id)) }.toDomain()

    override suspend fun reportFacility(facilityId: String, reason: FacilityReportReason, note: String?) {
        call {
            reports.publicFacilityReportCreate(
                facilityId = uuid(facilityId),
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
                provinceId = provinceId?.let(::uuid),
                address = address,
            ),
        )
    }.toDomain()

    override suspend fun updateProfileImage(payload: OwnerUploadPayload): AccountProfile =
        call { account.accountProfileImageUpdate(payload.toFilePart()) }.toDomain()

    override suspend fun removeProfileImage(): AccountProfile =
        call { account.accountProfileImageDelete() }.toDomain()

    override suspend fun startPhoneChange(phone: String): String =
        call { account.accountPhoneChangeStart(PhoneChangeStart(phone = phone)) }.challengeId

    override suspend fun confirmPhoneChange(challengeId: String, code: String): AccountProfile = call {
        account.accountPhoneChangeConfirm(
            ChallengeVerify(challengeId = uuid(challengeId), code = code),
        )
    }.toDomain()

    override suspend fun requestAccountDeletion() {
        call { account.accountDeletionRequestCreate(DeletionRequest(confirm = true)) }
    }

    override suspend fun ratings(): List<UserRating> =
        call { account.accountRatingsList() }.items.map { it.toDomain() }

    override suspend fun upsertRating(facilityId: String, stars: Int): Int =
        call { ratings.facilityRatingUpsert(uuid(facilityId), RatingWrite(stars)) }.stars

    override suspend fun deleteRating(facilityId: String) {
        callForNoContent { ratings.facilityRatingDelete(uuid(facilityId)) }
    }

    override suspend fun resolvePlace(latitude: Double, longitude: Double): ResolvedPlace =
        call { taxonomy.publicLocationResolve(latitude = latitude, longitude = longitude) }.toDomain()

    override suspend fun favorites(cursor: String?): Page<FacilitySummary> =
        call { account.accountFavoritesList(cursor = cursor) }.toDomain()

    override suspend fun addFavorite(facilityId: String): Boolean =
        call { account.accountFavoriteAdd(FavoriteWrite(uuid(facilityId))) }.isFavorite

    override suspend fun removeFavorite(facilityId: String): Boolean =
        call { account.accountFavoriteRemove(uuid(facilityId)) }.isFavorite

    override suspend fun inbox(cursor: String?): InboxPage =
        call { account.accountNotificationsList(cursor = cursor) }.toDomain()

    override suspend fun unreadMessageCount(): Int =
        call { account.accountNotificationsUnreadCount() }.unreadCount

    override suspend fun markMessageRead(messageId: String): Int =
        call { account.accountNotificationMarkRead(uuid(messageId)) }.unreadCount

    override suspend fun markAllMessagesRead() {
        call { account.accountNotificationsMarkAllRead() }
    }

    override suspend fun notificationSwitches(): NotificationSwitches =
        call { account.accountNotificationPreferencesRetrieve() }.toDomain()

    override suspend fun updateNotificationSwitches(
        dutyReminders: Boolean?,
        provinceNews: Boolean?,
        applicationStatus: Boolean?,
    ): NotificationSwitches = call {
        account.accountNotificationPreferencesUpdate(
            PatchedNotificationPreferences(
                dutyReminders = dutyReminders,
                provinceNews = provinceNews,
                applicationStatus = applicationStatus,
            ),
        )
    }.toDomain()

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
