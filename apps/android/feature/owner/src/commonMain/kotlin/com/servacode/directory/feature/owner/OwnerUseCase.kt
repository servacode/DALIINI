package com.servacode.directory.feature.owner

import com.servacode.directory.core.inject.Inject
import com.servacode.directory.core.model.CategoryTags
import com.servacode.directory.core.model.FacilityInvitation
import com.servacode.directory.core.model.FacilityMember
import com.servacode.directory.core.model.FacilityMemberRole
import com.servacode.directory.core.model.OwnerFacilityDetail
import com.servacode.directory.core.model.TemporaryClosure
import com.servacode.directory.core.network.OwnerFacilityPatch
import com.servacode.directory.core.network.OwnerUploadPayload
import com.servacode.directory.core.network.TemporaryClosureInput

class LoadOwnerFacilitiesUseCase @Inject constructor(
    private val repository: OwnerRepository,
) {
    suspend operator fun invoke() = repository.facilities()
}

data class ManageFacilityLoad(
    val facility: Result<OwnerFacilityDetail>,
    val closures: Result<List<TemporaryClosure>>,
    val members: Result<List<FacilityMember>>,
    /** A failure here is not the page's: only the facility's owner may see its invitations. */
    val invitations: Result<List<FacilityInvitation>>,
)

class LoadManageFacilityUseCase @Inject constructor(
    private val repository: OwnerRepository,
) {
    suspend operator fun invoke(id: String) = ManageFacilityLoad(
        facility = repository.facility(id),
        closures = repository.closures(id),
        members = repository.members(id),
        invitations = repository.invitations(id),
    )
}

/** How people engaged with one of the owner's facilities over the last 30 days. */
class LoadOwnerInsightsUseCase @Inject constructor(
    private val repository: OwnerRepository,
) {
    suspend operator fun invoke(id: String) = repository.insights(id)
}

/**
 * What an owner may pick for a facility: the specialties and services of its category's entry in
 * its province's owner configuration. Null when the category is not listed there, which is when
 * the province does not take owners for it.
 */
class LoadTagChoicesUseCase @Inject constructor(
    private val repository: OwnerRepository,
) {
    suspend operator fun invoke(provinceId: String, categoryId: String): Result<CategoryTags?> =
        repository.config(provinceId).map { config ->
            config.categories.firstOrNull { it.category.id == categoryId }?.tags
        }
}

/** The weekly «تأكيد أوقات الدوام». */
class ConfirmHoursUseCase @Inject constructor(
    private val repository: OwnerRepository,
) {
    suspend operator fun invoke(id: String) = repository.confirmHours(id)
}

class ManageFacilityUseCase @Inject constructor(
    private val repository: OwnerRepository,
) {
    suspend fun patch(id: String, patch: OwnerFacilityPatch) = repository.patch(id, patch)
    suspend fun createClosure(id: String, input: TemporaryClosureInput) =
        repository.createClosure(id, input)
    suspend fun deleteClosure(id: String, closureId: String) = repository.deleteClosure(id, closureId)
    suspend fun upsertMember(id: String, userId: String, role: FacilityMemberRole) =
        repository.upsertMember(id, userId, role)
    suspend fun deleteMember(id: String, userId: String) = repository.deleteMember(id, userId)
    suspend fun invite(id: String, phone: String) = repository.invite(id, phone, FacilityMemberRole.MANAGER)
    suspend fun revokeInvitation(id: String, invitationId: String) = repository.revokeInvitation(id, invitationId)
}

/**
 * «هذه منشأتي» (DECISION-064): find a published facility nobody owns, start a claim on it, upload
 * the documents its category asks for, and send it; or withdraw it while it is open.
 */
class ClaimFacilityUseCase @Inject constructor(
    private val repository: OwnerRepository,
) {
    suspend fun search(query: String) = repository.claimable(query)
    suspend fun claims() = repository.claims()
    suspend fun claim(claimId: String) = repository.claim(claimId)
    suspend fun start(facilityId: String) = repository.startClaim(facilityId)
    suspend fun upload(claimId: String, requirementId: String, payload: OwnerUploadPayload) =
        repository.uploadClaimEvidence(claimId, requirementId, payload)
    suspend fun deleteEvidence(claimId: String, evidenceId: String) =
        repository.deleteClaimEvidence(claimId, evidenceId)
    suspend fun submit(claimId: String) = repository.submitClaim(claimId)
    suspend fun withdraw(claimId: String) = repository.withdrawClaim(claimId)
}

/** The invitations waiting for the signed-in account (DECISION-064). */
class ReceivedInvitationsUseCase @Inject constructor(
    private val repository: OwnerRepository,
) {
    suspend fun load() = repository.receivedInvitations()
    suspend fun accept(invitationId: String) = repository.acceptInvitation(invitationId)
    suspend fun decline(invitationId: String) = repository.declineInvitation(invitationId)
}
