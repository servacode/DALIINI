package com.servacode.directory.feature.facility

import com.servacode.directory.core.database.Loaded
import com.servacode.directory.core.model.FacilityDetail
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FacilityUseCase @Inject constructor(
    private val repository: FacilityRepository,
) {
    operator fun invoke(id: String): Flow<Loaded<FacilityDetail>> = repository.load(id)
    suspend fun myRating(id: String): Result<Int?> = repository.myRating(id)
    suspend fun rate(id: String, stars: Int): Result<Int> = repository.rate(id, stars)
}
