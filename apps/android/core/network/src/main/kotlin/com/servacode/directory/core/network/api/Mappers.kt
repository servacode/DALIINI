package com.servacode.directory.core.network.api

import com.servacode.directory.api.models.FacilityReportReasonEnum
import com.servacode.directory.api.models.OwnerFacilityInsights as WireOwnerFacilityInsights
import com.servacode.directory.core.model.FacilityReportReason
import com.servacode.directory.core.model.OwnerFacilityInsights
import com.servacode.directory.api.models.AccountRating
import com.servacode.directory.api.models.AdvertisementAction
import com.servacode.directory.api.models.AdvertisementActionTypeEnum
import com.servacode.directory.api.models.AvailabilityStateEnum
import com.servacode.directory.api.models.BilingualRef
import com.servacode.directory.api.models.BusinessHour as WireBusinessHour
import com.servacode.directory.api.models.CategoryCapabilities
import com.servacode.directory.api.models.ChallengeAccepted
import com.servacode.directory.api.models.CompactFacility
import com.servacode.directory.api.models.DestinationEnum
import com.servacode.directory.api.models.FavoriteFacility
import com.servacode.directory.api.models.FavoriteList
import com.servacode.directory.api.models.KeyEnum
import com.servacode.directory.api.models.LegalDocument
import com.servacode.directory.api.models.LegalDocumentSummary
import com.servacode.directory.api.models.Notification
import com.servacode.directory.api.models.NotificationPage
import com.servacode.directory.api.models.PublicLocationResolve
import com.servacode.directory.api.models.ResolvedByEnum
import com.servacode.directory.api.models.Coordinates
import com.servacode.directory.api.models.DutyShift as WireDutyShift
import com.servacode.directory.api.models.FacilityCursorPage
import com.servacode.directory.api.models.FacilityMemberRoleEnum
import com.servacode.directory.api.models.FacilityStatusEnum
import com.servacode.directory.api.models.HomeCategory
import com.servacode.directory.api.models.MapMarker
import com.servacode.directory.api.models.NamedRef
import com.servacode.directory.api.models.OwnerApplication as WireOwnerApplication
import com.servacode.directory.api.models.OwnerConfig as WireOwnerConfig
import com.servacode.directory.api.models.OwnerConfigProvince
import com.servacode.directory.api.models.OwnerEvidenceRef
import com.servacode.directory.api.models.OwnerFacilityDetail as WireOwnerFacilityDetail
import com.servacode.directory.api.models.OwnerFacilityImage as WireOwnerFacilityImage
import com.servacode.directory.api.models.OwnerFacilitySummary as WireOwnerFacilitySummary
import com.servacode.directory.api.models.OwnerHoursEntry
import com.servacode.directory.api.models.OwnerMember
import com.servacode.directory.api.models.OwnerSubmitResult
import com.servacode.directory.api.models.OwnerVerificationRequirement
import com.servacode.directory.api.models.Profile
import com.servacode.directory.api.models.PublicAdvertisement
import com.servacode.directory.api.models.PublicCategory
import com.servacode.directory.api.models.PublicFacilityDetail
import com.servacode.directory.api.models.PublicHome
import com.servacode.directory.api.models.PublicHoursEntry
import com.servacode.directory.api.models.PublicProvince
import com.servacode.directory.api.models.SessionCredentials
import com.servacode.directory.api.models.TemporaryClosure as WireTemporaryClosure
import com.servacode.directory.api.models.UserSession
import com.servacode.directory.core.auth.SessionTokens
import com.servacode.directory.core.model.AccountProfile
import com.servacode.directory.core.model.AdAction
import com.servacode.directory.core.model.AccountSession
import com.servacode.directory.core.model.AuthChallenge
import com.servacode.directory.core.model.AvailabilityState
import com.servacode.directory.core.model.BusinessHour
import com.servacode.directory.core.model.Category
import com.servacode.directory.core.model.DutyShift
import com.servacode.directory.core.model.FacilityCapabilities
import com.servacode.directory.core.model.FacilityDetail
import com.servacode.directory.core.model.FacilityMember
import com.servacode.directory.core.model.FacilityMemberRole
import com.servacode.directory.core.model.FacilitySummary
import com.servacode.directory.core.model.GeoPoint
import com.servacode.directory.core.model.HomeAd
import com.servacode.directory.core.model.HomeSnapshot
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import com.servacode.directory.core.model.InboxMessage
import com.servacode.directory.core.model.InboxPage
import com.servacode.directory.core.model.LegalPage
import com.servacode.directory.core.model.LegalPageKey
import com.servacode.directory.core.model.MessageDestination
import com.servacode.directory.core.model.PlaceResolution
import com.servacode.directory.core.model.ResolvedPlace
import com.servacode.directory.core.model.OwnerApplication
import com.servacode.directory.core.model.OwnerCategoryConfig
import com.servacode.directory.core.model.OwnerConfig
import com.servacode.directory.core.model.OwnerEvidence
import com.servacode.directory.core.model.OwnerFacilityDetail
import com.servacode.directory.core.model.OwnerFacilityImage
import com.servacode.directory.core.model.OwnerFacilityStatus
import com.servacode.directory.core.model.OwnerFacilitySummary
import com.servacode.directory.core.model.OwnerSubmission
import com.servacode.directory.core.model.Page
import com.servacode.directory.core.model.Province
import com.servacode.directory.core.model.PublicMapFacility
import com.servacode.directory.core.model.TemporaryClosure
import com.servacode.directory.core.model.UserRating
import com.servacode.directory.core.model.VerificationRequirementDescriptor
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

