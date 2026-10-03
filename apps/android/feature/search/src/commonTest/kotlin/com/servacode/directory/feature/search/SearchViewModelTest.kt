package com.servacode.directory.feature.search

import com.servacode.directory.core.analytics.AnalyticsEvent
import com.servacode.directory.core.analytics.AnalyticsTracker
import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.Page
import com.servacode.directory.core.testing.FakeLocation
import com.servacode.directory.core.testing.FakePreferences
import com.servacode.directory.core.testing.facility
import com.servacode.directory.core.testing.runMainTest
import com.servacode.directory.core.testing.ScriptedPublicApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.Test

class SearchViewModelTest {
    private val api = ScriptedPublicApi()
    private val events = mutableListOf<AnalyticsEvent>()
    private val analytics = object : AnalyticsTracker {
        override fun track(event: AnalyticsEvent) {
            events += event
        }
    }

    private fun viewModel() = SearchViewModel(
        SearchUseCase(SearchRepository(api, FakePreferences("raqqa"), FakeLocation())),
        analytics,
    )

    @Test fun `typing waits before asking and only the last query is sent`() = runMainTest {
        api.searchAnswer = { query, _ -> Page(listOf(facility(query)), null, false) }
        val model = viewModel()

        model.updateQuery("ph")
        advanceTimeBy(100)
        model.updateQuery("pharmacy")
        advanceUntilIdle()

        assertEquals(listOf("search:pharmacy:null"), api.calls)
        assertEquals(listOf("pharmacy"), (model.state.value as SearchUiState.Results).values.map { it.id })
    }

    @Test fun `more is asked for with the backend's own cursor`() = runMainTest {
        api.searchAnswer = { _, cursor ->
            if (cursor == null) Page(listOf(facility("a")), "next", true) else Page(listOf(facility("b")), null, false)
        }
        val model = viewModel()
        model.updateQuery("lab")
        advanceUntilIdle()

        model.loadMore()
        advanceUntilIdle()

        val results = model.state.value as SearchUiState.Results
        assertEquals(listOf("a", "b"), results.values.map { it.id })
        assertEquals(false, results.hasMore)
        assertEquals("search:lab:next", api.calls.last())
    }

    @Test fun `a search that finds nothing is counted by its length and never its words`() = runMainTest {
        api.searchAnswer = { _, _ -> Page(emptyList(), null, false) }
        val model = viewModel()

        model.updateQuery("  lab  ")
        advanceUntilIdle()

        assertEquals(
            listOf(AnalyticsEvent.SearchSubmitted(3, "raqqa"), AnalyticsEvent.SearchZeroResults(3, "raqqa")),
            events,
        )
    }

    @Test fun `an empty query is idle and a failure offers its error`() = runMainTest {
        val model = viewModel()
        model.updateQuery("   ")
        advanceUntilIdle()
        assertEquals(SearchUiState.Idle, model.state.value)
        assertTrue(api.calls.isEmpty())

        model.updateQuery("lab")
        advanceUntilIdle()

        assertEquals(AppError.Kind.OFFLINE, (model.state.value as SearchUiState.Error).error.kind)
    }
}
