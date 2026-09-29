package com.servacode.directory.core.network.api

import com.servacode.directory.core.model.HoursConfirmation
import com.servacode.directory.core.model.OwnerFacilityInsights
import com.servacode.directory.api.apis.AvailabilityApi
import com.servacode.directory.api.apis.DutyApi
import com.servacode.directory.api.apis.MediaApi
import com.servacode.directory.api.apis.OwnerApi
import com.servacode.directory.api.models.BusinessHourInput
import com.servacode.directory.api.models.DutyShiftInput as WireDutyShiftInput
import com.servacode.directory.api.models.FacilityCreate
import com.servacode.directory.api.models.FacilityLocation
import com.servacode.directory.api.models.FacilityMember as WireFacilityMember
import com.servacode.directory.api.models.PatchedDutyShiftInput
import com.servacode.directory.api.models.PatchedFacilityPatch
import com.servacode.directory.api.models.TemporaryClosureInput as WireTemporaryClosureInput
import com.servacode.directory.core.model.BusinessHour
import com.servacode.directory.core.model.DutyShift
import com.servacode.directory.core.model.FacilityMember
import com.servacode.directory.core.model.FacilityMemberRole
import com.servacode.directory.core.model.OwnerConfig
import com.servacode.directory.core.model.OwnerEvidence
import com.servacode.directory.core.model.OwnerFacilityDetail
import com.servacode.directory.core.model.OwnerFacilityImage
import com.servacode.directory.core.model.OwnerFacilitySummary
import com.servacode.directory.core.model.OwnerSubmission
import com.servacode.directory.core.model.TemporaryClosure
import com.servacode.directory.core.network.DutyShiftInput
import com.servacode.directory.core.network.OwnerApiBoundary
import com.servacode.directory.core.network.OwnerFacilityDraftInput
import com.servacode.directory.core.network.OwnerFacilityPatch
import com.servacode.directory.core.network.OwnerUploadPayload
import com.servacode.directory.core.network.TemporaryClosureInput
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID

/**
 * [OwnerApiBoundary] over the generated client.
 *
 * Uploads are sent as they were picked; the backend validates type, size and content and its
 * refusal comes back as an ordinary validation error. Nothing here sees a storage key: the
 * responses carry only the public URL of an image and the id of a piece of evidence.
 */
class GeneratedOwnerApi(client: GeneratedClient) : OwnerApiBoundary {
    private val owner by lazy { client.create<OwnerApi>() }
    private val availability by lazy { client.create<AvailabilityApi>() }
    private val duty by lazy { client.create<DutyApi>() }
    private val media by lazy { client.create<MediaApi>() }

    override suspend fun ownerConfig(provinceId: String): OwnerConfig =
        call { owner.ownerConfigRetrieve(provinceId) }.toDomain()

    override suspend fun facilities(): List<OwnerFacilitySummary> =
        call { owner.ownerFacilitiesList() }.items.map { it.toDomain() }

    override suspend fun createFacility(input: OwnerFacilityDraftInput): OwnerFacilityDetail = call {
        owner.ownerFacilityCreate(
            FacilityCreate(
                provinceId = UUID.fromString(input.provinceId),
                categoryId = UUID.fromString(input.categoryId),
                nameAr = input.nameAr,
                nameEn = input.nameEn,
            ),
        )
    }.toDomain()

    override suspend fun facility(id: String): OwnerFacilityDetail =
        call { owner.ownerFacilityRetrieve(UUID.fromString(id)) }.toDomain()

    override suspend fun confirmHours(id: String): HoursConfirmation =
        call { owner.ownerFacilityHoursConfirm(UUID.fromString(id)) }.let {
            HoursConfirmation(it.hoursConfirmedAt.toEpochMillis(), it.infoConfirmedAt.toEpochMillis())
        }

    override suspend fun insights(id: String): OwnerFacilityInsights =
        call { owner.ownerFacilityInsightsRetrieve(UUID.fromString(id)) }.toDomain()

    override suspend fun patchFacility(id: String, input: OwnerFacilityPatch): OwnerFacilityDetail = call {
        owner.ownerFacilityUpdate(
            facilityId = UUID.fromString(id),
            patchedFacilityPatch = PatchedFacilityPatch(
                nameAr = input.nameAr,
                nameEn = input.nameEn,
                descriptionAr = input.descriptionAr,
                descriptionEn = input.descriptionEn,
                phone = input.phone,
                whatsapp = input.whatsapp,
                addressAr = input.addressAr,
                addressEn = input.addressEn,
                cityId = input.cityId?.let(UUID::fromString),
                neighborhoodId = input.neighborhoodId?.let(UUID::fromString),
                // Integer keys on the wire; the domain keeps every id as a String.
                specialtyIds = input.specialtyIds?.map(String::toInt),
                serviceTagIds = input.serviceTagIds?.map(String::toInt),
            ),
        )
    }.toDomain()

    override suspend fun submitFacility(id: String): OwnerSubmission =
        call { owner.ownerFacilitySubmit(UUID.fromString(id)) }.toDomain()

    override suspend fun updateLocation(id: String, latitude: Double, longitude: Double): OwnerFacilityDetail =
        call {
            owner.ownerFacilityLocationReplace(
                facilityId = UUID.fromString(id),
                facilityLocation = FacilityLocation(latitude = latitude, longitude = longitude),
            )
        }.toDomain()

