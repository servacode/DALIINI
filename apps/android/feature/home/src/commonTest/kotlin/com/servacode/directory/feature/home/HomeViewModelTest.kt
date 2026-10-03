package com.servacode.directory.feature.home

import com.servacode.directory.core.analytics.NoOpAnalyticsTracker
import com.servacode.directory.core.model.HomeSnapshot
import com.servacode.directory.core.model.Province
import com.servacode.directory.core.network.BackendLocationNameResolver
import com.servacode.directory.core.network.RealtimeInvalidationBus
import com.servacode.directory.core.testing.facility
import com.servacode.directory.core.testing.FakeLocation
import com.servacode.directory.core.testing.FakePreferences
import com.servacode.directory.core.testing.FakePublicCache
import com.servacode.directory.core.testing.runMainTest
import com.servacode.directory.core.testing.ScriptedPublicApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.Test

/** The home's view model, shared since DECISION-095, on the JVM and on the iPhone simulator. */
class HomeViewModelTest {
    private val raqqa = Province("raqqa", "R")
    private val cache = FakePublicCache().apply { provinces = listOf(raqqa) }
    private val api = ScriptedPublicApi()

    private fun snapshot(vararg ids: String) =
        HomeSnapshot(raqqa, emptyList(), ids.map { facility(it) }, refreshedAtEpochMillis = 1)

    private fun viewModel(province: String? = "raqqa") = HomeViewModel(
        HomeUseCase(
            HomeRepository(
                cache,
                api,
                FakePreferences(province),
                FakeLocation(),
                BackendLocationNameResolver(api) { 0L },
            ),
        ),
        HomeAdsUseCase(HomeAdsRepository(cache, api)),
        RealtimeInvalidationBus(),
        NoOpAnalyticsTracker,
    )

    @Test fun `without a province the home asks for one`() = runMainTest {
        val model = viewModel(province = null)
        advanceUntilIdle()

        assertEquals(HomeUiState.ProvinceRequired, model.state.value)
    }

    @Test fun `the backend's home for the province is shown`() = runMainTest {
        api.homeAnswer = { snapshot("a", "b") }

        val model = viewModel()
        advanceUntilIdle()

        val content = model.state.value as HomeUiState.Content
        assertEquals(listOf("a", "b"), content.snapshot.nearby.map { it.id })
        assertEquals(false, content.stale)
    }

    @Test fun `offline the saved home stays on screen and says so`() = runMainTest {
        cache.homes["raqqa"] = snapshot("old")

        val model = viewModel()
        advanceUntilIdle()

        val content = model.state.value as HomeUiState.Content
        assertEquals(listOf("old"), content.snapshot.nearby.map { it.id })
        assertTrue(content.stale)
    }
}
