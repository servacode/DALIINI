package com.servacode.directory.core.network

import com.servacode.directory.core.model.BusinessHour
import com.servacode.directory.core.model.DutyShift
import com.servacode.directory.core.model.FacilityMember
import com.servacode.directory.core.model.FacilityMemberRole
import com.servacode.directory.core.model.OwnerApplication
import com.servacode.directory.core.model.OwnerConfig
import com.servacode.directory.core.model.OwnerEvidence
import com.servacode.directory.core.model.OwnerFacilityDetail
import com.servacode.directory.core.model.OwnerFacilityImage
import com.servacode.directory.core.model.OwnerFacilitySummary
import com.servacode.directory.core.model.TemporaryClosure

data class OwnerFacilityDraftInput(
    val provinceId: String,
    val categoryId: String,
    val nameAr: String,
    val nameEn: String? = null,
)

data class OwnerFacilityPatch(
    val nameAr: String? = null,
    val nameEn: String? = null,
    val descriptionAr: String? = null,
    val descriptionEn: String? = null,
    val phone: String? = null,
    val addressAr: String? = null,
    val addressEn: String? = null,
    val cityId: String? = null,
    val neighborhoodId: String? = null,
    val specialtyIds: List<String>? = null,
    val serviceTagIds: List<String>? = null,
)

data class OwnerUploadPayload(
    val fileName: String,
    val mediaType: String,
    val bytes: ByteArray,
)

data class TemporaryClosureInput(
    val startsAtEpochMillis: Long,
    val endsAtEpochMillis: Long,
    val reason: String? = null,
)

data class DutyShiftInput(
    val startsAtEpochMillis: Long,
    val endsAtEpochMillis: Long,
)

interface OwnerApiBoundary {
    suspend fun ownerConfig(provinceId: String): OwnerConfig
    suspend fun facilities(): List<OwnerFacilitySummary>
    suspend fun createFacility(input: OwnerFacilityDraftInput): OwnerFacilityDetail
    suspend fun facility(id: String): OwnerFacilityDetail
    suspend fun patchFacility(id: String, input: OwnerFacilityPatch): OwnerFacilityDetail
    suspend fun submitFacility(id: String): OwnerApplication
    suspend fun updateLocation(id: String, latitude: Double, longitude: Double): OwnerFacilityDetail
    suspend fun replaceHours(id: String, hours: List<BusinessHour>): List<BusinessHour>
    suspend fun images(id: String): List<OwnerFacilityImage>
    suspend fun uploadImage(id: String, payload: OwnerUploadPayload): OwnerFacilityImage
    suspend fun deleteImage(id: String, imageId: String)
    suspend fun uploadEvidence(
        id: String,
        requirementId: String,
        payload: OwnerUploadPayload,
    ): OwnerEvidence
    suspend fun deleteEvidence(id: String, evidenceId: String)
    suspend fun temporaryClosures(id: String): List<TemporaryClosure>
    suspend fun createTemporaryClosure(
        id: String,
        input: TemporaryClosureInput,
    ): TemporaryClosure
    suspend fun deleteTemporaryClosure(id: String, closureId: String)
    suspend fun members(id: String): List<FacilityMember>
    suspend fun upsertMember(
        id: String,
        userId: String,
        role: FacilityMemberRole,
    ): FacilityMember
    suspend fun deleteMember(id: String, userId: String)
    suspend fun duty(id: String): List<DutyShift>
    suspend fun createDuty(id: String, input: DutyShiftInput): DutyShift
    suspend fun updateDuty(id: String, shiftId: String, input: DutyShiftInput): DutyShift
    suspend fun deleteDuty(id: String, shiftId: String)
}

object UnboundGeneratedOwnerApi : OwnerApiBoundary {
    private fun unavailable(): Nothing = throw GeneratedClientRequiredException()
    override suspend fun ownerConfig(provinceId: String): OwnerConfig = unavailable()
    override suspend fun facilities(): List<OwnerFacilitySummary> = unavailable()
    override suspend fun createFacility(input: OwnerFacilityDraftInput): OwnerFacilityDetail = unavailable()
    override suspend fun facility(id: String): OwnerFacilityDetail = unavailable()
    override suspend fun patchFacility(id: String, input: OwnerFacilityPatch): OwnerFacilityDetail = unavailable()
    override suspend fun submitFacility(id: String): OwnerApplication = unavailable()
    override suspend fun updateLocation(
        id: String,
        latitude: Double,
        longitude: Double,
    ): OwnerFacilityDetail = unavailable()
    override suspend fun replaceHours(id: String, hours: List<BusinessHour>): List<BusinessHour> =
        unavailable()
    override suspend fun images(id: String): List<OwnerFacilityImage> = unavailable()
    override suspend fun uploadImage(id: String, payload: OwnerUploadPayload): OwnerFacilityImage =
        unavailable()
    override suspend fun deleteImage(id: String, imageId: String): Unit = unavailable()
    override suspend fun uploadEvidence(
        id: String,
        requirementId: String,
        payload: OwnerUploadPayload,
    ): OwnerEvidence = unavailable()
    override suspend fun deleteEvidence(id: String, evidenceId: String): Unit = unavailable()
    override suspend fun temporaryClosures(id: String): List<TemporaryClosure> = unavailable()
    override suspend fun createTemporaryClosure(
        id: String,
        input: TemporaryClosureInput,
    ): TemporaryClosure = unavailable()
    override suspend fun deleteTemporaryClosure(id: String, closureId: String): Unit = unavailable()
    override suspend fun members(id: String): List<FacilityMember> = unavailable()
    override suspend fun upsertMember(
        id: String,
        userId: String,
        role: FacilityMemberRole,
    ): FacilityMember = unavailable()
    override suspend fun deleteMember(id: String, userId: String): Unit = unavailable()
    override suspend fun duty(id: String): List<DutyShift> = unavailable()
    override suspend fun createDuty(id: String, input: DutyShiftInput): DutyShift = unavailable()
    override suspend fun updateDuty(
        id: String,
        shiftId: String,
        input: DutyShiftInput,
    ): DutyShift = unavailable()
    override suspend fun deleteDuty(id: String, shiftId: String): Unit = unavailable()
}
