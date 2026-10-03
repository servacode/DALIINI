package com.servacode.directory.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CacheDao {
    @Query("SELECT * FROM province_cache ORDER BY sortRank ASC")
    suspend fun provinces(): List<ProvinceCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putProvinces(values: List<ProvinceCacheEntity>)

    @Query("DELETE FROM province_cache")
    suspend fun clearProvinces()

    @Query("SELECT * FROM home_snapshot_cache WHERE provinceId = :provinceId LIMIT 1")
    suspend fun home(provinceId: String): HomeSnapshotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putHome(value: HomeSnapshotEntity)

    @Query(
        "SELECT * FROM facility_cache " +
            "WHERE provinceId = :provinceId AND categoryId = :categoryId " +
            "ORDER BY sortRank ASC"
    )
    suspend fun facilities(provinceId: String, categoryId: String): List<FacilityCacheEntity>

    @Query("SELECT * FROM facility_cache WHERE provinceId = :provinceId ORDER BY sortRank ASC")
    suspend fun allFacilities(provinceId: String): List<FacilityCacheEntity>

    @Query("SELECT * FROM facility_cache WHERE id = :id LIMIT 1")
    suspend fun facility(id: String): FacilityCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putFacilities(values: List<FacilityCacheEntity>)

    @Query("DELETE FROM facility_cache WHERE provinceId = :provinceId")
    suspend fun invalidateFacilities(provinceId: String)

    @Query("DELETE FROM facility_cache WHERE provinceId = :provinceId AND categoryId = :categoryId")
    suspend fun clearDirectory(provinceId: String, categoryId: String)

    @Query("SELECT * FROM category_cache WHERE id = :id LIMIT 1")
    suspend fun category(id: String): CategoryCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putCategory(value: CategoryCacheEntity)
}
