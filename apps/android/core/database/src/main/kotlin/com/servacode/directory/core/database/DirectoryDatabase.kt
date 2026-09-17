package com.servacode.directory.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProvinceCacheEntity::class,
        CategoryCacheEntity::class,
        HomeSnapshotEntity::class,
        FacilityCacheEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class DirectoryDatabase : RoomDatabase() {
    abstract fun cacheDao(): CacheDao
}
