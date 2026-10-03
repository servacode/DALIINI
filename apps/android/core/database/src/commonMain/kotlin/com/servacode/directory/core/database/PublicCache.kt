package com.servacode.directory.core.database

import com.servacode.directory.core.model.CategoryTags
import com.servacode.directory.core.model.FacilityDetail
import com.servacode.directory.core.model.FacilitySummary
import com.servacode.directory.core.model.HomeSnapshot
import com.servacode.directory.core.model.Province

/**
 * The public, reconstructible cache behind the cache-first screens.
 *
 * Only public data lives here: provinces, the home snapshot, facility rows and details, and the
 * specialties and services a category offers. No token, code, password, evidence, rating of the
 * user's own or anything owner-private is ever written to it.
 */
interface PublicCache {
    suspend fun provinces(): List<Province>
    suspend fun putProvinces(values: List<Province>)
    suspend fun home(provinceId: String): HomeSnapshot?
    suspend fun putHome(value: HomeSnapshot)

    /** The cached rows for a list, in the order the backend served them. */
    suspend fun directory(provinceId: String, categoryId: String): List<FacilitySummary>

    /**
     * Stores one page of a list. `offset` is how many rows came before it: 0 replaces the list,
     * anything else appends after the rows already cached, so a replay offline shows the same
     * order the backend returned across pages.
     */
    suspend fun putDirectoryPage(
        values: List<FacilitySummary>,
        provinceId: String,
        categoryId: String,
        offset: Int,
    )

    suspend fun facility(id: String): FacilityDetail?
    suspend fun putFacility(value: FacilityDetail, provinceId: String)

    /** The specialties and services [categoryId] offered when last asked; null when never kept. */
    suspend fun categoryTags(categoryId: String): CategoryTags?

    /** Replaces what is kept for [categoryId]. [provinceId] is the province it was listed in. */
    suspend fun putCategoryTags(value: CategoryTags, categoryId: String, provinceId: String)
}
