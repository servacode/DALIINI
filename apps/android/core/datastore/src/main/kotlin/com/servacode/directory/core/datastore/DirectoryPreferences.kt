package com.servacode.directory.core.datastore

data class DirectoryPreferences(
    val selectedProvinceId: String? = null,
    val locationPreference: LocationPreference = LocationPreference.ASK,
    val onboardingHintsSeen: Boolean = false,
)

enum class LocationPreference { ASK, ENABLED, DISABLED }
