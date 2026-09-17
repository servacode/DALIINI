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
