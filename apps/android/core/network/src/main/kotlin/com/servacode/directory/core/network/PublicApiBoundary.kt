package com.servacode.directory.core.network

import com.servacode.directory.core.model.AccountProfile
import com.servacode.directory.core.model.InboxPage
import com.servacode.directory.core.model.LegalPage
import com.servacode.directory.core.model.LegalPageKey
import com.servacode.directory.core.model.ResolvedPlace
import com.servacode.directory.core.model.Category
import com.servacode.directory.core.model.FacilityDetail
import com.servacode.directory.core.model.FacilitySummary
import com.servacode.directory.core.model.HomeSnapshot
import com.servacode.directory.core.model.Page
import com.servacode.directory.core.model.Province
import com.servacode.directory.core.model.PublicMapFacility

/** What a directory listing asks the backend for. Filtering and ordering happen there. */
data class DirectoryQuery(
    val provinceId: String,
    /** Null lists the whole province, which is what Home's quick filters ask for. */
    val categoryId: String? = null,
    val openNow: Boolean = false,
    val dutyNow: Boolean = false,
    val search: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    /** Rows per page; the backend's default when null, and it caps what it accepts. */
    val pageSize: Int? = null,
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
    suspend fun home(province: Province, latitude: Double?, longitude: Double?): HomeSnapshot
    suspend fun search(
        provinceId: String,
        query: String,
        latitude: Double?,
        longitude: Double?,
        cursor: String? = null,
    ): Page<FacilitySummary>

    suspend fun directory(query: DirectoryQuery, cursor: String? = null): Page<FacilitySummary>

    suspend fun mapFacilities(
        provinceId: String,
        west: Double,
        south: Double,
        east: Double,
        north: Double,
    ): List<PublicMapFacility>

    suspend fun facility(id: String): FacilityDetail
    suspend fun profile(): AccountProfile
    suspend fun updateProfile(displayName: String? = null, provinceId: String? = null): AccountProfile
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
