package com.servacode.directory.feature.owner

import com.servacode.directory.core.model.FacilityMember
import com.servacode.directory.core.model.FacilityMemberRole
import com.servacode.directory.core.model.OwnerFacilityDetail
import com.servacode.directory.core.model.OwnerFacilitySummary
import com.servacode.directory.core.model.TemporaryClosure
import com.servacode.directory.core.network.OwnerApiBoundary
import com.servacode.directory.core.network.OwnerFacilityPatch
import com.servacode.directory.core.network.TemporaryClosureInput
import javax.inject.Inject

class OwnerRepository @Inject constructor(
    private val api: OwnerApiBoundary,
) {
    suspend fun facilities(): Result<List<OwnerFacilitySummary>> = runCatching { api.facilities() }

    suspend fun facility(id: String): Result<OwnerFacilityDetail> = runCatching { api.facility(id) }

    suspend fun patch(id: String, patch: OwnerFacilityPatch): Result<OwnerFacilityDetail> =
        runCatching { api.patchFacility(id, patch) }
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
}
