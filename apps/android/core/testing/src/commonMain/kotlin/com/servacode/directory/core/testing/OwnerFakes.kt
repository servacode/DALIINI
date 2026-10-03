package com.servacode.directory.core.testing

import com.servacode.directory.core.model.HoursConfirmation
import com.servacode.directory.core.model.BusinessHour
import com.servacode.directory.core.model.ClaimEvidence
import com.servacode.directory.core.model.ClaimableFacility
import com.servacode.directory.core.model.FacilityClaim
import com.servacode.directory.core.model.DutyShift
import com.servacode.directory.core.model.FacilityInvitation
import com.servacode.directory.core.model.FacilityMember
import com.servacode.directory.core.model.FacilityMemberRole
import com.servacode.directory.core.model.OwnerConfig
import com.servacode.directory.core.model.OwnerEvidence
import com.servacode.directory.core.model.OwnerFacilityDetail
import com.servacode.directory.core.model.OwnerFacilityImage
import com.servacode.directory.core.model.OwnerFacilityInsights
import com.servacode.directory.core.model.OwnerFacilitySummary
import com.servacode.directory.core.model.OwnerSubmission
import com.servacode.directory.core.model.ReceivedInvitation
import com.servacode.directory.core.model.TemporaryClosure
import com.servacode.directory.core.network.DutyShiftInput
import com.servacode.directory.core.network.OwnerApiBoundary
import com.servacode.directory.core.network.OwnerFacilityDraftInput
import com.servacode.directory.core.network.OwnerFacilityPatch
import com.servacode.directory.core.network.OwnerUploadPayload
import com.servacode.directory.core.network.TemporaryClosureInput

/**
 * An [OwnerApiBoundary] whose answers each test sets, recording what was asked. As with
 * [ScriptedPublicApi], an unset answer fails as if offline.
 */
class ScriptedOwnerApi : OwnerApiBoundary {
    var insightsAnswer: (String) -> OwnerFacilityInsights = { throw offline }
    var patchAnswer: (String, OwnerFacilityPatch) -> OwnerFacilityDetail = { _, _ -> throw offline }
    var confirmAnswer: (String) -> HoursConfirmation = { throw offline }
    var dutyAnswer: (String) -> List<DutyShift> = { throw offline }
    var createDutyAnswer: (String, DutyShiftInput) -> DutyShift = { _, _ -> throw offline }
    var closuresAnswer: (String) -> List<TemporaryClosure> = { throw offline }
    val calls = mutableListOf<String>()

    override suspend fun insights(id: String): OwnerFacilityInsights {
        calls += "insights:$id"
        return insightsAnswer(id)
    }

    override suspend fun patchFacility(id: String, input: OwnerFacilityPatch): OwnerFacilityDetail {
        calls += "patch:$id"
        return patchAnswer(id, input)
    }

    override suspend fun confirmHours(id: String): HoursConfirmation {
        calls += "confirm:$id"
        return confirmAnswer(id)
    }

    var configAnswer: (String) -> OwnerConfig = { throw offline }

    override suspend fun ownerConfig(provinceId: String): OwnerConfig {
        calls += "config:$provinceId"
        return configAnswer(provinceId)
    }

