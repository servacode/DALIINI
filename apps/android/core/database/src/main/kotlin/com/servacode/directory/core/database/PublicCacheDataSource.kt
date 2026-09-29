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
) : PublicCache {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun provinces(): List<Province> = dao.provinces().mapNotNull {
        runCatching { json.decodeFromString<Province>(it.payloadJson) }.getOrNull()
    }

    override suspend fun putProvinces(values: List<Province>) {
        val now = System.currentTimeMillis()
        // Replace, not merge: a province the backend stopped serving must not linger offline.
        dao.clearProvinces()
        dao.putProvinces(
            values.mapIndexed { index, value ->
                ProvinceCacheEntity(value.id, index, json.encodeToString(value), now)
            },
        )
    }

    override suspend fun home(provinceId: String): HomeSnapshot? = dao.home(provinceId)?.let {
        runCatching { json.decodeFromString<HomeSnapshot>(it.payloadJson) }.getOrNull()
    }

    override suspend fun putHome(value: HomeSnapshot) {
        dao.putHome(
            HomeSnapshotEntity(
                provinceId = value.province.id,
                payloadJson = json.encodeToString(value),
                updatedAtEpochMillis = System.currentTimeMillis(),
            ),
        )
        // The widget redraws from this snapshot; it never asks the backend on its own for it.
        CacheEvents.homeWritten(value.province.id)
    }

    override suspend fun directory(provinceId: String, categoryId: String): List<FacilitySummary> =
        dao.facilities(provinceId, categoryId).mapNotNull {
            runCatching { json.decodeFromString<FacilitySummary>(it.summaryPayloadJson) }.getOrNull()
        }

    override suspend fun facility(id: String): FacilityDetail? = dao.facility(id)?.detailPayloadJson?.let {
        runCatching { json.decodeFromString<FacilityDetail>(it) }.getOrNull()
    }

    override suspend fun putFacility(value: FacilityDetail, provinceId: String) {
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

    override suspend fun putDirectoryPage(
        values: List<FacilitySummary>,
        provinceId: String,
        categoryId: String,
        offset: Int,
    ) {
        // Details were fetched separately and stay valid; carry them across the rewrite.
        val details = dao.facilities(provinceId, categoryId).associate { it.id to it.detailPayloadJson }
        if (offset == 0) dao.clearDirectory(provinceId, categoryId)
        val now = System.currentTimeMillis()
        dao.putFacilities(
            values.mapIndexed { index, value ->
                FacilityCacheEntity(
                    id = value.id,
                    provinceId = provinceId,
                    categoryId = categoryId,
                    sortRank = offset + index,
                    detailPayloadJson = details[value.id] ?: dao.facility(value.id)?.detailPayloadJson,
                    summaryPayloadJson = json.encodeToString(value),
                    updatedAtEpochMillis = now,
                )
            },
        )
    }
}
