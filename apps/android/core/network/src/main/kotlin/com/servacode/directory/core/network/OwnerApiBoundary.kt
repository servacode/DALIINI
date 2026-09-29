package com.servacode.directory.core.network

import com.servacode.directory.core.model.HoursConfirmation
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
    /** Blank clears it; the backend normalises a Syrian mobile to E.164 or refuses it. */
    val whatsapp: String? = null,
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

/**
 * The owner-side operations over the generated client. Whether a province and category accept
 * onboarding, and what evidence they require, is the backend's decision: [ownerConfig]
 * returns only what is open, and creation is refused otherwise.
 */
interface OwnerApiBoundary {
    suspend fun ownerConfig(provinceId: String): OwnerConfig
    suspend fun facilities(): List<OwnerFacilitySummary>
    suspend fun createFacility(input: OwnerFacilityDraftInput): OwnerFacilityDetail
    suspend fun facility(id: String): OwnerFacilityDetail
    suspend fun patchFacility(id: String, input: OwnerFacilityPatch): OwnerFacilityDetail

    /** The owner confirms the facility's opening hours are still right. 409 HOURS_NOT_SUPPORTED otherwise. */
    suspend fun confirmHours(id: String): HoursConfirmation

    /** Views, calls and directions for the facility over the backend's window (30 days). */
    suspend fun insights(id: String): OwnerFacilityInsights
    suspend fun submitFacility(id: String): OwnerSubmission
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
