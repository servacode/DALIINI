package com.servacode.directory.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CacheDao {
    @Query("SELECT * FROM home_snapshot_cache WHERE provinceId = :provinceId LIMIT 1")
    suspend fun home(provinceId: String): HomeSnapshotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putHome(value: HomeSnapshotEntity)

    @Query("SELECT * FROM facility_cache WHERE id = :id LIMIT 1")
    suspend fun facility(id: String): FacilityCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putFacilities(values: List<FacilityCacheEntity>)

    @Query("DELETE FROM facility_cache WHERE provinceId = :provinceId")
    suspend fun invalidateFacilities(provinceId: String)
}
