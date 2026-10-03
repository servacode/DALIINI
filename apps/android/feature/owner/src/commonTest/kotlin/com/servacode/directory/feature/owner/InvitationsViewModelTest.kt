package com.servacode.directory.feature.owner

import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.FacilityMemberRole
import com.servacode.directory.core.model.ReceivedInvitation
import com.servacode.directory.core.testing.runMainTest
import com.servacode.directory.core.testing.ScriptedOwnerApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.Test

internal fun received(id: String, facilityId: String = "f-$id") = ReceivedInvitation(
    id = id,
    role = FacilityMemberRole.MANAGER,
    facilityId = facilityId,
    facilityNameAr = "صيدلية الشفاء",
    categoryNameAr = "صيدليات",
    provinceNameAr = "الرقة",
    invitedByName = "سامر",
    createdAtEpochMillis = 1_790_000_000_000L,
    expiresAtEpochMillis = 1_790_604_800_000L,
)

class InvitationsViewModelTest {
    private val api = ScriptedOwnerApi()
    private fun viewModel() = InvitationsViewModel(ReceivedInvitationsUseCase(OwnerRepository(api)))

    @Test fun `the waiting invitations are listed`() = runMainTest {
        api.receivedAnswer = { listOf(received("i-1"), received("i-2")) }
        val model = viewModel()
        advanceUntilIdle()

        assertEquals(InvitationsUiState.Content(listOf(received("i-1"), received("i-2"))), model.state.value)
    }

    @Test fun `accepting opens the facility joined and the card goes`() = runMainTest {
        api.receivedAnswer = { listOf(received("i-1"), received("i-2")) }
        api.acceptAnswer = { "f-joined" }
        val model = viewModel()
        advanceUntilIdle()

        model.accept("i-1")
        advanceUntilIdle()

        assertEquals("f-joined", model.joined.value)
        assertEquals(InvitationsUiState.Content(listOf(received("i-2"))), model.state.value)
        model.consumeJoined()
        assertNull(model.joined.value)
    }

    @Test fun `declining only takes the card away`() = runMainTest {
        api.receivedAnswer = { listOf(received("i-1")) }
        val model = viewModel()
        advanceUntilIdle()

        model.decline("i-1")
        advanceUntilIdle()

        assertNull(model.joined.value)
        assertEquals(InvitationsUiState.Content(emptyList()), model.state.value)
        assertEquals(listOf("decline:i-1"), api.calls)
    }

    @Test fun `a refused answer keeps the card and says why`() = runMainTest {
        api.receivedAnswer = { listOf(received("i-1")) }
        val model = viewModel()
        advanceUntilIdle()

        model.accept("i-1")
        advanceUntilIdle()

        val state = model.state.value as InvitationsUiState.Content
        assertEquals(listOf(received("i-1")), state.items)
        assertNull(state.busy)
        assertEquals(AppError.Kind.OFFLINE, state.failure?.kind)
        assertNull(model.joined.value)
    }

    @Test fun `a list that cannot load is an error with a retry`() = runMainTest {
        val model = viewModel()
        advanceUntilIdle()
        assertEquals(AppError.Kind.OFFLINE, (model.state.value as InvitationsUiState.Error).error.kind)

        api.receivedAnswer = { emptyList() }
        model.refresh()
        advanceUntilIdle()

        assertEquals(InvitationsUiState.Content(emptyList()), model.state.value)
    }
}
