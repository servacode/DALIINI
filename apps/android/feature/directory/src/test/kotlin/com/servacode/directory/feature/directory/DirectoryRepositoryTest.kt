package com.servacode.directory.feature.directory

import com.servacode.directory.core.database.Loaded
import com.servacode.directory.core.model.Page
import com.servacode.directory.core.testing.FakeLocation
import com.servacode.directory.core.testing.FakePreferences
import com.servacode.directory.core.testing.FakePublicCache
import com.servacode.directory.core.testing.ScriptedPublicApi
import com.servacode.directory.core.testing.facility
import com.servacode.directory.core.testing.fix
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DirectoryRepositoryTest {
    private val cache = FakePublicCache()
    private val api = ScriptedPublicApi()
    private val repository =
        DirectoryRepository(cache, api, FakePreferences("raqqa"), FakeLocation(fix(35.95, 39.01)))

    @Test fun `paging keeps the backend's order in the cache, across pages`() = runTest {
        api.directoryAnswer = { _, cursor ->
            when (cursor) {
                null -> Page(listOf(facility("c"), facility("a")), nextCursor = "opaque-1", hasMore = true)
                "opaque-1" -> Page(listOf(facility("b")), nextCursor = null, hasMore = false)
                else -> error("unexpected cursor $cursor")
            }
        }

        val first = repository.firstPage("pharmacy", DirectoryFilter()).toList().last() as DirectoryLoad.FirstPage
        val page = (first.loaded as Loaded.Fresh).value
        val next = repository.nextPage(first.query, page.nextCursor!!, loadedCount = page.items.size)

        assertTrue(!next.hasMore)
        assertEquals(listOf("c", "a", "b"), cache.directory("raqqa", "pharmacy").map { it.id })
        assertEquals(listOf("directory:raqqa:pharmacy@0", "directory:raqqa:pharmacy@2"), cache.writes)
    }

    @Test fun `offline, the cached list replays in the same order, with nothing more to page`() = runTest {
        cache.putDirectoryPage(listOf(facility("c"), facility("a")), "raqqa", "pharmacy", 0)
        cache.putDirectoryPage(listOf(facility("b")), "raqqa", "pharmacy", 2)

        val emitted = repository.firstPage("pharmacy", DirectoryFilter()).toList()

        val stale = (emitted.last() as DirectoryLoad.FirstPage).loaded as Loaded.Stale
        assertEquals(listOf("c", "a", "b"), stale.value.items.map { it.id })
        assertEquals(null, stale.value.nextCursor)
    }

    @Test fun `a filtered list is neither read from nor written to the cache`() = runTest {
        cache.putDirectoryPage(listOf(facility("cached")), "raqqa", "pharmacy", 0)
        cache.writes.clear()
        api.directoryAnswer = { _, _ -> Page(listOf(facility("on-duty")), null, false) }

        val emitted = repository.firstPage("pharmacy", DirectoryFilter(dutyNow = true)).toList()

        assertEquals(1, emitted.size)
        val fresh = (emitted.single() as DirectoryLoad.FirstPage).loaded as Loaded.Fresh
        assertEquals(listOf("on-duty"), fresh.value.items.map { it.id })
        assertTrue(cache.writes.isEmpty())
        assertEquals(true, (emitted.single() as DirectoryLoad.FirstPage).query.dutyNow)
    }

    @Test fun `the location of the first page is the location of every page`() = runTest {
        api.directoryAnswer = { query, _ ->
            assertEquals(35.95, query.latitude!!, 0.0)
            Page(listOf(facility("x")), nextCursor = "n", hasMore = true)
        }

        val first = repository.firstPage("pharmacy", DirectoryFilter()).toList().last() as DirectoryLoad.FirstPage
        repository.nextPage(first.query, "n", 1)

        assertEquals(listOf("directory:pharmacy:null", "directory:pharmacy:n"), api.calls)
    }

    @Test fun `without a province there is nothing to list`() = runTest {
        val noProvince = DirectoryRepository(cache, api, FakePreferences(null), FakeLocation())

        assertEquals(listOf(DirectoryLoad.ProvinceRequired), noProvince.firstPage("pharmacy",
            DirectoryFilter()).toList())
    }
}
