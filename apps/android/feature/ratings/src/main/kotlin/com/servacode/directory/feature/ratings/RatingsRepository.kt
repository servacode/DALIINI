package com.servacode.directory.feature.ratings

import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.AppException
import com.servacode.directory.core.model.UserRating
import com.servacode.directory.core.network.PublicApiBoundary
import javax.inject.Inject

/**
 * The signed-in user's own ratings. Never cached: they belong to one account, and the public
 * cache is shared by whoever uses the device next.
 */
class RatingsRepository @Inject constructor(
    private val api: PublicApiBoundary,
) {
    suspend fun list(): Result<List<UserRating>> = runCatching { api.ratings() }

    /** The stars this user gave a facility, or null when they have not rated it. */
    suspend fun mine(facilityId: String): Result<Int?> =
        runCatching { api.ratings().firstOrNull { it.facilityId == facilityId }?.stars }

    /** 1 to 5, checked here and again by the backend, whose refusal is what counts. */
    suspend fun rate(facilityId: String, stars: Int): Result<Int> {
        if (!RatingValidator.isValid(stars)) {
            return Result.failure(
                AppException(
                    AppError(
                        kind = AppError.Kind.VALIDATION,
                        code = "VALIDATION_ERROR",
                        fieldErrors = mapOf("stars" to listOf("1..5")),
                    ),
                ),
            )
        }
        return runCatching { api.upsertRating(facilityId, stars) }
    }

    suspend fun delete(facilityId: String): Result<Unit> = runCatching { api.deleteRating(facilityId) }
}