/*
 * Every generated-to-domain conversion lives in this file, and nowhere else.
 *
 * Generated models never reach a repository or a screen. Enums are mapped with exhaustive
 * `when`s over the generated types, so a value added to the contract fails the build here
 * instead of being compared as a string somewhere in the UI. A value the backend sends that
 * the client was not generated for fails decoding, which the adapter reports as an
 * UNEXPECTED error rather than showing a wrong state.
 *
 * Time crosses the boundary here too: wire timestamps become epoch milliseconds, and the
 * domain never does calendar arithmetic of its own. Whether a facility is open or on duty
 * always comes from the backend.
 */

// Time

internal fun OffsetDateTime.toEpochMillis(): Long = toInstant().toEpochMilli()

internal fun Long.toOffsetDateTime(): OffsetDateTime =
    OffsetDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneOffset.UTC)

// Taxonomy and places

internal fun Coordinates.toGeoPoint() = GeoPoint(latitude = latitude, longitude = longitude)

internal fun PublicProvince.toDomain() = Province(
    id = id.toString(),
    nameAr = nameAr,
    nameEn = nameEn,
    mapCenter = mapCenter?.toGeoPoint(),
)

internal fun OwnerConfigProvince.toDomain() = Province(
    id = id.toString(),
    nameAr = nameAr,
    mapCenter = mapCenter?.toGeoPoint(),
)

internal fun NamedRef.toProvince() = Province(id = id.toString(), nameAr = nameAr)

internal fun NamedRef.toCategory() = Category(id = id.toString(), nameAr = nameAr)

internal fun BilingualRef.toCategory() = Category(id = id.toString(), nameAr = nameAr, nameEn = nameEn)

internal fun CategoryCapabilities.toDomain() = FacilityCapabilities(
    supportsHours = hours,
    supportsPhotos = photos,
    supportsDuty = duty,
    supportsSpecialtyFilter = specialtyFilter,
    supportsServiceFilter = serviceFilter,
    supportsTemporaryClosure = temporaryClosure,
    supportsOwnerOnboarding = ownerOnboarding,
    supportsRatings = ratings,
)

internal fun PublicCategory.toDomain() = Category(
    id = id.toString(),
    nameAr = nameAr,
    nameEn = nameEn,
    iconKey = iconKey,
    capabilities = capabilities.toDomain(),
)

/** The home payload carries the capabilities a home screen needs, not the owner-side ones. */
internal fun HomeCategory.toDomain() = Category(
    id = id.toString(),
    nameAr = nameAr,
    nameEn = nameEn,
    iconKey = iconKey,
    capabilities = FacilityCapabilities(
        supportsHours = capabilities.hours,
        supportsPhotos = false,
        supportsDuty = capabilities.duty,
        supportsSpecialtyFilter = capabilities.specialtyFilter,
        supportsServiceFilter = capabilities.serviceFilter,
        supportsTemporaryClosure = false,
        supportsOwnerOnboarding = false,
        supportsRatings = capabilities.ratings,
    ),
)

