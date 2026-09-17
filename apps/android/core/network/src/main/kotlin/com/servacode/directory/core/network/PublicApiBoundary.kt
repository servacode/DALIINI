package com.servacode.directory.core.network

import com.servacode.directory.core.model.AccountProfile
import com.servacode.directory.core.model.FacilityDetail
import com.servacode.directory.core.model.FacilitySummary
import com.servacode.directory.core.model.HomeSnapshot
import com.servacode.directory.core.model.Province
import com.servacode.directory.core.model.PublicMapFacility
import com.servacode.directory.core.model.UserRating

/**
 * Domain-facing adapter implemented by the generated P10 Kotlin API client.
 * This interface is not a wire contract and contains no hand-authored transport DTOs.
 */
interface PublicApiBoundary {
    suspend fun provinces(): List<Province>
    suspend fun home(provinceId: String, latitude: Double?, longitude: Double?): HomeSnapshot
    suspend fun search(
        provinceId: String,
        query: String,
        latitude: Double?,
        longitude: Double?,
    ): List<FacilitySummary>

    suspend fun directory(
        provinceId: String,
        categoryId: String,
        openNow: Boolean,
        dutyNow: Boolean,
        latitude: Double?,
        longitude: Double?,
    ): List<FacilitySummary>

    suspend fun mapFacilities(
        provinceId: String,
        west: Double,
        south: Double,
        east: Double,
        north: Double,
    ): List<PublicMapFacility>

    suspend fun facility(id: String): FacilityDetail
    suspend fun profile(): AccountProfile
    suspend fun ratings(): List<UserRating>
    suspend fun upsertRating(facilityId: String, stars: Int): UserRating
    suspend fun deleteRating(facilityId: String)
}

class GeneratedClientRequiredException : IllegalStateException(
    "P10 generated Kotlin client adapter is not installed.",
)

object UnboundGeneratedPublicApi : PublicApiBoundary {
    private fun unavailable(): Nothing = throw GeneratedClientRequiredException()
    override suspend fun provinces(): List<Province> = unavailable()
    override suspend fun home(provinceId: String, latitude: Double?, longitude: Double?): HomeSnapshot =
        unavailable()

    override suspend fun search(
        provinceId: String,
        query: String,
        latitude: Double?,
        longitude: Double?,
    ): List<FacilitySummary> = unavailable()

    override suspend fun directory(
        provinceId: String,
        categoryId: String,
        openNow: Boolean,
        dutyNow: Boolean,
        latitude: Double?,
        longitude: Double?,
    ): List<FacilitySummary> = unavailable()

    override suspend fun mapFacilities(
        provinceId: String,
        west: Double,
        south: Double,
        east: Double,
        north: Double,
    ): List<PublicMapFacility> = unavailable()

    override suspend fun facility(id: String): FacilityDetail = unavailable()
    override suspend fun profile(): AccountProfile = unavailable()
    override suspend fun ratings(): List<UserRating> = unavailable()
    override suspend fun upsertRating(facilityId: String, stars: Int): UserRating = unavailable()
    override suspend fun deleteRating(facilityId: String): Unit = unavailable()
}
