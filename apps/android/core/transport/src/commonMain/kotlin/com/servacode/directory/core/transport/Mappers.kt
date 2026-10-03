package com.servacode.directory.core.transport

import com.servacode.directory.api.multiplatform.models.AccountRating
import com.servacode.directory.api.multiplatform.models.AdvertisementAction
import com.servacode.directory.api.multiplatform.models.AdvertisementActionTypeEnum
import com.servacode.directory.api.multiplatform.models.AppRelease as WireAppRelease
import com.servacode.directory.api.multiplatform.models.AvailabilityStateEnum
import com.servacode.directory.api.multiplatform.models.BilingualRef
import com.servacode.directory.api.multiplatform.models.BusinessHour as WireBusinessHour
import com.servacode.directory.api.multiplatform.models.CategoryCapabilities
import com.servacode.directory.api.multiplatform.models.ChallengeAccepted
import com.servacode.directory.api.multiplatform.models.Claim as WireClaim
import com.servacode.directory.api.multiplatform.models.ClaimEvidence as WireClaimEvidence
import com.servacode.directory.api.multiplatform.models.ClaimableFacility as WireClaimableFacility
import com.servacode.directory.api.multiplatform.models.CompactFacility
import com.servacode.directory.api.multiplatform.models.Coordinates
import com.servacode.directory.api.multiplatform.models.DestinationEnum
import com.servacode.directory.api.multiplatform.models.DutyShift as WireDutyShift
import com.servacode.directory.api.multiplatform.models.EmergencyNumber as WireEmergencyNumber
import com.servacode.directory.api.multiplatform.models.EmergencyNumberScopeEnum
import com.servacode.directory.api.multiplatform.models.FacilityApplicationStatusEnum
import com.servacode.directory.api.multiplatform.models.FacilityCursorPage
import com.servacode.directory.api.multiplatform.models.FacilityMemberRoleEnum
import com.servacode.directory.api.multiplatform.models.FacilityReportReasonEnum
import com.servacode.directory.api.multiplatform.models.FacilityStatusEnum
import com.servacode.directory.api.multiplatform.models.FavoriteFacility
import com.servacode.directory.api.multiplatform.models.FavoriteList
import com.servacode.directory.api.multiplatform.models.HomeCategory
import com.servacode.directory.api.multiplatform.models.Invitation as WireInvitation
import com.servacode.directory.api.multiplatform.models.InvitationStatusEnum
import com.servacode.directory.api.multiplatform.models.KeyEnum
import com.servacode.directory.api.multiplatform.models.LegalDocument
import com.servacode.directory.api.multiplatform.models.LegalDocumentSummary
import com.servacode.directory.api.multiplatform.models.MapMarker
import com.servacode.directory.api.multiplatform.models.NamedIntRef
import com.servacode.directory.api.multiplatform.models.NamedRef
import com.servacode.directory.api.multiplatform.models.Notification
import com.servacode.directory.api.multiplatform.models.NotificationPage
import com.servacode.directory.api.multiplatform.models.NotificationPreferences as WireNotificationPreferences
import com.servacode.directory.api.multiplatform.models.OwnerApplication as WireOwnerApplication
import com.servacode.directory.api.multiplatform.models.OwnerConfig as WireOwnerConfig
import com.servacode.directory.api.multiplatform.models.OwnerConfigProvince
import com.servacode.directory.api.multiplatform.models.OwnerEvidenceRef
import com.servacode.directory.api.multiplatform.models.OwnerFacilityDetail as WireOwnerFacilityDetail
import com.servacode.directory.api.multiplatform.models.OwnerFacilityImage as WireOwnerFacilityImage
import com.servacode.directory.api.multiplatform.models.OwnerFacilityInsights as WireOwnerFacilityInsights
import com.servacode.directory.api.multiplatform.models.OwnerFacilitySummary as WireOwnerFacilitySummary
import com.servacode.directory.api.multiplatform.models.OwnerHoursEntry
import com.servacode.directory.api.multiplatform.models.OwnerMember
import com.servacode.directory.api.multiplatform.models.OwnerSubmitResult
import com.servacode.directory.api.multiplatform.models.OwnerVerificationRequirement
import com.servacode.directory.api.multiplatform.models.Profile
import com.servacode.directory.api.multiplatform.models.PublicAdvertisement
import com.servacode.directory.api.multiplatform.models.PublicCategory
import com.servacode.directory.api.multiplatform.models.PublicCategoryTags
import com.servacode.directory.api.multiplatform.models.PublicDutyDay
import com.servacode.directory.api.multiplatform.models.PublicFacilityDetail
import com.servacode.directory.api.multiplatform.models.PublicHome
import com.servacode.directory.api.multiplatform.models.PublicHoursEntry
import com.servacode.directory.api.multiplatform.models.PublicLocationResolve
import com.servacode.directory.api.multiplatform.models.PublicProvince
import com.servacode.directory.api.multiplatform.models.ReceivedInvitation as WireReceivedInvitation
import com.servacode.directory.api.multiplatform.models.ResolvedByEnum
import com.servacode.directory.api.multiplatform.models.SessionCredentials
import com.servacode.directory.api.multiplatform.models.TemporaryClosure as WireTemporaryClosure
import com.servacode.directory.api.multiplatform.models.UserSession
import com.servacode.directory.core.auth.SessionTokens
import com.servacode.directory.core.model.AccountProfile
import com.servacode.directory.core.model.AccountSession
import com.servacode.directory.core.model.AdAction
import com.servacode.directory.core.model.AppRelease
import com.servacode.directory.core.model.AuthChallenge
import com.servacode.directory.core.model.AvailabilityState
import com.servacode.directory.core.model.BusinessHour
import com.servacode.directory.core.model.Category
import com.servacode.directory.core.model.CategoryTags
import com.servacode.directory.core.model.ClaimEvidence
import com.servacode.directory.core.model.ClaimRequirement
import com.servacode.directory.core.model.ClaimStatus
import com.servacode.directory.core.model.ClaimableFacility
import com.servacode.directory.core.model.DutyDay
import com.servacode.directory.core.model.DutyShift
import com.servacode.directory.core.model.DutyWindow
import com.servacode.directory.core.model.EmergencyNumber
import com.servacode.directory.core.model.EmergencyScope
import com.servacode.directory.core.model.FacilityCapabilities
import com.servacode.directory.core.model.FacilityClaim
import com.servacode.directory.core.model.FacilityDetail
import com.servacode.directory.core.model.FacilityInvitation
import com.servacode.directory.core.model.FacilityMember
import com.servacode.directory.core.model.FacilityMemberRole
import com.servacode.directory.core.model.FacilityReportReason
import com.servacode.directory.core.model.FacilitySummary
import com.servacode.directory.core.model.FacilityTag
import com.servacode.directory.core.model.GeoPoint
import com.servacode.directory.core.model.HomeAd
import com.servacode.directory.core.model.HomeSnapshot
import com.servacode.directory.core.model.InboxMessage
import com.servacode.directory.core.model.InboxPage
import com.servacode.directory.core.model.InvitationStatus
import com.servacode.directory.core.model.LegalPage
import com.servacode.directory.core.model.LegalPageKey
import com.servacode.directory.core.model.MessageDestination
import com.servacode.directory.core.model.NotificationSwitches
import com.servacode.directory.core.model.OwnerApplication
import com.servacode.directory.core.model.OwnerCategoryConfig
import com.servacode.directory.core.model.OwnerConfig
import com.servacode.directory.core.model.OwnerEvidence
import com.servacode.directory.core.model.OwnerFacilityDetail
import com.servacode.directory.core.model.OwnerFacilityImage
import com.servacode.directory.core.model.OwnerFacilityInsights
import com.servacode.directory.core.model.OwnerFacilityStatus
import com.servacode.directory.core.model.OwnerFacilitySummary
import com.servacode.directory.core.model.OwnerPendingChange
import com.servacode.directory.core.model.OwnerSubmission
import com.servacode.directory.core.model.Page
import com.servacode.directory.core.model.PlaceResolution
import com.servacode.directory.core.model.Province
import com.servacode.directory.core.model.PublicMapFacility
import com.servacode.directory.core.model.ReceivedInvitation
import com.servacode.directory.core.model.ResolvedPlace
import com.servacode.directory.core.model.TemporaryClosure
import com.servacode.directory.core.model.UserRating
import com.servacode.directory.core.model.VerificationRequirementDescriptor
import kotlinx.datetime.LocalTime
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/*
 * Every generated-to-domain conversion of the multiplatform transport lives in this file, and
 * nowhere else: the port of Android's `core/network/api/Mappers.kt`, conversion for conversion.
 *
 * Generated models never reach a repository or a screen. Enums are mapped with exhaustive
 * `when`s over the generated types, so a value added to the contract fails the build here
 * instead of being compared as a string somewhere in the UI. A value the backend sends that
 * the client was not generated for fails decoding, which the adapter reports as an
 * UNEXPECTED error rather than showing a wrong state.
 *
 * Time crosses the boundary here too: wire timestamps — `kotlin.time.Instant` in this client,
 * `OffsetDateTime` in the JVM one — become epoch milliseconds, and the domain never does
 * calendar arithmetic of its own. Whether a facility is open or on duty always comes from the
 * backend.
 *
 * The JVM client decoded ids as `java.util.UUID` and addresses as `java.net.URI`; this one
 * keeps both as the String the backend sent, so where Android wrote `id.toString()` the id is
 * taken as it is.
 */