// Facilities

internal fun AvailabilityStateEnum.toDomain(): AvailabilityState = when (this) {
    AvailabilityStateEnum.OPEN -> AvailabilityState.OPEN
    AvailabilityStateEnum.DUTY -> AvailabilityState.DUTY
    AvailabilityStateEnum.TEMP_CLOSED -> AvailabilityState.TEMP_CLOSED
    AvailabilityStateEnum.CLOSED -> AvailabilityState.CLOSED
}

internal fun CompactFacility.toDomain() = FacilitySummary(
    id = id.toString(),
    nameAr = nameAr,
    nameEn = nameEn,
    category = category.toCategory(),
    distanceMeters = distanceMeters,
    ratingAverage = ratingAverage,
    ratingCount = ratingCount,
    availability = availability.state.toDomain(),
    nextOpenAtEpochMillis = availability.nextOpenAt?.toEpochMillis(),
    cityNameAr = city?.nameAr,
    isFavorite = isFavorite,
    isOpenNow = availability.isOpenNow,
    isOnDutyToday = availability.isOnDutyToday,
    imageUrl = imageUrl?.toString(),
)

internal fun FavoriteFacility.toDomain() = FacilitySummary(
    id = id.toString(),
    nameAr = nameAr,
    nameEn = nameEn,
    category = category.toCategory(),
    distanceMeters = distanceMeters,
    ratingAverage = ratingAverage,
    ratingCount = ratingCount,
    availability = availability.state.toDomain(),
    nextOpenAtEpochMillis = availability.nextOpenAt?.toEpochMillis(),
    cityNameAr = city?.nameAr,
    // Everything in this list is saved by definition, whatever the row says.
    isFavorite = true,
    isOpenNow = availability.isOpenNow,
    isOnDutyToday = availability.isOnDutyToday,
    imageUrl = imageUrl?.toString(),
)

internal fun FavoriteList.toDomain() = Page(
    items = items.map { it.toDomain() },
    nextCursor = nextCursor,
    hasMore = hasMore,
)

internal fun PublicLocationResolve.toDomain() = ResolvedPlace(
    province = province?.toDomain(),
    cityNameAr = city?.nameAr,
    neighborhoodNameAr = neighborhood?.nameAr,
    label = label,
    resolvedBy = when (resolvedBy) {
        ResolvedByEnum.BOUNDARY -> PlaceResolution.BOUNDARY
        ResolvedByEnum.NEAREST_PROVINCE -> PlaceResolution.NEAREST_PROVINCE
        ResolvedByEnum.NONE -> PlaceResolution.NONE
    },
)

internal fun Notification.toDomain() = InboxMessage(
    id = id.toString(),
    type = type,
    titleAr = titleAr,
    bodyAr = bodyAr,
    destination = when (destination) {
        DestinationEnum.FACILITY -> MessageDestination.FACILITY
        DestinationEnum.OWNER_FACILITIES -> MessageDestination.OWNER_FACILITIES
        DestinationEnum.NONE -> MessageDestination.NONE
    },
    facilityId = facilityId?.toString(),
    isRead = isRead,
    createdAtEpochMillis = createdAt.toEpochMillis(),
)

internal fun NotificationPage.toDomain() = InboxPage(
    items = items.map { it.toDomain() },
    nextCursor = nextCursor,
    hasMore = hasMore,
    unreadCount = unreadCount,
)

internal fun KeyEnum.toDomain(): LegalPageKey = when (this) {
    KeyEnum.ABOUT -> LegalPageKey.ABOUT
    KeyEnum.PRIVACY -> LegalPageKey.PRIVACY
    KeyEnum.TERMS -> LegalPageKey.TERMS
    KeyEnum.INSTRUCTIONS -> LegalPageKey.INSTRUCTIONS
    KeyEnum.FAQ -> LegalPageKey.FAQ
    KeyEnum.CONTACT -> LegalPageKey.CONTACT
}

