package com.servacode.directory.feature.onboarding

import com.servacode.directory.core.network.OwnerFacilityPatch

/**
 * The basic information an owner types for a facility.
 *
 * Kept apart from the view model so the platform-free harness can test it: what follows a
 * draft's creation is decided here.
 */
data class OnboardingForm(
    val categoryId: String? = null,
    val nameAr: String = "",
    val nameEn: String = "",
    val descriptionAr: String = "",
    val phone: String = "",
    /** Optional. Blank clears it on the backend. */
    val whatsapp: String = "",
    val addressAr: String = "",
) {
    /** The details a draft's creation does not carry: only its name and category go with it. */
    val hasDetails: Boolean
        get() = listOf(descriptionAr, phone, whatsapp, addressAr).any { it.isNotBlank() }

    fun toPatch(): OwnerFacilityPatch = OwnerFacilityPatch(
        nameAr = nameAr.trim(),
        nameEn = nameEn.trim(),
        descriptionAr = descriptionAr.trim(),
        phone = phone.trim(),
        whatsapp = whatsapp.trim(),
        addressAr = addressAr.trim(),
    )
}
