package com.servacode.directory.feature.home

import com.servacode.directory.core.database.Loaded
import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.HomeSnapshot
import com.servacode.directory.core.model.Province
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

class HomeRepositoryTest {
    private val raqqa = Province("raqqa", "الرقة")
    private val cache = FakePublicCache()
    private val api = ScriptedPublicApi()

    private fun snapshot(vararg ids: String) =
        HomeSnapshot(raqqa, emptyList(), ids.map { facility(it) }, refreshedAtEpochMillis = 1)

    private fun repository(province: String? = "raqqa", location: FakeLocation = FakeLocation()) =
        HomeRepository(cache, api, FakePreferences(province), location)

    @Test fun `shows the cache first, then the backend's answer, and stores it`() = runTest {
        cache.provinces = listOf(raqqa)
        cache.homes["raqqa"] = snapshot("old")
        api.homeAnswer = { snapshot("new") }

        val emitted = repository().load().toList()

        assertEquals(
            listOf(
                HomeLoad.Snapshot(Loaded.Cached(snapshot("old"))),
                HomeLoad.Snapshot(Loaded.Fresh(snapshot("new"))),
            ),
            emitted,
        )
        assertEquals(snapshot("new"), cache.homes["raqqa"])
    }

    @Test fun `offline, the cache stays on screen marked stale`() = runTest {
        cache.provinces = listOf(raqqa)
        cache.homes["raqqa"] = snapshot("old")

        val last = repository().load().toList().last() as HomeLoad.Snapshot

        val stale = last.loaded as Loaded.Stale
        assertEquals(snapshot("old"), stale.value)
        assertEquals(AppError.Kind.OFFLINE, stale.error.kind)
    }

    @Test fun `nothing cached and offline is a failure, not an empty home`() = runTest {
        cache.provinces = listOf(raqqa)

        val only = repository().load().toList().single() as HomeLoad.Snapshot

        assertTrue(only.loaded is Loaded.Failed)
    }

    @Test fun `without a selected province the user is asked to choose one`() = runTest {
        assertEquals(listOf(HomeLoad.ProvinceRequired), repository(province = null).load().toList())
    }

    @Test fun `a province the backend no longer serves sends the user back to choose`() = runTest {
        cache.homes["aleppo"] = snapshot("old")
        api.provincesAnswer = { listOf(raqqa) }

        val emitted = repository(province = "aleppo").load().toList()

        assertEquals(HomeLoad.ProvinceRequired, emitted.last())
    }

    @Test fun `location is sent only when the user allowed it`() = runTest {
        cache.provinces = listOf(raqqa)
        api.homeAnswer = { snapshot("new") }

        repository(location = FakeLocation(fix(35.95, 39.01))).load().toList()
        repository(location = FakeLocation()).load().toList()

        assertEquals(listOf("home:raqqa:35.95:39.01", "home:raqqa:null:null"), api.calls)
    }
}