    override suspend fun replaceHours(id: String, hours: List<BusinessHour>): List<BusinessHour> = call {
        availability.ownerFacilityHoursReplace(
            facilityId = UUID.fromString(id),
            businessHourInput = hours.map {
                BusinessHourInput(
                    weekday = it.weekday,
                    opensAt = it.opensAt,
                    closesAt = it.closesAt,
                    sequence = it.sequence,
                )
            },
        )
    }.items.map { it.toDomain() }

    override suspend fun images(id: String): List<OwnerFacilityImage> =
        call { media.ownerFacilityImagesList(UUID.fromString(id)) }.items.map { it.toDomain() }

    override suspend fun uploadImage(id: String, payload: OwnerUploadPayload): OwnerFacilityImage = call {
        media.ownerFacilityImageCreate(facilityId = UUID.fromString(id), file = payload.toPart())
    }.toDomain()

    override suspend fun deleteImage(id: String, imageId: String) {
        callForNoContent {
            media.ownerFacilityImageDelete(facilityId = UUID.fromString(id), imageId = UUID.fromString(imageId))
        }
    }

    override suspend fun uploadEvidence(
        id: String,
        requirementId: String,
        payload: OwnerUploadPayload,
    ): OwnerEvidence = call {
        media.ownerFacilityEvidenceCreate(
            facilityId = UUID.fromString(id),
            // The model's integer key (INT-068); the domain keeps every id opaque.
            requirementId = requirementId.toInt(),
            file = payload.toPart(),
        )
    }.let { OwnerEvidence(id = it.id.toString(), requirementId = it.requirementId.toString()) }

    override suspend fun deleteEvidence(id: String, evidenceId: String) {
        callForNoContent {
            media.ownerFacilityEvidenceDelete(
                evidenceId = UUID.fromString(evidenceId),
                facilityId = UUID.fromString(id),
            )
        }
    }

    override suspend fun temporaryClosures(id: String): List<TemporaryClosure> =
        call { availability.ownerFacilityTemporaryClosuresList(UUID.fromString(id)) }.items.map { it.toDomain() }

    override suspend fun createTemporaryClosure(id: String, input: TemporaryClosureInput): TemporaryClosure =
        call {
            availability.ownerFacilityTemporaryClosureCreate(
                facilityId = UUID.fromString(id),
                temporaryClosureInput = WireTemporaryClosureInput(
                    startsAt = input.startsAtEpochMillis.toOffsetDateTime(),
                    endsAt = input.endsAtEpochMillis.toOffsetDateTime(),
                    reason = input.reason,
                ),
            )
        }.toDomain()

    override suspend fun deleteTemporaryClosure(id: String, closureId: String) {
        callForNoContent {
            availability.ownerFacilityTemporaryClosureCancel(
                closureId = UUID.fromString(closureId),
                facilityId = UUID.fromString(id),
            )
        }
    }

    override suspend fun members(id: String): List<FacilityMember> =
        call { owner.ownerFacilityMembersList(UUID.fromString(id)) }.items.map { it.toDomain() }

    override suspend fun upsertMember(id: String, userId: String, role: FacilityMemberRole): FacilityMember =
        call {
            owner.ownerFacilityMemberUpsert(
                facilityId = UUID.fromString(id),
                facilityMember = WireFacilityMember(userId = UUID.fromString(userId), role = role.toWire()),
            )
        }.let { FacilityMember(userId = it.userId.toString(), name = it.name, role = it.role.toDomain()) }

    override suspend fun deleteMember(id: String, userId: String) {
        callForNoContent {
            owner.ownerFacilityMemberDelete(facilityId = UUID.fromString(id), userId = UUID.fromString(userId))
        }
    }

    override suspend fun duty(id: String): List<DutyShift> =
        call { duty.ownerFacilityDutyList(UUID.fromString(id)) }.items.map { it.toDomain() }

    override suspend fun createDuty(id: String, input: DutyShiftInput): DutyShift = call {
        duty.ownerFacilityDutyCreate(
            facilityId = UUID.fromString(id),
            dutyShiftInput = WireDutyShiftInput(
                startsAt = input.startsAtEpochMillis.toOffsetDateTime(),
                endsAt = input.endsAtEpochMillis.toOffsetDateTime(),
            ),
        )
    }.toDomain()

    override suspend fun updateDuty(id: String, shiftId: String, input: DutyShiftInput): DutyShift = call {
        duty.ownerFacilityDutyUpdate(
            facilityId = UUID.fromString(id),
            shiftId = UUID.fromString(shiftId),
            patchedDutyShiftInput = PatchedDutyShiftInput(
                startsAt = input.startsAtEpochMillis.toOffsetDateTime(),
                endsAt = input.endsAtEpochMillis.toOffsetDateTime(),
            ),
        )
    }.toDomain()

    override suspend fun deleteDuty(id: String, shiftId: String) {
        callForNoContent {
            duty.ownerFacilityDutyDelete(facilityId = UUID.fromString(id), shiftId = UUID.fromString(shiftId))
        }
    }
}

private fun OwnerUploadPayload.toPart(): MultipartBody.Part = MultipartBody.Part.createFormData(
    name = "file",
    filename = fileName,
    body = bytes.toRequestBody(mediaType.toMediaTypeOrNull()),
)