internal fun LegalDocumentSummary.toDomain() = LegalPage(
    key = key.toDomain(),
    titleAr = titleAr,
    version = version,
)

internal fun LegalDocument.toDomain() = LegalPage(
    key = key.toDomain(),
    titleAr = titleAr,
    version = version,
    bodyAr = bodyAr,
)

internal fun FacilityCursorPage.toDomain() = Page(
    items = items.map { it.toDomain() },
    nextCursor = nextCursor,
    hasMore = hasMore,
)

private fun PublicHoursEntry.toDomain() =
    BusinessHour(weekday = weekday, opensAt = opensAt, closesAt = closesAt, sequence = sequence)

private val hourOrder = compareBy<BusinessHour>({ it.weekday }, { it.sequence })

internal fun PublicFacilityDetail.toDomain() = FacilityDetail(
    summary = FacilitySummary(
        id = id.toString(),
        nameAr = nameAr,
        nameEn = nameEn,
        category = category.toCategory(),
        distanceMeters = distanceMeters,
        ratingAverage = ratingAverage,
        ratingCount = ratingCount,
        availability = availability.state.toDomain(),
        nextOpenAtEpochMillis = availability.nextOpenAt?.toEpochMillis(),
        cityNameAr = city?.nameAr,
    ),
    descriptionAr = descriptionAr,
    phone = phone,
    addressAr = addressAr,
    latitude = location?.latitude,
    longitude = location?.longitude,
    imageUrls = images.map { it.url },
    neighborhoodNameAr = neighborhood?.nameAr,
    hours = hours.map { it.toDomain() }.sortedWith(hourOrder),
    specialties = specialties.map { it.nameAr },
    services = services.map { it.nameAr },
    whatsapp = whatsapp?.takeIf { it.isNotBlank() },
    lastVerifiedAtEpochMillis = lastVerifiedAt?.toEpochMillis(),
    updatedAtEpochMillis = updatedAt.toEpochMillis(),
)

internal fun PublicAdvertisement.toDomain() = HomeAd(
    id = id.toString(),
    imageUrl = imageUrl.toString(),
    titleAr = titleAr,
    subtitleAr = subtitleAr,
    slideDurationMs = slideDurationMs,
    action = action.toDomain(),
)

/**
 * The backend validates the payload per type; the app checks again, because a tap on an ad
 * must never open anything but a facility, a category or an `https` page.
 */
internal fun AdvertisementAction.toDomain(): AdAction {
    fun text(key: String): String? =
        runCatching { payload[key]?.jsonPrimitive?.contentOrNull }.getOrNull()?.trim()?.takeIf { it.isNotEmpty() }
    return when (type) {
        AdvertisementActionTypeEnum.FACILITY -> text("facilityId")?.let(AdAction::OpenFacility)
        AdvertisementActionTypeEnum.CATEGORY -> text("categoryId")?.let(AdAction::OpenCategory)
        AdvertisementActionTypeEnum.EXTERNAL_URL -> text("url")?.takeIf(::isSafeExternalUrl)?.let(AdAction::OpenUrl)
        // App routes are typed; a path string from the backend has no safe mapping yet.
        AdvertisementActionTypeEnum.IN_APP_ROUTE, AdvertisementActionTypeEnum.NONE -> null
    } ?: AdAction.None
}

/** An absolute `https` address with a host and no credentials. */
internal fun isSafeExternalUrl(value: String): Boolean {
    val uri = runCatching { java.net.URI(value) }.getOrNull() ?: return false
    return uri.scheme.equals("https", ignoreCase = true) &&
        !uri.host.isNullOrBlank() &&
        uri.rawUserInfo == null
}

internal fun PublicHome.toDomain(province: Province) = HomeSnapshot(
    province = province,
    categories = categories.map { it.toDomain() },
    nearby = nearby.map { it.toDomain() },
    openNearby = openNearby.map { it.toDomain() },
    dutyNow = dutyNow.map { it.toDomain() },
    ads = ads.map { it.toDomain() },
    refreshedAtEpochMillis = serverTime.toEpochMillis(),
)

