package com.servacode.directory.core.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toPath
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The preferences on the iPhone's own file system, written by one instance and read by the next. */
@OptIn(ExperimentalForeignApi::class)
class IosDirectoryDataStoreTest {
    private val folder = NSTemporaryDirectory() + "datastore-test-" + NSUUID().UUIDString()
    private val file = "$folder/${DirectoryDataStore.NAME}.preferences_pb"

    @AfterTest
    fun removeTheFolder() {
        NSFileManager.defaultManager.removeItemAtPath(folder, error = null)
    }

    private fun open(scope: CoroutineScope) = DirectoryDataStore(
        PreferenceDataStoreFactory.createWithPath(scope = scope, produceFile = { file.toPath() }),
    )

    @Test
    fun `what one launch writes the next launch reads`() = runBlocking {
        val firstLaunch = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val written = open(firstLaunch)
        PreferencesRepository(written).selectProvince("homs")
        PreferencesRepository(written).setThemePreference(ThemePreference.DARK)
        val id = StoredAnonymousId(written).current()
        firstLaunch.coroutineContext[Job]!!.cancelAndJoin()

        assertTrue(NSFileManager.defaultManager.fileExistsAtPath(file), "no file at $file")

        val secondLaunch = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val read = open(secondLaunch)
        val values = PreferencesRepository(read).values.first()
        assertEquals("homs", values.selectedProvinceId)
        assertEquals(ThemePreference.DARK, values.themePreference)
        assertEquals(id, StoredAnonymousId(read).current())
        secondLaunch.coroutineContext[Job]!!.cancelAndJoin()
    }
}
