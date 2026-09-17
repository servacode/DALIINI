package com.servacode.directory.feature.search

import com.servacode.directory.core.model.FacilitySummary
import javax.inject.Inject

class SearchUseCase @Inject constructor(
    private val repository: SearchRepository,
) {
    suspend operator fun invoke(query: String): Result<List<FacilitySummary>> = repository.search(query)
}
