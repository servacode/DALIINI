package com.servacode.directory.feature.account

import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.Page
import com.servacode.directory.core.testing.facility
import com.servacode.directory.core.testing.runMainTest
import com.servacode.directory.core.testing.ScriptedPublicApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.Test

/** The account's saved facilities, shared since DECISION-098, on both platforms. */
class FavoritesViewModelTest {
    private val api = ScriptedPublicApi()

    @Test fun `the saved facilities are listed and more are asked for with the backend's cursor`() = runMainTest {
        api.favoritesAnswer = { cursor ->
            if (cursor == null) Page(listOf(facility("a")), "next", true) else Page(listOf(facility("b")), null, false)
        }
        val model = FavoritesViewModel(SavedRepository(api))
        advanceUntilIdle()

        model.loadMore()
        advanceUntilIdle()

        val content = model.state.value as FavoritesUiState.Content
        assertEquals(listOf("a", "b"), content.items.map { it.id })
        assertFalse(content.hasMore)
    }

    @Test fun `offline the list is an error to retry`() = runMainTest {
        val model = FavoritesViewModel(SavedRepository(api))
        advanceUntilIdle()

        assertEquals(AppError.Kind.OFFLINE, (model.state.value as FavoritesUiState.Error).error.kind)
    }
}
