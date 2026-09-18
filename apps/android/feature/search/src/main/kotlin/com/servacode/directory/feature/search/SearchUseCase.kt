package com.servacode.directory.feature.search

import com.servacode.directory.core.model.FacilitySummary
import com.servacode.directory.core.model.Page
import javax.inject.Inject

class SearchUseCase @Inject constructor(
    private val repository: SearchRepository,
) {
    suspend fun first(query: String): Pair<SearchContext, Page<FacilitySummary>>? = repository.first(query)
    suspend fun next(context: SearchContext, cursor: String): Page<FacilitySummary> =
        repository.next(context, cursor)
}
