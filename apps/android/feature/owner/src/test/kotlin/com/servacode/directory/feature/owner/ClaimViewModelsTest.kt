package com.servacode.directory.feature.owner

import android.content.ContextWrapper
import androidx.lifecycle.SavedStateHandle
import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.AppException
import com.servacode.directory.core.model.ClaimEvidence
import com.servacode.directory.core.model.ClaimRequirement
import com.servacode.directory.core.model.ClaimStatus
import com.servacode.directory.core.model.ClaimableFacility
import com.servacode.directory.core.model.FacilityClaim
import com.servacode.directory.core.network.OwnerUploadPayload
import com.servacode.directory.core.network.UploadReader
import com.servacode.directory.core.testing.MainDispatcherRule
import com.servacode.directory.core.testing.ScriptedOwnerApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private val licence =
    ClaimRequirement("7", "رخصة مزاولة المهنة", required = true, minFiles = 1, maxFiles = 2)
private val storefront = ClaimRequirement("8", "صورة الواجهة", required = false, minFiles = 0, maxFiles = 1)

internal fun claim(
    id: String = "c-1",
    status: ClaimStatus = ClaimStatus.DRAFT,
    evidence: List<ClaimEvidence> = emptyList(),
) = FacilityClaim(
    id = id,
    status = status,
    facilityId = "f-9",
    facilityNameAr = "صيدلية النور",
    categoryNameAr = "صيدليات",
    provinceNameAr = "حلب",
    requirements = listOf(licence, storefront),
    evidence = evidence,
)

class FacilityClaimTest {
    @Test fun `a draft may be sent once every required document is there`() {
        assertEquals(listOf(licence), claim().missing)
        assertFalse(claim().canSubmit)

        val ready = claim(evidence = listOf(ClaimEvidence("e-1", "7", 0)))
        assertTrue(ready.missing.isEmpty())
        assertTrue(ready.canSubmit)
        assertFalse(ready.copy(status = ClaimStatus.SUBMITTED).canSubmit)
    }

    @Test fun `only an open claim can be withdrawn`() {
        assertTrue(claim(status = ClaimStatus.DRAFT).canWithdraw)
        assertTrue(claim(status = ClaimStatus.SUBMITTED).canWithdraw)
        assertFalse(claim(status = ClaimStatus.APPROVED).canWithdraw)
        assertFalse(claim(status = ClaimStatus.REJECTED).canWithdraw)
    }
}

class ClaimSearchViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val api = ScriptedOwnerApi()
    private fun viewModel() = ClaimSearchViewModel(ClaimFacilityUseCase(OwnerRepository(api)))
    private val nour = ClaimableFacility("f-9", "صيدلية النور", "صيدليات", "حلب")

    @Test fun `one letter is not looked up`() = runTest(main.dispatcher) {
        val model = viewModel()
        model.query("ص")
        advanceUntilIdle()

        assertNull(model.state.value.results)
        assertTrue(api.calls.isEmpty())
    }

    @Test fun `typing waits for a pause, then looks up only the last name`() = runTest(main.dispatcher) {
        api.claimableAnswer = { listOf(nour) }
        val model = viewModel()

        model.query("صي")
        advanceTimeBy(CLAIM_SEARCH_PAUSE_MS / 2)
        model.query("صيدلية الن")
        advanceUntilIdle()

        assertEquals(listOf("claimable:صيدلية الن"), api.calls)
        assertEquals(listOf(nour), model.state.value.results)
    }

    @Test fun `picking a facility starts a claim and opens it`() = runTest(main.dispatcher) {
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

    @Test fun `a refused start says why and keeps the results`() = runTest(main.dispatcher) {
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

class ClaimViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val api = ScriptedOwnerApi()
    private val file = OwnerUploadPayload("upload.jpg", "image/jpeg", byteArrayOf(1, 2, 3))

    private fun viewModel() = ClaimViewModel(
        SavedStateHandle(mapOf("id" to "c-1")),
        ClaimFacilityUseCase(OwnerRepository(api)),
        // Never reached: the tests hand the bytes over themselves.
        UploadReader(ContextWrapper(null)),
    )

    @Test fun `an uploaded document counts toward sending`() = runTest(main.dispatcher) {
        api.claimAnswer = { claim() }
        api.claimEvidenceAnswer = { _, requirementId -> ClaimEvidence("e-1", requirementId, 0) }
        val model = viewModel()
        advanceUntilIdle()

        model.send("7", Result.success(file))
        advanceUntilIdle()

        val state = model.state.value as ClaimUiState.Content
        assertNull(state.uploading)
        assertTrue(state.claim.canSubmit)
        assertEquals(listOf("claimEvidence:c-1:7"), api.calls)
    }

    @Test fun `a file that cannot be read says so`() = runTest(main.dispatcher) {
        api.claimAnswer = { claim() }
        val model = viewModel()
        advanceUntilIdle()

        model.send("7", Result.failure(IllegalStateException("unreadable")))

        assertTrue((model.state.value as ClaimUiState.Content).unreadable)
        assertTrue(api.calls.isEmpty())
    }

    @Test fun `sending replaces the draft with what the backend answered`() = runTest(main.dispatcher) {
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

    @Test fun `deleting a document takes it off the list`() = runTest(main.dispatcher) {
        api.claimAnswer = { claim(evidence = listOf(ClaimEvidence("e-1", "7", 0))) }
        val model = viewModel()
        advanceUntilIdle()

        model.deleteEvidence("e-1")
        advanceUntilIdle()

        assertEquals(emptyList<ClaimEvidence>(), (model.state.value as ClaimUiState.Content).claim.evidence)
        assertEquals(listOf("deleteClaimEvidence:c-1:e-1"), api.calls)
    }

    @Test fun `withdrawing closes the screen`() = runTest(main.dispatcher) {
        api.claimAnswer = { claim(status = ClaimStatus.SUBMITTED) }
        val model = viewModel()
        advanceUntilIdle()

        model.withdraw()
        advanceUntilIdle()

        assertTrue(model.withdrawn.value)
        assertEquals(listOf("withdrawClaim:c-1"), api.calls)
    }

    @Test fun `after a refusal a new claim on the same facility opens`() = runTest(main.dispatcher) {
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
