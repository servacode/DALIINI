package com.servacode.directory.core.transport

import com.servacode.directory.api.multiplatform.apis.AccountApi
import com.servacode.directory.api.multiplatform.apis.AvailabilityApi
import com.servacode.directory.api.multiplatform.apis.DutyApi
import com.servacode.directory.api.multiplatform.apis.MediaApi
import com.servacode.directory.api.multiplatform.apis.OwnerApi
import com.servacode.directory.api.multiplatform.models.BusinessHourInput
import com.servacode.directory.api.multiplatform.models.ClaimStart
import com.servacode.directory.api.multiplatform.models.DutyShiftInput as WireDutyShiftInput
import com.servacode.directory.api.multiplatform.models.FacilityCreate
import com.servacode.directory.api.multiplatform.models.FacilityLocation
import com.servacode.directory.api.multiplatform.models.FacilityMember as WireFacilityMember
import com.servacode.directory.api.multiplatform.models.InvitationRequest
import com.servacode.directory.api.multiplatform.models.PatchedDutyShiftInput
import com.servacode.directory.api.multiplatform.models.PatchedFacilityPatch
import com.servacode.directory.api.multiplatform.models.TemporaryClosureInput as WireTemporaryClosureInput
import com.servacode.directory.core.model.BusinessHour
import com.servacode.directory.core.model.ClaimEvidence
import com.servacode.directory.core.model.ClaimableFacility
import com.servacode.directory.core.model.DutyShift
import com.servacode.directory.core.model.FacilityClaim
import com.servacode.directory.core.model.FacilityInvitation
import com.servacode.directory.core.model.FacilityMember
import com.servacode.directory.core.model.FacilityMemberRole
import com.servacode.directory.core.model.HoursConfirmation
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
import kotlinx.datetime.LocalTime
import kotlin.time.Instant

/**
 * [OwnerApiBoundary] over the multiplatform client: the port of Android's `GeneratedOwnerApi`,
 * request for request, all of it on the signed-in client.
 *
 * Uploads are sent as they were picked; the backend validates type, size and content and its
 * refusal comes back as an ordinary validation error. Nothing here sees a storage key: the
 * responses carry only the public URL of an image and the id of a piece of evidence.
 */
class KtorOwnerApi(clients: TransportClients) : OwnerApiBoundary {
    private val owner by lazy { OwnerApi(clients.baseUrl, clients.authorized) }

    // The invitee's side of an invitation lives under account/, on the same signed-in client.
    private val account by lazy { AccountApi(clients.baseUrl, clients.authorized) }
    private val availability by lazy { AvailabilityApi(clients.baseUrl, clients.authorized) }
    private val duty by lazy { DutyApi(clients.baseUrl, clients.authorized) }
    private val media by lazy { MediaApi(clients.baseUrl, clients.authorized) }

    override suspend fun ownerConfig(provinceId: String): OwnerConfig =
        call { owner.ownerConfigRetrieve(provinceId) }.toDomain()

    override suspend fun facilities(): List<OwnerFacilitySummary> =
        call { owner.ownerFacilitiesList() }.items.map { it.toDomain() }

    override suspend fun createFacility(input: OwnerFacilityDraftInput): OwnerFacilityDetail = call {
        owner.ownerFacilityCreate(
            FacilityCreate(
                provinceId = uuid(input.provinceId),
                categoryId = uuid(input.categoryId),
                nameAr = input.nameAr,
                nameEn = input.nameEn,
            ),
        )
    }.toDomain()

    override suspend fun facility(id: String): OwnerFacilityDetail =
        call { owner.ownerFacilityRetrieve(uuid(id)) }.toDomain()

    override suspend fun confirmHours(id: String): HoursConfirmation =
        call { owner.ownerFacilityHoursConfirm(uuid(id)) }.let {
            HoursConfirmation(it.hoursConfirmedAt.toEpochMilliseconds(), it.infoConfirmedAt.toEpochMilliseconds())
        }

    override suspend fun insights(id: String): OwnerFacilityInsights =
        call { owner.ownerFacilityInsightsRetrieve(uuid(id)) }.toDomain()

