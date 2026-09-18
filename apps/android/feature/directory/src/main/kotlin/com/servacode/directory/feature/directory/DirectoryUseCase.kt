package com.servacode.directory.feature.directory

import com.servacode.directory.core.model.FacilitySummary
import com.servacode.directory.core.model.Page
import com.servacode.directory.core.network.DirectoryQuery
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DirectoryUseCase @Inject constructor(
    private val repository: DirectoryRepository,
) {
    fun firstPage(categoryId: String, filter: DirectoryFilter): Flow<DirectoryLoad> =
        repository.firstPage(categoryId, filter)

    suspend fun nextPage(query: DirectoryQuery, cursor: String, loadedCount: Int): Page<FacilitySummary> =
        repository.nextPage(query, cursor, loadedCount)
}
