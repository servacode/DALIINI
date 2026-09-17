package com.servacode.directory.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Province(
    val id: String,
    val nameAr: String,
    val nameEn: String? = null,
)

@Serializable
data class Category(
    val id: String,
    val nameAr: String,
    val nameEn: String? = null,
    val iconKey: String? = null,
)

@Serializable
enum class AvailabilityState { OPEN, DUTY, TEMP_CLOSED, CLOSED }

@Serializable
data class FacilitySummary(
    val id: String,
    val nameAr: String,
    val nameEn: String? = null,
    val category: Category,
    val distanceMeters: Double? = null,
    val ratingAverage: Double? = null,
    val ratingCount: Int = 0,
    val availability: AvailabilityState = AvailabilityState.CLOSED,
)

@Serializable
data class FacilityDetail(
    val summary: FacilitySummary,
    val descriptionAr: String? = null,
    val phone: String? = null,
    val addressAr: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val imageUrls: List<String> = emptyList(),
)

@Serializable
data class HomeSnapshot(
    val province: Province,
    val categories: List<Category>,
    val nearby: List<FacilitySummary>,
    val refreshedAtEpochMillis: Long,
)

@Serializable
data class AccountProfile(
    val id: String,
    val name: String,
    val phone: String,
    val provinceId: String? = null,
)

@Serializable
data class UserRating(
    val id: String,
    val facilityId: String,
    val facilityNameAr: String,
    val stars: Int,
    val updatedAtEpochMillis: Long,
)

@Serializable
data class PublicMapFacility(
    val id: String,
    val label: String,
    val latitude: Double,
    val longitude: Double,
    val categoryIconKey: String? = null,
    val availability: AvailabilityState = AvailabilityState.CLOSED,
)

@Serializable
enum class OwnerFacilityStatus {
    DRAFT,
    SUBMITTED,
    ACTIVE,
    REVERIFICATION_REQUIRED,
    SUSPENDED,
    CLOSED,
}

@Serializable
enum class FacilityMemberRole { OWNER, MANAGER }

@Serializable
data class FacilityCapabilities(
    val supportsHours: Boolean,
    val supportsPhotos: Boolean,
    val supportsDuty: Boolean,
    val supportsSpecialtyFilter: Boolean,
    val supportsServiceFilter: Boolean,
    val supportsTemporaryClosure: Boolean,
    val supportsOwnerOnboarding: Boolean,
)

@Serializable
data class VerificationRequirementDescriptor(
    val id: String,
    val labelAr: String,
    val labelEn: String? = null,
    val instructionsAr: String? = null,
    val required: Boolean,
    val minFiles: Int,
    val maxFiles: Int,
)

@Serializable
data class OwnerCategoryConfig(
    val category: Category,
    val specialization: String,
    val capabilities: FacilityCapabilities,
    val verificationRequirements: List<VerificationRequirementDescriptor>,
)

@Serializable
data class OwnerConfig(
    val province: Province,
    val categories: List<OwnerCategoryConfig>,
)

@Serializable
data class OwnerFacilitySummary(
    val id: String,
    val nameAr: String,
    val category: Category,
    val province: Province,
    val status: OwnerFacilityStatus,
    val lastUpdateEpochMillis: Long,
    val requiredAction: String? = null,
)

@Serializable
data class OwnerFacilityImage(
    val id: String,
    val url: String,
    val sortOrder: Int,
    val width: Int,
    val height: Int,
)

@Serializable
data class OwnerEvidence(
    val id: String,
    val requirementId: String,
    val createdAtEpochMillis: Long,
)

@Serializable
data class OwnerApplication(
    val id: String,
    val kind: String,
    val status: String,
    val rejectionReason: String? = null,
    val submittedAtEpochMillis: Long? = null,
)

@Serializable
data class BusinessHour(
    val weekday: Int,
    val opensAt: String,
    val closesAt: String,
    val sortOrder: Int = 0,
)

@Serializable
data class TemporaryClosure(
    val id: String,
    val startsAtEpochMillis: Long,
    val endsAtEpochMillis: Long,
    val reason: String? = null,
)

@Serializable
data class FacilityMember(
    val userId: String,
    val name: String,
    val phone: String,
    val role: FacilityMemberRole,
)

@Serializable
data class OwnerFacilityDetail(
    val summary: OwnerFacilitySummary,
    val nameEn: String? = null,
    val descriptionAr: String? = null,
    val descriptionEn: String? = null,
    val phone: String? = null,
    val addressAr: String? = null,
    val addressEn: String? = null,
    val cityId: String? = null,
    val neighborhoodId: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val specialtyIds: List<String> = emptyList(),
    val serviceTagIds: List<String> = emptyList(),
    val hours: List<BusinessHour> = emptyList(),
    val evidence: List<OwnerEvidence> = emptyList(),
    val application: OwnerApplication? = null,
)

@Serializable
data class DutyShift(
    val id: String,
    val startsAtEpochMillis: Long,
    val endsAtEpochMillis: Long,
)
