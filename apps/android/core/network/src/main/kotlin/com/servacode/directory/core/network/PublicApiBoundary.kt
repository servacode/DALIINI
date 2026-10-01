package com.servacode.directory.core.network

import com.servacode.directory.core.model.EmergencyNumber
import com.servacode.directory.core.model.DutyDay
import com.servacode.directory.core.model.FacilityReportReason
import com.servacode.directory.core.model.AccountProfile
import com.servacode.directory.core.model.InboxPage
import com.servacode.directory.core.model.LegalPage
import com.servacode.directory.core.model.LegalPageKey
import com.servacode.directory.core.model.ResolvedPlace
import com.servacode.directory.core.model.Category
import com.servacode.directory.core.model.CategoryTags
import com.servacode.directory.core.model.FacilityDetail
import com.servacode.directory.core.model.FacilitySummary
import com.servacode.directory.core.model.HomeAd
import com.servacode.directory.core.model.HomeSnapshot
import com.servacode.directory.core.model.Page
import com.servacode.directory.core.model.Province
import com.servacode.directory.core.model.PublicMapFacility

/**
 * What a list is ordered by.
 *
 * Distance is measured whenever coordinates are supplied, whichever of these is chosen, so
 * asking for the province by name never costs the reader the distances.
 */
enum class DirectorySort { NEAREST, NAME }

/** What a directory listing asks the backend for. Filtering and ordering happen there. */
data class DirectoryQuery(
    val provinceId: String,
    /** Null lists the whole province, which is what Home's quick filters ask for. */
    val categoryId: String? = null,
    /** Open at this moment. Combines with the others; it never implies anything about duty. */
    val openNow: Boolean = false,
    /** A duty shift running right now. */
    val dutyNow: Boolean = false,
    /**
     * On today's duty roster, which is a different question from [dutyNow]: a pharmacy on
     * tonight's roster qualifies all day, including while it is shut.
     */
    val dutyToday: Boolean = false,
    /**
     * What to order by. Null leaves the choice to the backend, which is nearest whenever
     * coordinates are supplied — the behaviour every caller had before this existed, and the
     * reason only a caller with a reason to differ should set it.
     */
    val sort: DirectorySort? = null,
    val search: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    /** Rows per page; the backend's default when null, and it caps what it accepts. */
    val pageSize: Int? = null,
    /**
     * One specialty, by the id of one of the category's [CategoryTags] choices; null narrows by
     * none. Only facilities of a category that declares `specialtyFilter` can match.
     */
    val specialtyId: String? = null,
    /** One service, the same way; only a category that declares `serviceFilter` can match. */
    val serviceTagId: String? = null,
)

/**
 * Domain-facing adapter over the generated P10 Kotlin client.
 *
 * This interface is not a wire contract and carries no transport DTOs; the implementation in
 * `api/` is the only code that sees generated types. Failures surface as
 * [com.servacode.directory.core.model.AppException].
 */
interface PublicApiBoundary {
    suspend fun provinces(): List<Province>
    suspend fun categories(provinceId: String): List<Category>

    /**
     * The specialties and services [categoryId] offers, in the operators' order: the choices
     * behind its filters. Whether a filter is offered at all is still the category's
     * capabilities' decision. NOT_FOUND unless the category is public somewhere.
     */
    suspend fun categoryTags(categoryId: String): CategoryTags
    suspend fun home(province: Province, latitude: Double?, longitude: Double?): HomeSnapshot

    /** The country's emergency numbers and, for [provinceId], the province's, in the backend's order. */
    suspend fun emergencyNumbers(provinceId: String?): List<EmergencyNumber>

    /** Who is on duty in the province, [days] days from [date] ("YYYY-MM-DD"; today when null). */
    suspend fun dutyRoster(provinceId: String, date: String? = null, days: Int = 1): List<DutyDay>

    /** The province's live home advertisements, in the backend's sort order. */
    suspend fun ads(provinceId: String): List<HomeAd>
    suspend fun search(
        provinceId: String,
        query: String,
        latitude: Double?,
        longitude: Double?,
        cursor: String? = null,
    ): Page<FacilitySummary>

    suspend fun directory(query: DirectoryQuery, cursor: String? = null): Page<FacilitySummary>

    /**
     * Markers inside a viewport, narrowed the same way a list is: by category and by the
     * availability the backend computes. The map never decides any of that for itself.
     */
    suspend fun mapFacilities(
        provinceId: String,
        west: Double,
        south: Double,
        east: Double,
        north: Double,
        categoryId: String? = null,
        openNow: Boolean = false,
        dutyNow: Boolean = false,
    ): List<PublicMapFacility>

    suspend fun facility(id: String): FacilityDetail

    /**
     * Reports a problem with a facility's public details. Works signed out; when signed in the
     * backend records who reported. [note] is optional and at most 500 characters.
     */
    suspend fun reportFacility(facilityId: String, reason: FacilityReportReason, note: String?)
    suspend fun profile(): AccountProfile
    suspend fun updateProfile(
        displayName: String? = null,
        provinceId: String? = null,
        address: String? = null,
    ): AccountProfile

    /** Replaces the picture on the account, or removes it. Returns the profile as it now is. */
    suspend fun updateProfileImage(payload: OwnerUploadPayload): AccountProfile
    suspend fun removeProfileImage(): AccountProfile

    /**
     * Moves the account to another number.
     *
     * The code goes to the number being claimed, and confirming it ends every session — the
     * phone is how this account signs in, so the app signs in again afterwards.
     */
    suspend fun startPhoneChange(phone: String): String
    suspend fun confirmPhoneChange(challengeId: String, code: String): AccountProfile
    suspend fun requestAccountDeletion()
    suspend fun ratings(): List<com.servacode.directory.core.model.UserRating>

    /** Returns the stars the backend stored. */
    suspend fun upsertRating(facilityId: String, stars: Int): Int
    suspend fun deleteRating(facilityId: String)

    /** Where a coordinate is, in the platform's own taxonomy. Never stores the coordinate. */
    suspend fun resolvePlace(latitude: Double, longitude: Double): ResolvedPlace

    suspend fun favorites(cursor: String? = null): Page<FacilitySummary>

    /** Idempotent in both directions; returns the state the backend now holds. */
    suspend fun addFavorite(facilityId: String): Boolean
    suspend fun removeFavorite(facilityId: String): Boolean

    suspend fun inbox(cursor: String? = null): InboxPage
    suspend fun unreadMessageCount(): Int
    suspend fun markMessageRead(messageId: String): Int
    suspend fun markAllMessagesRead()

    /** The published pages, titles and versions only. */
    suspend fun legalPages(): List<LegalPage>
    suspend fun legalPage(key: LegalPageKey): LegalPage

    /** Replaces the password and ends every session, this one included. */
    suspend fun changePassword(currentPassword: String, newPassword: String)
}
