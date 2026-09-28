package com.servacode.directory.core.testing

import com.servacode.directory.core.model.BusinessHour
import com.servacode.directory.core.model.DutyShift
import com.servacode.directory.core.model.FacilityMember
import com.servacode.directory.core.model.FacilityMemberRole
import com.servacode.directory.core.model.OwnerConfig
import com.servacode.directory.core.model.OwnerEvidence
import com.servacode.directory.core.model.OwnerFacilityDetail
import com.servacode.directory.core.model.OwnerFacilityImage
import com.servacode.directory.core.model.OwnerFacilityInsights
import com.servacode.directory.core.model.OwnerFacilitySummary
import com.servacode.directory.core.model.OwnerSubmission
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
    val calls = mutableListOf<String>()

    override suspend fun insights(id: String): OwnerFacilityInsights {
        calls += "insights:$id"
        return insightsAnswer(id)
    }

    override suspend fun patchFacility(id: String, input: OwnerFacilityPatch): OwnerFacilityDetail {
        calls += "patch:$id"
        return patchAnswer(id, input)
    }

    override suspend fun ownerConfig(provinceId: String): OwnerConfig = throw offline
    override suspend fun facilities(): List<OwnerFacilitySummary> = throw offline
    override suspend fun createFacility(input: OwnerFacilityDraftInput): OwnerFacilityDetail = throw offline
    override suspend fun facility(id: String): OwnerFacilityDetail = throw offline
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
    override suspend fun temporaryClosures(id: String): List<TemporaryClosure> = throw offline
    override suspend fun createTemporaryClosure(id: String, input: TemporaryClosureInput): TemporaryClosure =
        throw offline
    override suspend fun deleteTemporaryClosure(id: String, closureId: String) = throw offline
    override suspend fun members(id: String): List<FacilityMember> = throw offline
    override suspend fun upsertMember(id: String, userId: String, role: FacilityMemberRole): FacilityMember =
        throw offline
    override suspend fun deleteMember(id: String, userId: String) = throw offline
    override suspend fun duty(id: String): List<DutyShift> = throw offline
    override suspend fun createDuty(id: String, input: DutyShiftInput): DutyShift = throw offline
    override suspend fun updateDuty(id: String, shiftId: String, input: DutyShiftInput): DutyShift = throw offline
    override suspend fun deleteDuty(id: String, shiftId: String) = throw offline
}
