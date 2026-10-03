package com.servacode.directory.core.database

import androidx.room.Room
import androidx.sqlite.driver.NativeSQLiteDriver
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSURLIsExcludedFromBackupKey
import platform.Foundation.NSUserDomainMask

/**
 * The iPhone's database, `Application Support/database/directory-cache.db`, on the system's own
 * SQLite. Opened once per process; the app's graph holds the one this returns.
 */
val iosDirectoryDatabase: DirectoryDatabase by lazy {
    Room.databaseBuilder<DirectoryDatabase>(name = "${databaseFolder()}/${DirectoryDatabase.FILE_NAME}")
        .setDriver(NativeSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}

/**
 * The folder the database lives in, made if it is not there yet and kept out of the phone's
 * backups.
 *
 * The cache is public and fetched again whenever it is missing, and what the reader keeps here
 * (recently viewed, emergency numbers) stays on this phone: Android's app is not backed up
 * either (`allowBackup="false"`). Apple's storage rules say the same of anything an app can
 * download again.
 */
@OptIn(ExperimentalForeignApi::class)
internal fun databaseFolder(): String {
    val manager = NSFileManager.defaultManager
    val support: NSURL = requireNotNull(
        manager.URLForDirectory(
            directory = NSApplicationSupportDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = null,
        ),
    ) { "Application Support is not available" }
    val folder = requireNotNull(support.URLByAppendingPathComponent("database", isDirectory = true)) {
        "no database folder"
    }
    manager.createDirectoryAtURL(folder, withIntermediateDirectories = true, attributes = null, error = null)
    folder.setResourceValue(true, forKey = NSURLIsExcludedFromBackupKey, error = null)
    return requireNotNull(folder.path) { "the database folder has no path" }
}