// Time

/**
 * A time of day as the domain has always held it: the backend's own text, `HH:MM:SS`, with
 * microseconds when there are any (Python's `time.isoformat`).
 *
 * The JVM client kept opening hours as the strings the backend sent; this one reads them as
 * [LocalTime], whose `toString` leaves out zero seconds, so they are written back here the way
 * the backend writes them and the domain sees the same text as on Android.
 */
internal fun LocalTime.toBackendText(): String {
    val text = "${twoDigits(hour)}:${twoDigits(minute)}:${twoDigits(second)}"
    if (nanosecond == 0) return text
    val fraction = nanosecond.toString().padStart(NANOSECOND_DIGITS, '0')
    return "$text." + if (fraction.endsWith("000")) fraction.dropLast(3) else fraction
}

private fun twoDigits(value: Int): String = value.toString().padStart(2, '0')

private const val NANOSECOND_DIGITS = 9

// Taxonomy and places

internal fun Coordinates.toGeoPoint() = GeoPoint(latitude = latitude, longitude = longitude)

internal fun WireAppRelease.toDomain() = AppRelease(
    minimumVersionCode = minimumVersionCode,
    latestVersionCode = latestVersionCode,
    storeUrl = storeUrl,
    noticeAr = noticeAr,
)

internal fun PublicProvince.toDomain() = Province(
    id = id,
    nameAr = nameAr,
    nameEn = nameEn,
    mapCenter = mapCenter?.toGeoPoint(),
    code = code.trim().lowercase().takeIf { it.isNotEmpty() },
)

