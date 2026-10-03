package com.servacode.directory.core.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

/**
 * The device's database: the public cache and what the reader keeps on the device. Shared with
 * the iPhone app (DECISION-092); each platform only opens it, Android with its framework SQLite
 * (DatabaseModule) and the iPhone with the system's (IosDirectoryDatabase).
 */
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
@ConstructedBy(DirectoryDatabaseConstructor::class)
abstract class DirectoryDatabase : RoomDatabase() {
    abstract fun cacheDao(): CacheDao
    abstract fun localStoresDao(): LocalStoresDao

    companion object {
        /** The file, the same name on both platforms. */
        const val FILE_NAME = "directory-cache.db"
    }
}

/** Room writes this for each platform; nothing calls it but Room. */
@Suppress("KotlinNoActualForExpect")
expect object DirectoryDatabaseConstructor : RoomDatabaseConstructor<DirectoryDatabase> {
    override fun initialize(): DirectoryDatabase
}
