package com.servacode.directory.feature.owner

import com.servacode.directory.core.model.FacilityMemberRole
import com.servacode.directory.core.network.OwnerFacilityPatch
import com.servacode.directory.core.network.TemporaryClosureInput
import javax.inject.Inject

class LoadOwnerFacilitiesUseCase @Inject constructor(
    private val repository: OwnerRepository,
) {
    suspend operator fun invoke() = repository.facilities()
}

class LoadManageFacilityUseCase @Inject constructor(
    private val repository: OwnerRepository,
) {
    suspend operator fun invoke(id: String) = Triple(
        repository.facility(id),
        repository.closures(id),
        repository.members(id),
    )

}

/** How people engaged with one of the owner's facilities over the last 30 days. */
class LoadOwnerInsightsUseCase @Inject constructor(
    private val repository: OwnerRepository,
) {
    suspend operator fun invoke(id: String) = repository.insights(id)
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
}