    override suspend fun facilities(): List<OwnerFacilitySummary> = throw offline
    override suspend fun createFacility(input: OwnerFacilityDraftInput): OwnerFacilityDetail = throw offline
    var facilityAnswer: (String) -> OwnerFacilityDetail = { throw offline }
    override suspend fun facility(id: String): OwnerFacilityDetail = facilityAnswer(id)
    override suspend fun submitFacility(id: String): OwnerSubmission = throw offline
    override suspend fun updateLocation(id: String, latitude: Double, longitude: Double): OwnerFacilityDetail =
        throw offline
    override suspend fun replaceHours(id: String, hours: List<BusinessHour>): List<BusinessHour> = throw offline
    override suspend fun images(id: String): List<OwnerFacilityImage> = throw offline
    override suspend fun uploadImage(id: String, payload: OwnerUploadPayload): OwnerFacilityImage = throw offline
    override suspend fun deleteImage(id: String, imageId: String) = throw offline
    override suspend fun uploadEvidence(id: String, requirementId: String, payload: OwnerUploadPayload): OwnerEvidence =
        throw offline
    override suspend fun deleteEvidence(id: String, evidenceId: String) = throw offline
    override suspend fun temporaryClosures(id: String): List<TemporaryClosure> = closuresAnswer(id)
    override suspend fun createTemporaryClosure(id: String, input: TemporaryClosureInput): TemporaryClosure =
        throw offline
    override suspend fun deleteTemporaryClosure(id: String, closureId: String) = throw offline
    var membersAnswer: (String) -> List<FacilityMember> = { throw offline }
    override suspend fun members(id: String): List<FacilityMember> = membersAnswer(id)
    override suspend fun upsertMember(id: String, userId: String, role: FacilityMemberRole): FacilityMember =
        throw offline
    override suspend fun deleteMember(id: String, userId: String) = throw offline
    var invitationsAnswer: (String) -> List<FacilityInvitation> = { throw offline }
    var inviteAnswer: (String, String, FacilityMemberRole) -> FacilityInvitation = { _, _, _ -> throw offline }
    var receivedAnswer: () -> List<ReceivedInvitation> = { throw offline }
    var acceptAnswer: (String) -> String = { throw offline }
    override suspend fun invitations(id: String): List<FacilityInvitation> = invitationsAnswer(id)
    override suspend fun invite(id: String, phone: String, role: FacilityMemberRole): FacilityInvitation {
        calls += "invite:$id:$phone"
        return inviteAnswer(id, phone, role)
    }
    override suspend fun revokeInvitation(id: String, invitationId: String) {
        calls += "revokeInvitation:$id:$invitationId"
    }
    override suspend fun receivedInvitations(): List<ReceivedInvitation> = receivedAnswer()
    override suspend fun acceptInvitation(invitationId: String): String {
        calls += "accept:$invitationId"
        return acceptAnswer(invitationId)
    }
    override suspend fun declineInvitation(invitationId: String) {
        calls += "decline:$invitationId"
    }
    var claimableAnswer: (String) -> List<ClaimableFacility> = { throw offline }
    var claimsAnswer: () -> List<FacilityClaim> = { throw offline }
    var claimAnswer: (String) -> FacilityClaim = { throw offline }
    var startClaimAnswer: (String) -> FacilityClaim = { throw offline }
    var claimEvidenceAnswer: (String, String) -> ClaimEvidence = { _, _ -> throw offline }
    var submitClaimAnswer: (String) -> FacilityClaim = { throw offline }
    override suspend fun claimableFacilities(query: String, provinceId: String?): List<ClaimableFacility> {
        calls += "claimable:$query"
        return claimableAnswer(query)
    }
    override suspend fun claims(): List<FacilityClaim> = claimsAnswer()
    override suspend fun claim(claimId: String): FacilityClaim = claimAnswer(claimId)
    override suspend fun startClaim(facilityId: String): FacilityClaim {
        calls += "startClaim:$facilityId"
        return startClaimAnswer(facilityId)
    }
    override suspend fun uploadClaimEvidence(
        claimId: String,
        requirementId: String,
        payload: OwnerUploadPayload,
    ): ClaimEvidence {
        calls += "claimEvidence:$claimId:$requirementId"
        return claimEvidenceAnswer(claimId, requirementId)
    }
    override suspend fun deleteClaimEvidence(claimId: String, evidenceId: String) {
        calls += "deleteClaimEvidence:$claimId:$evidenceId"
    }
    override suspend fun submitClaim(claimId: String): FacilityClaim {
        calls += "submitClaim:$claimId"
        return submitClaimAnswer(claimId)
    }
    override suspend fun withdrawClaim(claimId: String) {
        calls += "withdrawClaim:$claimId"
    }
    override suspend fun duty(id: String): List<DutyShift> = dutyAnswer(id)
    override suspend fun createDuty(id: String, input: DutyShiftInput): DutyShift {
        calls += "createDuty:$id"
        return createDutyAnswer(id, input)
    }
    override suspend fun updateDuty(id: String, shiftId: String, input: DutyShiftInput): DutyShift = throw offline
    override suspend fun deleteDuty(id: String, shiftId: String) = throw offline
}
