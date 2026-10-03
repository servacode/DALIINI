package com.servacode.directory.core.database

import androidx.room.Room
import androidx.sqlite.driver.NativeSQLiteDriver
import com.servacode.directory.core.model.Category
import com.servacode.directory.core.model.CategoryTags
import com.servacode.directory.core.model.EmergencyNumber
import com.servacode.directory.core.model.EmergencyScope
import com.servacode.directory.core.model.FacilityDetail
import com.servacode.directory.core.model.FacilitySummary
import com.servacode.directory.core.model.FacilityTag
import com.servacode.directory.core.model.HomeSnapshot
import com.servacode.directory.core.model.Province
import com.servacode.directory.core.model.RecentFacility
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import platform.Foundation.NSFileManager
import platform.Foundation.NSNumber
import platform.Foundation.NSURL
import platform.Foundation.NSURLIsExcludedFromBackupKey
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The shared Room code on the iPhone's own SQLite: the cache and the device's stores behave as the
 * in-memory fakes the repositories' tests use say they do (core:testing).
 */
class RoomOnIosTest {
    private val database = Room.inMemoryDatabaseBuilder<DirectoryDatabase>()
        .setDriver(NativeSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
    private val cache = PublicCacheDataSource(database.cacheDao())
    private val recent = RoomRecentlyViewedStore(database.localStoresDao())
    private val emergency = RoomEmergencyNumbersCache(database.localStoresDao())

    @AfterTest
    fun close() = database.close()

    private val aleppoHospital = EmergencyNumber("مشفى حلب", "0213333333", EmergencyScope.PROVINCE, "aleppo")

    private fun province(id: String) = Province(id = id, nameAr = "محافظة $id")
    private fun facility(id: String, categoryId: String = "pharmacy") = FacilitySummary(
        id = id,
        nameAr = "صيدلية $id",
        category = Category(id = categoryId, nameAr = "صيدلية"),
    )

    @Test
    fun `provinces are replaced and kept in the order served`() = runTest {
        cache.putProvinces(listOf(province("homs"), province("damascus"), province("aleppo")))
        cache.putProvinces(listOf(province("tartus"), province("homs")))

        assertEquals(listOf("tartus", "homs"), cache.provinces().map { it.id })
    }

    @Test
    fun `a province's home snapshot is read back whole`() = runTest {
        val snapshot = HomeSnapshot(
            province = province("homs"),
            categories = listOf(Category("pharmacy", "صيدلية")),
            nearby = listOf(facility("a")),
            refreshedAtEpochMillis = 1_700_000_000_000,
            dutyNow = listOf(facility("b")),
        )
        cache.putHome(snapshot)

        assertEquals(snapshot, cache.home("homs"))
        assertNull(cache.home("aleppo"))
    }

    @Test
    fun `the first page replaces a list and later pages append at their offset`() = runTest {
        cache.putDirectoryPage(listOf(facility("old")), "homs", "pharmacy", offset = 0)
        cache.putDirectoryPage(listOf(facility("a"), facility("b")), "homs", "pharmacy", offset = 0)
        cache.putDirectoryPage(listOf(facility("c")), "homs", "pharmacy", offset = 2)
        cache.putDirectoryPage(listOf(facility("x", "lab")), "homs", "lab", offset = 0)

        assertEquals(listOf("a", "b", "c"), cache.directory("homs", "pharmacy").map { it.id })
        assertEquals(listOf("x"), cache.directory("homs", "lab").map { it.id })
        assertEquals(emptyList(), cache.directory("aleppo", "pharmacy"))
    }

    @Test
    fun `a facility's detail survives its list being fetched again`() = runTest {
        cache.putDirectoryPage(listOf(facility("a")), "homs", "pharmacy", offset = 0)
        val detail = FacilityDetail(summary = facility("a"), phone = "0311234567", addressAr = "شارع الحضارة")
        cache.putFacility(detail, "homs")

        cache.putDirectoryPage(listOf(facility("b"), facility("a")), "homs", "pharmacy", offset = 0)

        assertEquals(detail, cache.facility("a"))
        assertEquals(listOf("b", "a"), cache.directory("homs", "pharmacy").map { it.id })
    }

    @Test
    fun `a category's specialties and services are kept per category`() = runTest {
        val tags = CategoryTags(
            specialties = listOf(FacilityTag("1", "أطفال")),
            services = listOf(FacilityTag("2", "توصيل")),
        )
        cache.putCategoryTags(tags, "clinic", "homs")

        assertEquals(tags, cache.categoryTags("clinic"))
        assertNull(cache.categoryTags("pharmacy"))
    }

    @Test
    fun `a row this build cannot read is skipped rather than thrown`() = runTest {
        cache.putProvinces(listOf(province("homs")))
        database.cacheDao().putProvinces(listOf(ProvinceCacheEntity("broken", 1, "{not json", 0)))

        assertEquals(listOf("homs"), cache.provinces().map { it.id })
    }

    @Test
    fun `writing a home snapshot tells the widget which province changed`() = runTest {
        val heard = async { CacheEvents.homeSnapshots.first { it == "latakia" } }
        yield()

        cache.putHome(HomeSnapshot(province("latakia"), emptyList(), emptyList(), 0))

        assertEquals("latakia", heard.await())
    }

    @Test
    fun `recently viewed is newest first without repeats and capped`() = runTest {
        repeat(RecentlyViewedStore.LIMIT + 3) { index ->
            recent.record(RecentFacility("f$index", "منشأة $index", null, viewedAtEpochMillis = index.toLong()))
        }
        recent.record(RecentFacility("f5", "منشأة 5", "صيدلية", viewedAtEpochMillis = 1_000))

        val rows = recent.observe().first()
        assertEquals(RecentlyViewedStore.LIMIT, rows.size)
        assertEquals("f5", rows.first().id)
        assertEquals("صيدلية", rows.first().categoryNameAr)
        assertEquals(1, rows.count { it.id == "f5" })
        assertTrue(rows.none { it.id == "f0" }, "the oldest visits are trimmed")

        recent.clear()
        assertEquals(emptyList(), recent.observe().first())
    }

    @Test
    fun `emergency numbers are the national ones first and then the province's`() = runTest {
        emergency.write("aleppo", listOf(aleppoHospital))
        emergency.write(
            "homs",
            listOf(
                EmergencyNumber("مشفى حمص", "0312222222", EmergencyScope.PROVINCE, "homs"),
                EmergencyNumber("الإسعاف", "110", EmergencyScope.NATIONAL),
                EmergencyNumber("الشرطة", "112", EmergencyScope.NATIONAL),
            ),
        )

        assertEquals(listOf("110", "112", "0312222222"), emergency.read("homs").map { it.number })
        assertEquals(listOf("110", "112", "0213333333"), emergency.read("aleppo").map { it.number })
        assertEquals(listOf("110", "112"), emergency.read(null).map { it.number })
    }

    @Test
    fun `writing a province's numbers replaces the national ones too`() = runTest {
        emergency.write("homs", listOf(EmergencyNumber("الإسعاف", "110", EmergencyScope.NATIONAL)))
        emergency.write("aleppo", listOf(aleppoHospital))

        // As the fake has it: the answer for a province carries the national rows, so an answer
        // without them means there are none.
        assertEquals(emptyList(), emergency.read("homs").map { it.number })
    }

    @Test
    fun `a scope this build does not know is left out`() = runTest {
        database.localStoresDao().putEmergencyNumbers(
            listOf(
                EmergencyNumberEntity("national", 0, "الإسعاف", "110", "NATIONAL", null, 0),
                EmergencyNumberEntity("national", 1, "شيء جديد", "999", "GALACTIC", null, 0),
            ),
        )

        assertEquals(listOf("110"), emergency.read(null).map { it.number })
    }

    @OptIn(ExperimentalForeignApi::class)
    @Test
    fun `the database folder exists and is kept out of backups`() {
        val folder = databaseFolder()

        assertTrue(NSFileManager.defaultManager.fileExistsAtPath(folder), folder)
        val values = NSURL.fileURLWithPath(folder)
            .resourceValuesForKeys(listOf(NSURLIsExcludedFromBackupKey), null)
        val excluded = when (val value = values?.get(NSURLIsExcludedFromBackupKey)) {
            is Boolean -> value
            is NSNumber -> value.boolValue
            else -> null
        }
        assertEquals(true, excluded)
    }
}