internal fun MapMarker.toDomain() = PublicMapFacility(
    id = id.toString(),
    label = nameAr,
    latitude = latitude,
    longitude = longitude,
    availability = availability.toDomain(),
    categoryIconKey = categoryIconKey,
)

// Account

internal fun Profile.toDomain() = AccountProfile(
    id = id.toString(),
    name = displayName,
    phone = phone,
    provinceId = provinceId?.toString(),
    address = address,
    imageUrl = profileImageUrl?.toString(),
)

internal fun AccountRating.toDomain() = UserRating(
    id = facilityId.toString(),
    facilityId = facilityId.toString(),
    facilityNameAr = facilityNameAr,
    stars = stars,
    updatedAtEpochMillis = updatedAt.toEpochMillis(),
)

internal fun SessionCredentials.toDomain() = SessionTokens(
    accessToken = accessToken,
    refreshToken = refreshToken,
    sessionId = sessionId.toString(),
)

internal fun ChallengeAccepted.toDomain() =
    AuthChallenge(id = challengeId.toString(), expiresAtEpochMillis = expiresAt.toEpochMillis())

internal fun UserSession.toDomain() = AccountSession(
    id = id.toString(),
    platform = platform,
    deviceName = deviceName,
    createdAtEpochMillis = createdAt.toEpochMillis(),
    lastSeenAtEpochMillis = lastSeenAt?.toEpochMillis(),
    revoked = revoked,
)

// Owner

internal fun FacilityStatusEnum.toDomain(): OwnerFacilityStatus = when (this) {
    FacilityStatusEnum.DRAFT -> OwnerFacilityStatus.DRAFT
    FacilityStatusEnum.SUBMITTED -> OwnerFacilityStatus.SUBMITTED
    FacilityStatusEnum.ACTIVE -> OwnerFacilityStatus.ACTIVE
    FacilityStatusEnum.REVERIFICATION_REQUIRED -> OwnerFacilityStatus.REVERIFICATION_REQUIRED
    FacilityStatusEnum.SUSPENDED -> OwnerFacilityStatus.SUSPENDED
    FacilityStatusEnum.CLOSED -> OwnerFacilityStatus.CLOSED
}

internal fun FacilityMemberRoleEnum.toDomain(): FacilityMemberRole = when (this) {
    FacilityMemberRoleEnum.OWNER -> FacilityMemberRole.OWNER
    FacilityMemberRoleEnum.MANAGER -> FacilityMemberRole.MANAGER
}

internal fun FacilityMemberRole.toWire(): FacilityMemberRoleEnum = when (this) {
    FacilityMemberRole.OWNER -> FacilityMemberRoleEnum.OWNER
    FacilityMemberRole.MANAGER -> FacilityMemberRoleEnum.MANAGER
}

private fun OwnerVerificationRequirement.toDomain() = VerificationRequirementDescriptor(
    id = id.toString(),
    labelAr = labelAr,
    labelEn = labelEn,
    instructionsAr = instructionsAr,
    required = required,
    minFiles = minFiles,
    maxFiles = maxFiles,
)

/**
 * The evidence form is built from what the backend configured for the category, never from
 * assumptions in the app. With the pharmacy policy still open (LAUNCH_POLICY_PENDING) the
 * list may be empty, and then the app asks for nothing.
 */
internal fun WireOwnerConfig.toDomain() = OwnerConfig(
    province = province.toDomain(),
    categories = categories.map { item ->
        OwnerCategoryConfig(
            category = Category(
                id = item.category.id.toString(),
                nameAr = item.category.nameAr,
                nameEn = item.category.nameEn,
                iconKey = item.category.iconKey,
            ),
            specialization = item.category.specialization.value,
            capabilities = item.capabilities.toDomain(),
            verificationRequirements = item.verificationRequirements.map { it.toDomain() },
        )
    },
)

internal fun WireOwnerFacilitySummary.toDomain() = OwnerFacilitySummary(
    id = id.toString(),
    nameAr = nameAr,
    category = category.toCategory(),
    province = province.toProvince(),
    status = status.toDomain(),
    lastUpdateEpochMillis = lastUpdate.toEpochMillis(),
    requiredAction = requiredAction?.value,
    capabilities = capabilities.toDomain(),
)

