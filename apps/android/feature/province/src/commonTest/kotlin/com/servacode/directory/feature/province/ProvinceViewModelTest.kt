package com.servacode.directory.feature.province

import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.Province
import com.servacode.directory.core.testing.FakePreferences
import com.servacode.directory.core.testing.FakePublicCache
import com.servacode.directory.core.testing.runMainTest
import com.servacode.directory.core.testing.ScriptedPublicApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.Test

class ProvinceViewModelTest {
    private val cache = FakePublicCache()
    private val api = ScriptedPublicApi()
    private val preferences = FakePreferences()

    private fun viewModel() = ProvinceViewModel(ProvinceUseCase(ProvinceRepository(cache, api, preferences)))

    @Test fun `the backend's list is shown and saved for next time`() = runMainTest {
        api.provincesAnswer = { listOf(Province("raqqa", "R"), Province("aleppo", "A")) }

        val model = viewModel()
        advanceUntilIdle()

        val content = model.state.value as ProvinceUiState.Content
        assertEquals(listOf("raqqa", "aleppo"), content.provinces.map { it.id })
        assertEquals(false, content.stale)
        assertEquals(listOf("raqqa", "aleppo"), cache.provinces.map { it.id })
    }

    @Test fun `offline the saved list is shown and said to be saved`() = runMainTest {
        cache.provinces = listOf(Province("raqqa", "R"))

        val model = viewModel()
        advanceUntilIdle()

        val content = model.state.value as ProvinceUiState.Content
        assertEquals(listOf("raqqa"), content.provinces.map { it.id })
        assertTrue(content.stale)
    }

    @Test fun `offline with nothing saved is an error that a retry recovers from`() = runMainTest {
        val model = viewModel()
        advanceUntilIdle()
        assertEquals(AppError.Kind.OFFLINE, (model.state.value as ProvinceUiState.Error).error.kind)

        api.provincesAnswer = { listOf(Province("raqqa", "R")) }
        model.refresh()
        advanceUntilIdle()

        assertTrue(model.state.value is ProvinceUiState.Content)
    }

    @Test fun `choosing a province keeps it and only then moves on`() = runMainTest {
        api.provincesAnswer = { listOf(Province("raqqa", "R")) }
        val model = viewModel()
        advanceUntilIdle()
        var moved: String? = null

        model.select("raqqa") { moved = "home" }
        advanceUntilIdle()

        assertEquals("raqqa", preferences.values.first().selectedProvinceId)
        assertEquals("home", moved)
    }
}
