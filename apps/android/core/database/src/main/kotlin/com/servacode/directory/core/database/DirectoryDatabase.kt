package com.servacode.directory.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProvinceCacheEntity::class,
        CategoryCacheEntity::class,
        HomeSnapshotEntity::class,
        FacilityCacheEntity::class,
        RecentlyViewedEntity::class,
        EmergencyNumberEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class DirectoryDatabase : RoomDatabase() {
    abstract fun cacheDao(): CacheDao
    abstract fun localStoresDao(): LocalStoresDao
}