internal fun OwnerConfigProvince.toDomain() = Province(
    id = id,
    nameAr = nameAr,
    mapCenter = mapCenter?.toGeoPoint(),
)

internal fun NamedRef.toProvince() = Province(id = id, nameAr = nameAr)

internal fun NamedRef.toCategory() = Category(id = id, nameAr = nameAr)

internal fun BilingualRef.toCategory() = Category(id = id, nameAr = nameAr, nameEn = nameEn)

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
    id = id,
    nameAr = nameAr,
    nameEn = nameEn,
    iconKey = iconKey,
    capabilities = capabilities.toDomain(),
)

/** A specialty or a service. Its integer key becomes a string id, as every id is in the domain. */
internal fun NamedIntRef.toDomain() = FacilityTag(id = id.toString(), nameAr = nameAr)

/** The backend's order is the operators' order, so neither list is sorted here. */
internal fun PublicCategoryTags.toDomain() = CategoryTags(
    specialties = specialties.map { it.toDomain() },
    services = services.map { it.toDomain() },
)

/** The home payload carries the capabilities a home screen needs, not the owner-side ones. */
internal fun HomeCategory.toDomain() = Category(
    id = id,
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
    id = id,
    nameAr = nameAr,
    nameEn = nameEn,
    category = category.toCategory(),
    distanceMeters = distanceMeters,
    ratingAverage = ratingAverage,
    ratingCount = ratingCount,
    availability = availability.state.toDomain(),
    nextOpenAtEpochMillis = availability.nextOpenAt?.toEpochMilliseconds(),
    cityNameAr = city?.nameAr,
    isFavorite = isFavorite,
    isOpenNow = availability.isOpenNow,
    isOnDutyToday = availability.isOnDutyToday,
    imageUrl = imageUrl,
)

