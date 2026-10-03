package com.servacode.directory.feature.owner

import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.FacilityInvitation
import com.servacode.directory.core.model.FacilityMember
import com.servacode.directory.core.model.FacilityMemberRole
import com.servacode.directory.core.model.InvitationStatus
import com.servacode.directory.core.network.RealtimeInvalidationBus
import com.servacode.directory.core.testing.runMainTest
import com.servacode.directory.core.testing.ScriptedOwnerApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.Test

private fun invitation(id: String, status: InvitationStatus) = FacilityInvitation(
    id = id,
    phone = "+963933000111",
    role = FacilityMemberRole.MANAGER,
    status = status,
    createdAtEpochMillis = 1_790_000_000_000L,
    expiresAtEpochMillis = 1_790_604_800_000L,
)

class ManageFacilityViewModelTest {
    private val api = ScriptedOwnerApi().apply {
        facilityAnswer = { confirmable() }
        closuresAnswer = { emptyList() }
        membersAnswer = { listOf(FacilityMember("u-1", "سامر", role = FacilityMemberRole.OWNER)) }
    }

    private fun viewModel(): ManageFacilityViewModel {
        val repository = OwnerRepository(api)
        return ManageFacilityViewModel(
            "f-1",
            LoadManageFacilityUseCase(repository),
            ManageFacilityUseCase(repository),
            RealtimeInvalidationBus(),
        )
    }

    @Test fun `the owner sees the invitations with those still waiting first`() = runMainTest {
        api.invitationsAnswer = {
            listOf(invitation("a", InvitationStatus.DECLINED), invitation("b", InvitationStatus.PENDING))
        }
        val model = viewModel()
        advanceUntilIdle()

        val state = model.state.value as ManageFacilityUiState.Content
        assertEquals(listOf("b", "a"), state.invitations?.map { it.id })
    }

    @Test fun `a manager is not refused the page for not seeing invitations`() = runMainTest {
        // The fake's unset answer fails, as the backend refuses anyone but the owner.
        val model = viewModel()
        advanceUntilIdle()

        val state = model.state.value as ManageFacilityUiState.Content
        assertNull(state.invitations)
    }

    @Test fun `inviting says to whom and reads the list again`() = runMainTest {
        var sent = emptyList<FacilityInvitation>()
        api.invitationsAnswer = { sent }
        api.inviteAnswer = { _, _, _ -> invitation("c", InvitationStatus.PENDING).also { sent = listOf(it) } }
        val model = viewModel()
        advanceUntilIdle()

        model.invite(" 0933000111 ")
        advanceUntilIdle()

        val state = model.state.value as ManageFacilityUiState.Content
        assertEquals("0933000111", state.invited)
        assertEquals(listOf("c"), state.invitations?.map { it.id })
        assertEquals(listOf("invite:f-1:0933000111"), api.calls)

        model.dismissInvited()
        assertNull((model.state.value as ManageFacilityUiState.Content).invited)
    }

    @Test fun `a refused invitation stays on the page as its reason`() = runMainTest {
        api.invitationsAnswer = { emptyList() }
        val model = viewModel()
        advanceUntilIdle()

        model.invite("123")
        advanceUntilIdle()

        val state = model.state.value as ManageFacilityUiState.Content
        assertNull(state.invited)
        assertEquals(AppError.Kind.OFFLINE, state.failure?.kind)
    }

    @Test fun `revoking asks the backend and reads the list again`() = runMainTest {
        api.invitationsAnswer = { listOf(invitation("b", InvitationStatus.PENDING)) }
        val model = viewModel()
        advanceUntilIdle()

        model.revokeInvitation("b")
        advanceUntilIdle()

        assertEquals(listOf("revokeInvitation:f-1:b"), api.calls)
    }
}
