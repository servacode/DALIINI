package com.servacode.directory.feature.ratings

import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.AppException
import com.servacode.directory.core.model.UserRating
import com.servacode.directory.core.testing.runMainTest
import com.servacode.directory.core.testing.ScriptedPublicApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.Test

class RatingsViewModelTest {
    private val api = ScriptedPublicApi()
    private var ratings = listOf(UserRating("r-1", "f-1", "F", 3, 0L))

    private fun viewModel(): RatingsViewModel {
        api.ratingsAnswer = { ratings }
        return RatingsViewModel(RatingsUseCase(RatingsRepository(api)))
    }

    @Test fun `new stars are sent and the list is read again`() = runMainTest {
        api.upsertAnswer = { _, stars -> stars.also { ratings = listOf(ratings.single().copy(stars = it)) } }
        val model = viewModel()
        advanceUntilIdle()

        model.update("f-1", 5)
        advanceUntilIdle()

        val content = model.state.value as RatingsUiState.Content
        assertEquals(5, content.values.single().stars)
        assertNull(content.savingFacilityId)
        assertEquals(listOf("ratings", "rate:f-1:5", "ratings"), api.calls)
    }

    @Test fun `a refused change keeps the list and says why`() = runMainTest {
        api.upsertAnswer = { _, _ -> throw AppException(AppError(AppError.Kind.SERVER)) }
        val model = viewModel()
        advanceUntilIdle()

        model.update("f-1", 5)
        advanceUntilIdle()

        val content = model.state.value as RatingsUiState.Content
        assertEquals(3, content.values.single().stars)
        assertEquals(AppError.Kind.SERVER, content.failure?.kind)
    }

    @Test fun `a deleted rating leaves the list`() = runMainTest {
        val model = viewModel()
        advanceUntilIdle()

        ratings = emptyList()
        model.delete("f-1")
        advanceUntilIdle()

        assertEquals(emptyList(), (model.state.value as RatingsUiState.Content).values)
        assertEquals("unrate:f-1", api.calls[1])
    }
}
