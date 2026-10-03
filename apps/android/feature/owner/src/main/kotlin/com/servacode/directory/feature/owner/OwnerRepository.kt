package com.servacode.directory.feature.owner

import com.servacode.directory.core.model.ClaimEvidence
import com.servacode.directory.core.model.ClaimableFacility
import com.servacode.directory.core.model.FacilityClaim
import com.servacode.directory.core.model.HoursConfirmation
import com.servacode.directory.core.model.OwnerFacilityInsights
import com.servacode.directory.core.model.FacilityInvitation
import com.servacode.directory.core.model.FacilityMember
import com.servacode.directory.core.model.FacilityMemberRole
import com.servacode.directory.core.model.OwnerConfig
import com.servacode.directory.core.model.OwnerFacilityDetail
import com.servacode.directory.core.model.OwnerFacilitySummary
import com.servacode.directory.core.model.ReceivedInvitation
import com.servacode.directory.core.model.TemporaryClosure
import com.servacode.directory.core.network.OwnerApiBoundary
import com.servacode.directory.core.network.OwnerFacilityPatch
import com.servacode.directory.core.network.OwnerUploadPayload
import com.servacode.directory.core.network.TemporaryClosureInput
import javax.inject.Inject

class OwnerRepository @Inject constructor(
    private val api: OwnerApiBoundary,
) {
    suspend fun facilities(): Result<List<OwnerFacilitySummary>> = runCatching { api.facilities() }

    suspend fun facility(id: String): Result<OwnerFacilityDetail> = runCatching { api.facility(id) }

    /** What owners may do in [provinceId]: its categories open to them, and what each lets them pick. */
    suspend fun config(provinceId: String): Result<OwnerConfig> = runCatching { api.ownerConfig(provinceId) }

    suspend fun patch(id: String, patch: OwnerFacilityPatch): Result<OwnerFacilityDetail> =
        runCatching { api.patchFacility(id, patch) }

    /** "Our hours are still right": one tap, recorded by the backend as `hoursConfirmedAt`. */
    suspend fun confirmHours(id: String): Result<HoursConfirmation> = runCatching { api.confirmHours(id) }

    /** Views, calls and directions over the backend's window. Never cached: it is the owner's. */
    suspend fun insights(id: String): Result<OwnerFacilityInsights> = runCatching { api.insights(id) }
    suspend fun closures(id: String): Result<List<TemporaryClosure>> =
        runCatching { api.temporaryClosures(id) }
    suspend fun createClosure(
        id: String,
        input: TemporaryClosureInput,
    ): Result<TemporaryClosure> = runCatching { api.createTemporaryClosure(id, input) }
    suspend fun deleteClosure(id: String, closureId: String): Result<Unit> =
        runCatching { api.deleteTemporaryClosure(id, closureId) }
    suspend fun members(id: String): Result<List<FacilityMember>> = runCatching { api.members(id) }
    suspend fun upsertMember(
        id: String,
        userId: String,
        role: FacilityMemberRole,
    ): Result<FacilityMember> = runCatching { api.upsertMember(id, userId, role) }
    suspend fun deleteMember(id: String, userId: String): Result<Unit> =
        runCatching { api.deleteMember(id, userId) }

    /** Owners only: a manager's request is refused, and the screen then offers no invitations. */
    suspend fun invitations(id: String): Result<List<FacilityInvitation>> = runCatching { api.invitations(id) }
    suspend fun invite(id: String, phone: String, role: FacilityMemberRole): Result<FacilityInvitation> =
        runCatching { api.invite(id, phone, role) }
    suspend fun revokeInvitation(id: String, invitationId: String): Result<Unit> =
        runCatching { api.revokeInvitation(id, invitationId) }
    suspend fun receivedInvitations(): Result<List<ReceivedInvitation>> = runCatching { api.receivedInvitations() }
    suspend fun acceptInvitation(invitationId: String): Result<String> =
        runCatching { api.acceptInvitation(invitationId) }
    suspend fun declineInvitation(invitationId: String): Result<Unit> =
        runCatching { api.declineInvitation(invitationId) }

    suspend fun claimable(query: String): Result<List<ClaimableFacility>> =
        runCatching { api.claimableFacilities(query) }
    suspend fun claims(): Result<List<FacilityClaim>> = runCatching { api.claims() }
    suspend fun claim(claimId: String): Result<FacilityClaim> = runCatching { api.claim(claimId) }
    suspend fun startClaim(facilityId: String): Result<FacilityClaim> = runCatching { api.startClaim(facilityId) }
    suspend fun uploadClaimEvidence(
        claimId: String,
        requirementId: String,
        payload: OwnerUploadPayload,
    ): Result<ClaimEvidence> = runCatching { api.uploadClaimEvidence(claimId, requirementId, payload) }
    suspend fun deleteClaimEvidence(claimId: String, evidenceId: String): Result<Unit> =
        runCatching { api.deleteClaimEvidence(claimId, evidenceId) }
    suspend fun submitClaim(claimId: String): Result<FacilityClaim> = runCatching { api.submitClaim(claimId) }
    suspend fun withdrawClaim(claimId: String): Result<Unit> = runCatching { api.withdrawClaim(claimId) }
}