    override suspend fun patchFacility(id: String, input: OwnerFacilityPatch): OwnerFacilityDetail = call {
        owner.ownerFacilityUpdate(
            facilityId = uuid(id),
            patchedFacilityPatch = PatchedFacilityPatch(
                nameAr = input.nameAr,
                nameEn = input.nameEn,
                descriptionAr = input.descriptionAr,
                descriptionEn = input.descriptionEn,
                phone = input.phone,
                whatsapp = input.whatsapp,
                addressAr = input.addressAr,
                addressEn = input.addressEn,
                cityId = input.cityId?.let(::uuid),
                neighborhoodId = input.neighborhoodId?.let(::uuid),
                // Integer keys on the wire; the domain keeps every id as a String.
                specialtyIds = input.specialtyIds?.map(String::toInt),
                serviceTagIds = input.serviceTagIds?.map(String::toInt),
            ),
        )
    }.toDomain()

    override suspend fun submitFacility(id: String): OwnerSubmission =
        call { owner.ownerFacilitySubmit(uuid(id)) }.toDomain()

    override suspend fun updateLocation(id: String, latitude: Double, longitude: Double): OwnerFacilityDetail =
        call {
            owner.ownerFacilityLocationReplace(
                facilityId = uuid(id),
                facilityLocation = FacilityLocation(latitude = latitude, longitude = longitude),
            )
        }.toDomain()

    /**
     * The JVM client sent each time as the domain's text; this one takes a [LocalTime], so the
     * text is read as one here, inside `call {}`: text that is not a time of day fails as
     * UNEXPECTED before the request instead of coming back as the backend's VALIDATION_ERROR.
     */
    override suspend fun replaceHours(id: String, hours: List<BusinessHour>): List<BusinessHour> = call {
        availability.ownerFacilityHoursReplace(
            facilityId = uuid(id),
            businessHourInput = hours.map {
                BusinessHourInput(
                    weekday = it.weekday,
                    opensAt = LocalTime.parse(it.opensAt),
                    closesAt = LocalTime.parse(it.closesAt),
                    sequence = it.sequence,
                )
            },
        )
    }.items.map { it.toDomain() }

    override suspend fun images(id: String): List<OwnerFacilityImage> =
        call { media.ownerFacilityImagesList(uuid(id)) }.items.map { it.toDomain() }

    override suspend fun uploadImage(id: String, payload: OwnerUploadPayload): OwnerFacilityImage = call {
        media.ownerFacilityImageCreate(facilityId = uuid(id), file = payload.toFilePart())
    }.toDomain()

    override suspend fun deleteImage(id: String, imageId: String) {
        callForNoContent {
            media.ownerFacilityImageDelete(facilityId = uuid(id), imageId = uuid(imageId))
        }
    }

    override suspend fun uploadEvidence(
        id: String,
        requirementId: String,
        payload: OwnerUploadPayload,
    ): OwnerEvidence = call {
        media.ownerFacilityEvidenceCreate(
            facilityId = uuid(id),
            // The model's integer key (INT-068); the domain keeps every id opaque.
            requirementId = requirementId.toInt(),
            file = payload.toFilePart(),
        )
    }.let { OwnerEvidence(id = it.id, requirementId = it.requirementId.toString()) }

    override suspend fun deleteEvidence(id: String, evidenceId: String) {
        callForNoContent {
            media.ownerFacilityEvidenceDelete(
                evidenceId = uuid(evidenceId),
                facilityId = uuid(id),
            )
        }
    }

    override suspend fun temporaryClosures(id: String): List<TemporaryClosure> =
        call { availability.ownerFacilityTemporaryClosuresList(uuid(id)) }.items.map { it.toDomain() }

    override suspend fun createTemporaryClosure(id: String, input: TemporaryClosureInput): TemporaryClosure =
        call {
            availability.ownerFacilityTemporaryClosureCreate(
                facilityId = uuid(id),
                temporaryClosureInput = WireTemporaryClosureInput(
                    startsAt = Instant.fromEpochMilliseconds(input.startsAtEpochMillis),
                    endsAt = Instant.fromEpochMilliseconds(input.endsAtEpochMillis),
                    reason = input.reason,
                ),
            )
        }.toDomain()

    override suspend fun deleteTemporaryClosure(id: String, closureId: String) {
        callForNoContent {
            availability.ownerFacilityTemporaryClosureCancel(
                closureId = uuid(closureId),
                facilityId = uuid(id),
            )
        }
    }

    override suspend fun members(id: String): List<FacilityMember> =
        call { owner.ownerFacilityMembersList(uuid(id)) }.items.map { it.toDomain() }

    override suspend fun upsertMember(id: String, userId: String, role: FacilityMemberRole): FacilityMember =
        call {
            owner.ownerFacilityMemberUpsert(
                facilityId = uuid(id),
                facilityMember = WireFacilityMember(userId = uuid(userId), role = role.toWire()),
            )
        }.let { FacilityMember(userId = it.userId, name = it.name, role = it.role.toDomain()) }

