package com.servacode.directory.core.model

import kotlinx.serialization.Serializable

/** A WGS84 position in decimal degrees, as the backend's `Coordinates`. */
@Serializable
data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
)

@Serializable
data class Province(
    val id: String,
    val nameAr: String,
    val nameEn: String? = null,
    /** Where a map opens for the province when the user's position is unknown; null when unset. */
    val mapCenter: GeoPoint? = null,
)

@Serializable
data class Category(
    val id: String,
    val nameAr: String,
    val nameEn: String? = null,
    val iconKey: String? = null,
    /** Null where the backend sends only a reference to the category, as in facility rows. */
    val capabilities: FacilityCapabilities? = null,
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
    /** When a closed facility next opens, as the backend computed it. */
    val nextOpenAtEpochMillis: Long? = null,
    val cityNameAr: String? = null,
    /** Whether the signed-in account saved this facility; false for anyone not signed in. */
    val isFavorite: Boolean = false,
    /**
     * Whether the doors are open at this moment.
     *
     * Read this rather than [availability], which is one value that lets DUTY outrank OPEN and
     * so cannot say that a pharmacy is both on tonight's roster and serving customers now.
     */
    val isOpenNow: Boolean = false,
    /**
     * Whether the facility is on today's duty roster, today being the local day in Damascus.
     *
     * Independent of [isOpenNow]: a pharmacy on tonight's roster is on duty today from
     * midnight, hours before it opens. False means only that it is not on the roster, and the
     * interface says nothing at all in that case.
     */
    val isOnDutyToday: Boolean = false,
    /** The owner's first photograph, or null when the facility has none. */
    val imageUrl: String? = null,
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
    val neighborhoodNameAr: String? = null,
    /** Ordered by weekday, then by the wire's `sequence`. */
    val hours: List<BusinessHour> = emptyList(),
    val specialties: List<String> = emptyList(),
    val services: List<String> = emptyList(),
)

@Serializable
data class HomeAd(
    val id: String,
    val imageUrl: String,
    val titleAr: String? = null,
    val subtitleAr: String? = null,
    val slideDurationMs: Int = 5000,
    /**
     * The facility this advertisement is about, when it is about one.
     *
     * The backend restricts what an advertisement may carry; of those, a facility is the only
     * destination this app follows, because it is the only one that stays inside the app and
     * means something the user already understands.
     */
    val facilityId: String? = null,
)

@Serializable
data class HomeSnapshot(
    val province: Province,
    val categories: List<Category>,
    val nearby: List<FacilitySummary>,
    val refreshedAtEpochMillis: Long,
    val openNearby: List<FacilitySummary> = emptyList(),
    val dutyNow: List<FacilitySummary> = emptyList(),
    val ads: List<HomeAd> = emptyList(),
)

/**
 * One page of a cursor-paginated list.
 *
 * `nextCursor` is opaque: it is only ever handed back to the backend unchanged, never parsed
 * or built on the device.
 */
data class Page<T>(
    val items: List<T>,
    val nextCursor: String?,
    val hasMore: Boolean,
)

@Serializable
data class AccountProfile(
    val id: String,
    val name: String,
    val phone: String,
    val provinceId: String? = null,
    /** Free text, as the person wrote it. Empty when they have given none. */
    val address: String = "",
    /** The picture on the account, or null when there is none. */
    val imageUrl: String? = null,
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
    val supportsRatings: Boolean = false,
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
    /** The category's capabilities, as the backend serves them with the facility. */
    val capabilities: FacilityCapabilities,
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
    /** Absent in the upload response; present when read back with the facility. */
    val createdAtEpochMillis: Long? = null,
)

/** What the backend answers when a facility is submitted for review. */
data class OwnerSubmission(
    val applicationId: String,
    val status: String,
    val submittedAtEpochMillis: Long,
)

@Serializable
data class OwnerApplication(
    val id: String,
    val kind: String,
    val status: String,
    val rejectionReason: String? = null,
    val submittedAtEpochMillis: Long? = null,
)

/**
 * One opening span. A day with several spans has one row per span, ordered by `sequence`;
 * `closesAt` earlier than `opensAt` is a span that runs past midnight. Whether a facility is
 * open now is never derived from these rows on the device: the backend says so.
 */
@Serializable
data class BusinessHour(
    val weekday: Int,
    val opensAt: String,
    val closesAt: String,
    val sequence: Int = 0,
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
    /** Absent in the upsert response; present when members are listed. */
    val phone: String? = null,
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

/** A challenge the backend sent a one-time code for. */
data class AuthChallenge(
    val id: String,
    val expiresAtEpochMillis: Long,
)

data class AccountSession(
    val id: String,
    val platform: String,
    val deviceName: String,
    val createdAtEpochMillis: Long,
    val lastSeenAtEpochMillis: Long?,
    val revoked: Boolean,
)


/**
 * Where the app decided the user is, and how sure it is of that.
 *
 * The platform resolves a coordinate against its own city and neighbourhood boundaries, so the
 * name shown is the same name the lists and filters are scoped by.
 */
@Serializable
data class ResolvedPlace(
    val province: Province?,
    val cityNameAr: String? = null,
    val neighborhoodNameAr: String? = null,
    /** What to show the user, already assembled: "الرقة" or "الرقة — المشلب". */
    val label: String? = null,
    val resolvedBy: PlaceResolution = PlaceResolution.NONE,
)

@Serializable
enum class PlaceResolution {
    /** The point fell inside a seeded city or neighbourhood. */
    BOUNDARY,

    /** Only the closest province centre could be used. */
    NEAREST_PROVINCE,

    /** The point is outside every province the platform serves. */
    NONE,
}

/** One message in the account's own inbox. */
@Serializable
data class InboxMessage(
    val id: String,
    val type: String,
    val titleAr: String,
    val bodyAr: String,
    val destination: MessageDestination,
    val facilityId: String? = null,
    val isRead: Boolean,
    val createdAtEpochMillis: Long,
)

@Serializable
enum class MessageDestination { NONE, FACILITY, OWNER_FACILITIES }

/** A page of the inbox, with the unread total for the whole of it. */
data class InboxPage(
    val items: List<InboxMessage>,
    val nextCursor: String?,
    val hasMore: Boolean,
    val unreadCount: Int,
)

/** One of the platform's own published pages. */
@Serializable
data class LegalPage(
    val key: LegalPageKey,
    val titleAr: String,
    val version: Int,
    val bodyAr: String? = null,
)

@Serializable
enum class LegalPageKey { ABOUT, PRIVACY, TERMS, INSTRUCTIONS, FAQ, CONTACT }
