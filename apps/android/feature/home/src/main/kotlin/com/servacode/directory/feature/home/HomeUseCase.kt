package com.servacode.directory.feature.home

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class HomeUseCase @Inject constructor(
    private val repository: HomeRepository,
) {
    operator fun invoke(): Flow<HomeLoad> = repository.load()

    /** Where the user is, for the header. */
    suspend fun place(): HomePlace = repository.place()

    /** One quick filter's page, from the backend's own directory query. */
    suspend fun filtered(filter: HomeQuickFilter, cursor: String? = null) =
        repository.filtered(filter, cursor)

    /** Whether a position is known, which decides whether "nearest" is offered. */
    fun hasLocation(): Boolean = repository.hasLocation()
}