internal fun FavoriteFacility.toDomain() = FacilitySummary(
    id = id,
    nameAr = nameAr,
    nameEn = nameEn,
    category = category.toCategory(),
    distanceMeters = distanceMeters,
    ratingAverage = ratingAverage,
    ratingCount = ratingCount,
    availability = availability.state.toDomain(),
    nextOpenAtEpochMillis = availability.nextOpenAt?.toEpochMilliseconds(),
    cityNameAr = city?.nameAr,
    // Everything in this list is saved by definition, whatever the row says.
    isFavorite = true,
    isOpenNow = availability.isOpenNow,
    isOnDutyToday = availability.isOnDutyToday,
    imageUrl = imageUrl,
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
    id = id,
    type = type,
    titleAr = titleAr,
    bodyAr = bodyAr,
    destination = when (destination) {
        DestinationEnum.FACILITY -> MessageDestination.FACILITY
        DestinationEnum.OWNER_FACILITIES -> MessageDestination.OWNER_FACILITIES
        DestinationEnum.NONE -> MessageDestination.NONE
    },
    facilityId = facilityId,
    isRead = isRead,
    createdAtEpochMillis = createdAt.toEpochMilliseconds(),
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

private fun PublicHoursEntry.toDomain() = BusinessHour(
    weekday = weekday,
    opensAt = opensAt.toBackendText(),
    closesAt = closesAt.toBackendText(),
    sequence = sequence,
)

private val hourOrder = compareBy<BusinessHour>({ it.weekday }, { it.sequence })

internal fun PublicFacilityDetail.toDomain() = FacilityDetail(
    summary = FacilitySummary(
        id = id,
        nameAr = nameAr,
        nameEn = nameEn,
        category = category.toCategory(),
        distanceMeters = distanceMeters,
        ratingAverage = ratingAverage,
        ratingCount = ratingCount,
        availability = availability.state.toDomain(),
        nextOpenAtEpochMillis = availability.nextOpenAt?.toEpochMilliseconds(),
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
    lastVerifiedAtEpochMillis = lastVerifiedAt?.toEpochMilliseconds(),
    updatedAtEpochMillis = updatedAt.toEpochMilliseconds(),
    infoConfirmedAtEpochMillis = infoConfirmedAt?.toEpochMilliseconds(),
)

internal fun WireEmergencyNumber.toDomain() = EmergencyNumber(
    nameAr = labelAr,
    number = phone.filter { it.isDigit() || it == '+' },
    scope = when (scope) {
        EmergencyNumberScopeEnum.NATIONAL -> EmergencyScope.NATIONAL
        EmergencyNumberScopeEnum.PROVINCE -> EmergencyScope.PROVINCE
    },
    provinceId = provinceId,
)

internal fun PublicDutyDay.toDomain() = DutyDay(
    // ISO YYYY-MM-DD, as java.time.LocalDate writes it too.
    date = date.toString(),
    facilities = items.map { it.toDomain() },
    shifts = shifts.map {
        DutyWindow(it.facilityId, it.startsAt.toEpochMilliseconds(), it.endsAt.toEpochMilliseconds())
    },
)

internal fun PublicAdvertisement.toDomain() = HomeAd(
    id = id,
    imageUrl = imageUrl,
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

// isSafeExternalUrl, which Android writes with java.net.URI, is in ExternalUrls.kt.

internal fun PublicHome.toDomain(province: Province) = HomeSnapshot(
    province = province,
    categories = categories.map { it.toDomain() },
    nearby = nearby.map { it.toDomain() },
    openNearby = openNearby.map { it.toDomain() },
    dutyNow = dutyNow.map { it.toDomain() },
    ads = ads.map { it.toDomain() },
    refreshedAtEpochMillis = serverTime.toEpochMilliseconds(),
)

internal fun MapMarker.toDomain() = PublicMapFacility(
    id = id,
    label = nameAr,
    latitude = latitude,
    longitude = longitude,
    availability = availability.toDomain(),
    categoryIconKey = categoryIconKey,
)

// Account

internal fun Profile.toDomain() = AccountProfile(
    id = id,
    name = displayName,
    phone = phone,
    provinceId = provinceId,
    address = address,
    imageUrl = profileImageUrl,
)

internal fun AccountRating.toDomain() = UserRating(
    id = facilityId,
    facilityId = facilityId,
    facilityNameAr = facilityNameAr,
    stars = stars,
    updatedAtEpochMillis = updatedAt.toEpochMilliseconds(),
)

internal fun SessionCredentials.toDomain() = SessionTokens(
    accessToken = accessToken,
    refreshToken = refreshToken,
    sessionId = sessionId,
)

internal fun ChallengeAccepted.toDomain() =
    AuthChallenge(id = challengeId, expiresAtEpochMillis = expiresAt.toEpochMilliseconds())

internal fun UserSession.toDomain() = AccountSession(
    id = id,
    platform = platform,
    deviceName = deviceName,
    createdAtEpochMillis = createdAt.toEpochMilliseconds(),
    lastSeenAtEpochMillis = lastSeenAt?.toEpochMilliseconds(),
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
                id = item.category.id,
                nameAr = item.category.nameAr,
                nameEn = item.category.nameEn,
                iconKey = item.category.iconKey,
            ),
            specialization = item.category.specialization.value,
            capabilities = item.capabilities.toDomain(),
            verificationRequirements = item.verificationRequirements.map { it.toDomain() },
            tags = CategoryTags(
                specialties = item.specialties.map { it.toDomain() },
                services = item.services.map { it.toDomain() },
            ),
        )
    },
)

