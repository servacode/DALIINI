package com.servacode.directory.feature.home

import com.servacode.directory.core.inject.Inject
import com.servacode.directory.core.model.CategoryTags
import kotlinx.coroutines.flow.Flow

class HomeUseCase @Inject constructor(
    private val repository: HomeRepository,
) {
    operator fun invoke(): Flow<HomeLoad> = repository.load()

    /** Where the user is, for the header. */
    suspend fun place(): HomePlace = repository.place()

    /** Where the user is now, each time that answer changes. */
    fun placeUpdates(): Flow<HomePlace> = repository.placeUpdates()

    /** The list under the chips: one page of the backend's own directory query. */
    suspend fun filtered(
        provinceId: String,
        categoryId: String?,
        filters: HomeFilters,
        cursor: String? = null,
    ) = repository.filtered(provinceId, categoryId, filters, cursor)

    /** The specialties and services a category offers, cached first, for the rows under the chips. */
    fun tags(provinceId: String, categoryId: String): Flow<CategoryTags> = repository.tags(provinceId, categoryId)

    /** Whether a position is known, which decides whether "nearest" is offered. */
    fun hasLocation(): Boolean = repository.hasLocation()

    /** Unread messages, for the bell. Zero when signed out or when the count cannot be had. */
    suspend fun unreadMessages(): Int = repository.unreadMessages()
}
