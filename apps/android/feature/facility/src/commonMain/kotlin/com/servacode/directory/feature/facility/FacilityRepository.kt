package com.servacode.directory.feature.facility

import com.servacode.directory.core.database.Loaded
import com.servacode.directory.core.database.PublicCache
import com.servacode.directory.core.database.cacheFirst
import com.servacode.directory.core.datastore.DirectoryPreferencesStore
import com.servacode.directory.core.inject.Inject
import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.AppException
import com.servacode.directory.core.model.FacilityDetail
import com.servacode.directory.core.model.FacilityReportReason
import com.servacode.directory.core.network.PublicApiBoundary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * One facility, cache first. Opening hours are shown as served; whether the facility is open
 * or on duty now is the backend's answer, never recomputed from the hours on the device.
 */
class FacilityRepository @Inject constructor(
    private val cache: PublicCache,
    private val api: PublicApiBoundary,
    private val preferences: DirectoryPreferencesStore,
) {
    fun load(id: String): Flow<Loaded<FacilityDetail>> = cacheFirst(
        read = { cache.facility(id) },
        fetch = { api.facility(id) },
        write = { detail ->
            preferences.values.first().selectedProvinceId?.let { cache.putFacility(detail, it) }
        },
    )

    /** The signed-in user's stars for this facility, read from their account and not cached. */
    suspend fun myRating(id: String): Result<Int?> =
        runCatching { api.ratings().firstOrNull { it.facilityId == id }?.stars }

    /** 1 to 5. The backend validates again and its refusal is what the screen shows. */
    suspend fun rate(id: String, stars: Int): Result<Int> {
        if (stars !in 1..5) {
            return Result.failure(AppException(AppError(AppError.Kind.VALIDATION, code = "VALIDATION_ERROR")))
        }
        return runCatching { api.upsertRating(id, stars) }
    }

    /** Take the rating back. The stars are the person's word, and a word can be withdrawn. */
    suspend fun removeRating(id: String): Result<Unit> = runCatching { api.deleteRating(id) }

    /** Save this facility to the account. Idempotent, as the backend's own call is. */
    suspend fun save(id: String): Result<Boolean> = runCatching { api.addFavorite(id) }

    suspend fun unsave(id: String): Result<Boolean> = runCatching { api.removeFavorite(id) }

    /**
     * Report a problem with the facility's public details. The note is trimmed and capped here
     * as well as on the form, so a long paste cannot turn into a refused request.
     */
    suspend fun report(id: String, reason: FacilityReportReason, note: String?): Result<Unit> =
        runCatching {
            api.reportFacility(id, reason, note?.trim()?.take(REPORT_NOTE_MAX)?.takeIf { it.isNotEmpty() })
        }

    companion object {
        /** The backend's own limit on a report's note. */
        const val REPORT_NOTE_MAX = 500
    }
}
