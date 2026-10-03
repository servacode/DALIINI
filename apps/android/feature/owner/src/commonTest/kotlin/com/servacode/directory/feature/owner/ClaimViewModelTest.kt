package com.servacode.directory.feature.owner

import com.servacode.directory.core.model.ClaimEvidence
import com.servacode.directory.core.model.ClaimStatus
import com.servacode.directory.core.network.OwnerUploadPayload
import com.servacode.directory.core.testing.runMainTest
import com.servacode.directory.core.testing.ScriptedOwnerApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.Test

class ClaimViewModelTest {
    private val api = ScriptedOwnerApi()
    private val file = OwnerUploadPayload("upload.jpg", "image/jpeg", byteArrayOf(1, 2, 3))

    private fun viewModel() = ClaimViewModel("c-1", ClaimFacilityUseCase(OwnerRepository(api)))

    @Test fun `an uploaded document counts toward sending`() = runMainTest {
        api.claimAnswer = { claim() }
        api.claimEvidenceAnswer = { _, requirementId -> ClaimEvidence("e-1", requirementId, 0) }
        val model = viewModel()
        advanceUntilIdle()

        model.upload("7", file)
        advanceUntilIdle()

        val state = model.state.value as ClaimUiState.Content
        assertNull(state.uploading)
        assertTrue(state.claim.canSubmit)
        assertEquals(listOf("claimEvidence:c-1:7"), api.calls)
    }

    @Test fun `a file that cannot be read says so`() = runMainTest {
        api.claimAnswer = { claim() }
        val model = viewModel()
        advanceUntilIdle()

        model.upload("7", null)

        assertTrue((model.state.value as ClaimUiState.Content).unreadable)
        assertTrue(api.calls.isEmpty())
    }

    @Test fun `sending replaces the draft with what the backend answered`() = runMainTest {
        val ready = claim(evidence = listOf(ClaimEvidence("e-1", "7", 0)))
        api.claimAnswer = { ready }
        api.submitClaimAnswer = { ready.copy(status = ClaimStatus.SUBMITTED, submittedAtEpochMillis = 1) }
        val model = viewModel()
        advanceUntilIdle()

        model.submit()
        advanceUntilIdle()

        val state = model.state.value as ClaimUiState.Content
        assertEquals(ClaimStatus.SUBMITTED, state.claim.status)
        assertFalse(state.busy)
    }

    @Test fun `deleting a document takes it off the list`() = runMainTest {
        api.claimAnswer = { claim(evidence = listOf(ClaimEvidence("e-1", "7", 0))) }
        val model = viewModel()
        advanceUntilIdle()

        model.deleteEvidence("e-1")
        advanceUntilIdle()

        assertEquals(emptyList<ClaimEvidence>(), (model.state.value as ClaimUiState.Content).claim.evidence)
        assertEquals(listOf("deleteClaimEvidence:c-1:e-1"), api.calls)
    }

    @Test fun `withdrawing closes the screen`() = runMainTest {
        api.claimAnswer = { claim(status = ClaimStatus.SUBMITTED) }
        val model = viewModel()
        advanceUntilIdle()

        model.withdraw()
        advanceUntilIdle()

        assertTrue(model.withdrawn.value)
        assertEquals(listOf("withdrawClaim:c-1"), api.calls)
    }

    @Test fun `after a refusal a new claim on the same facility opens`() = runMainTest {
        api.claimAnswer = { claim(status = ClaimStatus.REJECTED) }
        api.startClaimAnswer = { claim(id = "c-2") }
        val model = viewModel()
        advanceUntilIdle()

        model.startAgain()
        advanceUntilIdle()

        assertEquals("c-2", model.reopened.value)
        assertEquals(listOf("startClaim:f-9"), api.calls)
    }
}
