package com.servacode.directory.feature.owner

import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.AppException
import com.servacode.directory.core.model.ClaimableFacility
import com.servacode.directory.core.testing.runMainTest
import com.servacode.directory.core.testing.ScriptedOwnerApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.Test

class ClaimSearchViewModelTest {
    private val api = ScriptedOwnerApi()
    private fun viewModel() = ClaimSearchViewModel(ClaimFacilityUseCase(OwnerRepository(api)))
    private val nour = ClaimableFacility("f-9", "صيدلية النور", "صيدليات", "حلب")

    @Test fun `one letter is not looked up`() = runMainTest {
        val model = viewModel()
        model.query("ص")
        advanceUntilIdle()

        assertNull(model.state.value.results)
        assertTrue(api.calls.isEmpty())
    }

    @Test fun `typing waits for a pause and then looks up only the last name`() = runMainTest {
        api.claimableAnswer = { listOf(nour) }
        val model = viewModel()

        model.query("صي")
        advanceTimeBy(CLAIM_SEARCH_PAUSE_MS / 2)
        model.query("صيدلية الن")
        advanceUntilIdle()

        assertEquals(listOf("claimable:صيدلية الن"), api.calls)
        assertEquals(listOf(nour), model.state.value.results)
    }

    @Test fun `picking a facility starts a claim and opens it`() = runMainTest {
        api.claimableAnswer = { listOf(nour) }
        api.startClaimAnswer = { claim(id = "c-7") }
        val model = viewModel()
        model.query("النور")
        advanceUntilIdle()

        model.start("f-9")
        advanceUntilIdle()

        assertEquals("c-7", model.opened.value)
        assertNull(model.state.value.starting)
        model.consumeOpened()
        assertNull(model.opened.value)
    }

    @Test fun `a refused start says why and keeps the results`() = runMainTest {
        api.claimableAnswer = { listOf(nour) }
        api.startClaimAnswer = {
            throw AppException(AppError(AppError.Kind.CONFLICT, code = "TOO_MANY_CLAIMS", status = 409))
        }
        val model = viewModel()
        model.query("النور")
        advanceUntilIdle()

        model.start("f-9")
        advanceUntilIdle()

        assertNull(model.opened.value)
        assertEquals("TOO_MANY_CLAIMS", model.state.value.failure?.code)
        assertEquals(listOf(nour), model.state.value.results)
    }
}