private fun OwnerHoursEntry.toDomain() =
    BusinessHour(weekday = weekday, opensAt = opensAt, closesAt = closesAt, sequence = sequence)

internal fun WireBusinessHour.toDomain() =
    BusinessHour(weekday = weekday, opensAt = opensAt, closesAt = closesAt, sequence = sequence)

private fun OwnerEvidenceRef.toDomain() = OwnerEvidence(
    id = id.toString(),
    requirementId = requirementId.toString(),
    createdAtEpochMillis = createdAt.toEpochMillis(),
)

private fun WireOwnerApplication.toDomain() = OwnerApplication(
    id = id.toString(),
    kind = kind.value,
    status = status.value,
    rejectionReason = rejectionReason,
    submittedAtEpochMillis = submittedAt?.toEpochMillis(),
)

internal fun WireOwnerFacilityDetail.toDomain() = OwnerFacilityDetail(
    summary = OwnerFacilitySummary(
        id = id.toString(),
        nameAr = nameAr,
        category = category.toCategory(),
        province = province.toProvince(),
        status = status.toDomain(),
        lastUpdateEpochMillis = lastUpdate.toEpochMillis(),
        requiredAction = requiredAction?.value,
        capabilities = capabilities.toDomain(),
    ),
    nameEn = nameEn,
    descriptionAr = descriptionAr,
    descriptionEn = descriptionEn,
    phone = phone,
    whatsapp = whatsapp?.takeIf { it.isNotBlank() },
    addressAr = addressAr,
    addressEn = addressEn,
    cityId = cityId?.toString(),
    neighborhoodId = neighborhoodId?.toString(),
    latitude = location?.latitude,
    longitude = location?.longitude,
    specialtyIds = specialtyIds.map { it.toString() },
    serviceTagIds = serviceTagIds.map { it.toString() },
    hours = hours.map { it.toDomain() }.sortedWith(hourOrder),
    evidence = evidence.map { it.toDomain() },
    application = application?.toDomain(),
)

internal fun WireOwnerFacilityInsights.toDomain() = OwnerFacilityInsights(
    facilityId = facilityId.toString(),
    windowDays = windowDays,
    sinceEpochMillis = since.toEpochMillis(),
    views = views,
    calls = calls,
    directions = directions,
)

internal fun FacilityReportReason.toWire(): FacilityReportReasonEnum = when (this) {
    FacilityReportReason.WRONG_INFO -> FacilityReportReasonEnum.WRONG_INFO
    FacilityReportReason.CLOSED_PERMANENTLY -> FacilityReportReasonEnum.CLOSED_PERMANENTLY
    FacilityReportReason.WRONG_LOCATION -> FacilityReportReasonEnum.WRONG_LOCATION
    FacilityReportReason.WRONG_HOURS -> FacilityReportReasonEnum.WRONG_HOURS
    FacilityReportReason.NOT_ON_DUTY -> FacilityReportReasonEnum.NOT_ON_DUTY
    FacilityReportReason.OTHER -> FacilityReportReasonEnum.OTHER
}

internal fun OwnerSubmitResult.toDomain() = OwnerSubmission(
    applicationId = applicationId.toString(),
    status = status.value,
    submittedAtEpochMillis = submittedAt.toEpochMillis(),
)

internal fun WireOwnerFacilityImage.toDomain() = OwnerFacilityImage(
    id = id.toString(),
    url = url,
    sortOrder = sortOrder,
    width = width,
    height = height,
)

internal fun WireTemporaryClosure.toDomain() = TemporaryClosure(
    id = id.toString(),
    startsAtEpochMillis = startsAt.toEpochMillis(),
    endsAtEpochMillis = endsAt.toEpochMillis(),
    // The backend stores an omitted reason as "": in the app, no reason is null.
    reason = reason?.trim()?.takeIf { it.isNotEmpty() },
)

internal fun WireDutyShift.toDomain() = DutyShift(
    id = id.toString(),
    startsAtEpochMillis = startsAt.toEpochMillis(),
    endsAtEpochMillis = endsAt.toEpochMillis(),
)

internal fun OwnerMember.toDomain() = FacilityMember(
    userId = userId.toString(),
    name = name,
    phone = phone,
    role = role.toDomain(),
)
