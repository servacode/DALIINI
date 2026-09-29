package com.servacode.directory.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "province_cache")
data class ProvinceCacheEntity(
    @PrimaryKey val id: String,
    val sortRank: Int,
    val payloadJson: String,
    val updatedAtEpochMillis: Long,
)

/**
 * One row per category: what is kept about it beyond the home snapshot, which lists it. That is
 * the specialties and services it offers, a `CategoryTags` as JSON; [provinceId] is the province
 * it was listed in when stored.
 */
@Entity(tableName = "category_cache")
data class CategoryCacheEntity(
    @PrimaryKey val id: String,
    val provinceId: String,
    val payloadJson: String,
    val updatedAtEpochMillis: Long,
)

@Entity(tableName = "home_snapshot_cache")
data class HomeSnapshotEntity(
    @PrimaryKey val provinceId: String,
    val payloadJson: String,
    val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "facility_cache",
    indices = [Index(value = ["provinceId", "categoryId"])],
)
data class FacilityCacheEntity(
    @PrimaryKey val id: String,
    val provinceId: String,
    val categoryId: String,
    val sortRank: Int,
    val detailPayloadJson: String?,
    val summaryPayloadJson: String,
    val updatedAtEpochMillis: Long,
)