    override suspend fun deleteMember(id: String, userId: String) {
        callForNoContent {
            owner.ownerFacilityMemberDelete(facilityId = uuid(id), userId = uuid(userId))
        }
    }

    override suspend fun invitations(id: String): List<FacilityInvitation> =
        call { owner.ownerFacilityInvitationsList(uuid(id)) }.items.map { it.toDomain() }

    override suspend fun invite(id: String, phone: String, role: FacilityMemberRole): FacilityInvitation =
        call {
            owner.ownerFacilityInvitationCreate(
                facilityId = uuid(id),
                invitationRequest = InvitationRequest(phone = phone.trim(), role = role.toWire()),
            )
        }.toDomain()

    override suspend fun revokeInvitation(id: String, invitationId: String) {
        callForNoContent {
            owner.ownerFacilityInvitationRevoke(
                facilityId = uuid(id),
                invitationId = uuid(invitationId),
            )
        }
    }

    override suspend fun receivedInvitations(): List<ReceivedInvitation> =
        call { account.accountInvitationsList() }.items.map { it.toDomain() }

    override suspend fun acceptInvitation(invitationId: String): String =
        call { account.accountInvitationAccept(uuid(invitationId)) }.facilityId

    override suspend fun declineInvitation(invitationId: String) {
        callForNoContent { account.accountInvitationDecline(uuid(invitationId)) }
    }

    override suspend fun claimableFacilities(query: String, provinceId: String?): List<ClaimableFacility> =
        call { owner.ownerClaimableFacilitiesList(q = query, provinceId = provinceId) }.items.map { it.toDomain() }

    override suspend fun claims(): List<FacilityClaim> = call { owner.ownerClaimsList() }.items.map { it.toDomain() }

    override suspend fun claim(claimId: String): FacilityClaim =
        call { owner.ownerClaimRetrieve(uuid(claimId)) }.toDomain()

    override suspend fun startClaim(facilityId: String): FacilityClaim =
        call { owner.ownerClaimStart(ClaimStart(uuid(facilityId))) }.toDomain()

    override suspend fun uploadClaimEvidence(
        claimId: String,
        requirementId: String,
        payload: OwnerUploadPayload,
    ): ClaimEvidence = call {
        media.ownerClaimEvidenceCreate(
            claimId = uuid(claimId),
            requirementId = requirementId.toInt(),
            file = payload.toFilePart(),
        )
    }.toDomain()

    override suspend fun deleteClaimEvidence(claimId: String, evidenceId: String) {
        callForNoContent { media.ownerClaimEvidenceDelete(uuid(claimId), uuid(evidenceId)) }
    }

    override suspend fun submitClaim(claimId: String): FacilityClaim =
        call { owner.ownerClaimSubmit(uuid(claimId)) }.toDomain()

    override suspend fun withdrawClaim(claimId: String) {
        callForNoContent { owner.ownerClaimWithdraw(uuid(claimId)) }
    }

    override suspend fun duty(id: String): List<DutyShift> =
        call { duty.ownerFacilityDutyList(uuid(id)) }.items.map { it.toDomain() }

    override suspend fun createDuty(id: String, input: DutyShiftInput): DutyShift = call {
        duty.ownerFacilityDutyCreate(
            facilityId = uuid(id),
            dutyShiftInput = WireDutyShiftInput(
                startsAt = Instant.fromEpochMilliseconds(input.startsAtEpochMillis),
                endsAt = Instant.fromEpochMilliseconds(input.endsAtEpochMillis),
            ),
        )
    }.toDomain()

    override suspend fun updateDuty(id: String, shiftId: String, input: DutyShiftInput): DutyShift = call {
        duty.ownerFacilityDutyUpdate(
            facilityId = uuid(id),
            shiftId = uuid(shiftId),
            patchedDutyShiftInput = PatchedDutyShiftInput(
                startsAt = Instant.fromEpochMilliseconds(input.startsAtEpochMillis),
                endsAt = Instant.fromEpochMilliseconds(input.endsAtEpochMillis),
            ),
        )
    }.toDomain()

    override suspend fun deleteDuty(id: String, shiftId: String) {
        callForNoContent {
            duty.ownerFacilityDutyDelete(facilityId = uuid(id), shiftId = uuid(shiftId))
        }
    }
}
