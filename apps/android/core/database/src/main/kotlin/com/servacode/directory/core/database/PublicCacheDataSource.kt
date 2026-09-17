package com.servacode.directory.core.database

import com.servacode.directory.core.model.FacilityDetail
import com.servacode.directory.core.model.FacilitySummary
import com.servacode.directory.core.model.HomeSnapshot
import com.servacode.directory.core.model.Province
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PublicCacheDataSource @Inject constructor(
    private val dao: CacheDao,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun provinces(): List<Province> = dao.provinces().mapNotNull {
        runCatching { json.decodeFromString<Province>(it.payloadJson) }.getOrNull()
    }

    suspend fun putProvinces(values: List<Province>) {
        val now = System.currentTimeMillis()
        dao.putProvinces(
            values.mapIndexed { index, value ->
                ProvinceCacheEntity(value.id, index, json.encodeToString(value), now)
            },
        )
    }

    suspend fun home(provinceId: String): HomeSnapshot? = dao.home(provinceId)?.let {
        runCatching { json.decodeFromString<HomeSnapshot>(it.payloadJson) }.getOrNull()
    }

    suspend fun putHome(value: HomeSnapshot) {
        dao.putHome(
            HomeSnapshotEntity(
                provinceId = value.province.id,
                payloadJson = json.encodeToString(value),
                updatedAtEpochMillis = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun directory(provinceId: String, categoryId: String): List<FacilitySummary> =
        dao.facilities(provinceId, categoryId).mapNotNull {
            runCatching { json.decodeFromString<FacilitySummary>(it.summaryPayloadJson) }.getOrNull()
        }

    suspend fun facility(id: String): FacilityDetail? = dao.facility(id)?.detailPayloadJson?.let {
        runCatching { json.decodeFromString<FacilityDetail>(it) }.getOrNull()
    }

    suspend fun putFacility(value: FacilityDetail, provinceId: String) {
        val existing = dao.facility(value.summary.id)
        dao.putFacilities(
            listOf(
                FacilityCacheEntity(
                    id = value.summary.id,
                    provinceId = provinceId,
                    categoryId = value.summary.category.id,
                    sortRank = existing?.sortRank ?: Int.MAX_VALUE,
                    detailPayloadJson = json.encodeToString(value),
                    summaryPayloadJson = json.encodeToString(value.summary),
                    updatedAtEpochMillis = System.currentTimeMillis(),
                ),
            ),
        )
    }

    suspend fun putDirectory(values: List<FacilitySummary>, provinceId: String, categoryId: String) {
        val now = System.currentTimeMillis()
        dao.putFacilities(
            values.mapIndexed { index, value ->
                val existing = dao.facility(value.id)
                FacilityCacheEntity(
                    id = value.id,
                    provinceId = provinceId,
                    categoryId = categoryId,
                    sortRank = index,
                    detailPayloadJson = existing?.detailPayloadJson,
                    summaryPayloadJson = json.encodeToString(value),
                    updatedAtEpochMillis = now,
                )
            },
        )
    }
}
