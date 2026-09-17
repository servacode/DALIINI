package com.servacode.directory.feature.facility

import javax.inject.Inject

class FacilityUseCase @Inject constructor(
    private val repository: FacilityRepository,
) {
    suspend operator fun invoke(id: String): FacilityLoadResult = repository.load(id)
}
