package com.servacode.directory.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Version 1 to 2: two tables for what the reader keeps on the device, added without touching
 * the cache already there. The SQL is Room's own for these entities (schemas/…/2.json).
 *
 * Android's alone: only Android ever had a version 1, and it opens the database through its
 * framework's SQLite, which is what this signature is for. The iPhone starts at version 2.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `recently_viewed` (`facilityId` TEXT NOT NULL, `nameAr` TEXT NOT NULL, " +
                "`categoryNameAr` TEXT, `viewedAtEpochMillis` INTEGER NOT NULL, PRIMARY KEY(`facilityId`))",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `emergency_number_cache` (`cacheKey` TEXT NOT NULL, " +
                "`position` INTEGER NOT NULL, `nameAr` TEXT NOT NULL, `number` TEXT NOT NULL, " +
                "`scope` TEXT NOT NULL, `provinceId` TEXT, `updatedAtEpochMillis` INTEGER NOT NULL, " +
                "PRIMARY KEY(`cacheKey`, `position`))",
        )
    }
}
