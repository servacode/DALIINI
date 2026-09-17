package com.servacode.directory.feature.ratings

import com.servacode.directory.core.model.UserRating
import com.servacode.directory.core.network.PublicApiBoundary
import javax.inject.Inject

class RatingsRepository @Inject constructor(
    private val api: PublicApiBoundary,
) {
    suspend fun list(): Result<List<UserRating>> = runCatching { api.ratings() }
    suspend fun update(facilityId: String, stars: Int): Result<UserRating> =
        runCatching { api.upsertRating(facilityId, RatingValidator.requireValid(stars)) }
    suspend fun delete(facilityId: String): Result<Unit> = runCatching { api.deleteRating(facilityId) }
}