internal fun WireOwnerFacilitySummary.toDomain() = OwnerFacilitySummary(
    id = id,
    nameAr = nameAr,
    category = category.toCategory(),
    province = province.toProvince(),
    status = status.toDomain(),
    lastUpdateEpochMillis = lastUpdate.toEpochMilliseconds(),
    requiredAction = requiredAction?.value,
    capabilities = capabilities.toDomain(),
)

private fun OwnerHoursEntry.toDomain() = BusinessHour(
    weekday = weekday,
    opensAt = opensAt.toBackendText(),
    closesAt = closesAt.toBackendText(),
    sequence = sequence,
)

internal fun WireBusinessHour.toDomain() = BusinessHour(
    weekday = weekday,
    opensAt = opensAt.toBackendText(),
    closesAt = closesAt.toBackendText(),
    sequence = sequence,
)

private fun OwnerEvidenceRef.toDomain() = OwnerEvidence(
    id = id,
    requirementId = requirementId.toString(),
    createdAtEpochMillis = createdAt.toEpochMilliseconds(),
)

private fun WireOwnerApplication.toDomain() = OwnerApplication(
    id = id,
    kind = kind.value,
    status = status.value,
    rejectionReason = rejectionReason,
    submittedAtEpochMillis = submittedAt?.toEpochMilliseconds(),
)

internal fun WireOwnerFacilityDetail.toDomain() = OwnerFacilityDetail(
    summary = OwnerFacilitySummary(
        id = id,
        nameAr = nameAr,
        category = category.toCategory(),
        province = province.toProvince(),
        status = status.toDomain(),
        lastUpdateEpochMillis = lastUpdate.toEpochMilliseconds(),
        requiredAction = requiredAction?.value,
        capabilities = capabilities.toDomain(),
    ),
    nameEn = nameEn,
    descriptionAr = descriptionAr,
    descriptionEn = descriptionEn,
    phone = phone,
    hoursConfirmedAtEpochMillis = hoursConfirmedAt?.toEpochMilliseconds(),
    whatsapp = whatsapp?.takeIf { it.isNotBlank() },
    addressAr = addressAr,
    addressEn = addressEn,
    cityId = cityId,
    neighborhoodId = neighborhoodId,
    latitude = location?.latitude,
    longitude = location?.longitude,
    specialtyIds = specialtyIds.map { it.toString() },
    serviceTagIds = serviceTagIds.map { it.toString() },
    hours = hours.map { it.toDomain() }.sortedWith(hourOrder),
    evidence = evidence.map { it.toDomain() },
    application = application?.toDomain(),
    pendingChange = pendingChange?.let { change ->
        OwnerPendingChange(
            id = change.id,
            proposedFields = change.proposedFields.map { it.value },
            submittedAtEpochMillis = change.submittedAt?.toEpochMilliseconds(),
        )
    },
)

