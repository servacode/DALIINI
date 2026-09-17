package com.servacode.directory.feature.ratings

import com.servacode.directory.core.model.UserRating
import javax.inject.Inject

class RatingsUseCase @Inject constructor(
    private val repository: RatingsRepository,
) {
    suspend fun list(): Result<List<UserRating>> = repository.list()
    suspend fun update(facilityId: String, stars: Int): Result<UserRating> = repository.update(facilityId, stars)
    suspend fun delete(facilityId: String): Result<Unit> = repository.delete(facilityId)
}
