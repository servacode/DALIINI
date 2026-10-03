package com.servacode.directory.core.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.cinterop.ExperimentalForeignApi
import okio.Path.Companion.toPath
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

/**
 * The iPhone's preferences file: `Application Support/datastore/directory_preferences.preferences_pb`,
 * the place Apple names for what an app keeps for itself and the name Android gives it.
 *
 * Made once per process, because DataStore refuses a second instance on the same file; the app's
 * graph holds the one this returns.
 */
val iosDirectoryDataStore: DirectoryDataStore by lazy {
    DirectoryDataStore(
        PreferenceDataStoreFactory.createWithPath(
            produceFile = { "${dataStoreFolder()}/${DirectoryDataStore.NAME}.preferences_pb".toPath() },
        ),
    )
}

@OptIn(ExperimentalForeignApi::class)
private fun dataStoreFolder(): String {
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
    val folder = requireNotNull(support.URLByAppendingPathComponent("datastore")) { "no datastore folder" }
    manager.createDirectoryAtURL(folder, withIntermediateDirectories = true, attributes = null, error = null)
    return requireNotNull(folder.path) { "the datastore folder has no path" }
}