internal fun WireInvitation.toDomain() = FacilityInvitation(
    id = id,
    phone = phone,
    role = role.toDomain(),
    status = when (status) {
        InvitationStatusEnum.PENDING -> InvitationStatus.PENDING
        InvitationStatusEnum.ACCEPTED -> InvitationStatus.ACCEPTED
        InvitationStatusEnum.DECLINED -> InvitationStatus.DECLINED
        InvitationStatusEnum.REVOKED -> InvitationStatus.REVOKED
        InvitationStatusEnum.EXPIRED -> InvitationStatus.EXPIRED
    },
    createdAtEpochMillis = createdAt.toEpochMilliseconds(),
    expiresAtEpochMillis = expiresAt.toEpochMilliseconds(),
    respondedAtEpochMillis = respondedAt?.toEpochMilliseconds(),
)

internal fun WireReceivedInvitation.toDomain() = ReceivedInvitation(
    id = id,
    role = role.toDomain(),
    facilityId = facility.id,
    facilityNameAr = facility.nameAr,
    categoryNameAr = facility.categoryNameAr,
    provinceNameAr = facility.provinceNameAr,
    invitedByName = invitedByName?.takeIf { it.isNotBlank() },
    createdAtEpochMillis = createdAt.toEpochMilliseconds(),
    expiresAtEpochMillis = expiresAt.toEpochMilliseconds(),
)

internal fun WireNotificationPreferences.toDomain() = NotificationSwitches(
    dutyReminders = dutyReminders,
    provinceNews = provinceNews,
    applicationStatus = applicationStatus,
)

internal fun WireClaimableFacility.toDomain() = ClaimableFacility(
    id = id,
    nameAr = nameAr,
    categoryNameAr = categoryNameAr,
    provinceNameAr = provinceNameAr,
    cityNameAr = cityNameAr?.takeIf { it.isNotBlank() },
    addressAr = addressAr?.takeIf { it.isNotBlank() },
)

internal fun WireClaimEvidence.toDomain() = ClaimEvidence(
    id = id,
    requirementId = requirementId.toString(),
    createdAtEpochMillis = createdAt.toEpochMilliseconds(),
)

internal fun WireClaim.toDomain() = FacilityClaim(
    id = id,
    status = when (status) {
        FacilityApplicationStatusEnum.DRAFT -> ClaimStatus.DRAFT
        FacilityApplicationStatusEnum.SUBMITTED -> ClaimStatus.SUBMITTED
        FacilityApplicationStatusEnum.APPROVED -> ClaimStatus.APPROVED
        FacilityApplicationStatusEnum.REJECTED -> ClaimStatus.REJECTED
    },
    facilityId = facility.id,
    facilityNameAr = facility.nameAr,
    categoryNameAr = facility.categoryNameAr,
    provinceNameAr = facility.provinceNameAr,
    addressAr = facility.addressAr?.takeIf { it.isNotBlank() },
    // The model's integer keys (INT-068); the domain keeps every id opaque.
    requirements = requirements.map {
        ClaimRequirement(it.id.toString(), it.labelAr, it.required, it.minFiles, it.maxFiles)
    },
    evidence = evidence.map { it.toDomain() },
    rejectionReason = rejectionReason?.takeIf { it.isNotBlank() },
    submittedAtEpochMillis = submittedAt?.toEpochMilliseconds(),
    reviewedAtEpochMillis = reviewedAt?.toEpochMilliseconds(),
)

internal fun WireOwnerFacilityInsights.toDomain() = OwnerFacilityInsights(
    facilityId = facilityId,
    windowDays = windowDays,
    sinceEpochMillis = since.toEpochMilliseconds(),
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
    applicationId = applicationId,
    status = status.value,
    submittedAtEpochMillis = submittedAt.toEpochMilliseconds(),
)

internal fun WireOwnerFacilityImage.toDomain() = OwnerFacilityImage(
    id = id,
    url = url,
    sortOrder = sortOrder,
    width = width,
    height = height,
)

internal fun WireTemporaryClosure.toDomain() = TemporaryClosure(
    id = id,
    startsAtEpochMillis = startsAt.toEpochMilliseconds(),
    endsAtEpochMillis = endsAt.toEpochMilliseconds(),
    // The backend stores an omitted reason as "": in the app, no reason is null.
    reason = reason?.trim()?.takeIf { it.isNotEmpty() },
)

internal fun WireDutyShift.toDomain() = DutyShift(
    id = id,
    startsAtEpochMillis = startsAt.toEpochMilliseconds(),
    endsAtEpochMillis = endsAt.toEpochMilliseconds(),
)

internal fun OwnerMember.toDomain() = FacilityMember(
    userId = userId,
    name = name,
    phone = phone,
    role = role.toDomain(),
)
