package com.servacode.directory.feature.onboarding

import com.servacode.directory.core.model.BusinessHour
import com.servacode.directory.core.model.OwnerConfig
import com.servacode.directory.core.model.OwnerFacilityDetail
import com.servacode.directory.core.model.OwnerFacilityImage
import com.servacode.directory.core.model.OwnerSubmission
import com.servacode.directory.core.network.OwnerApiBoundary
import com.servacode.directory.core.network.OwnerFacilityDraftInput
import com.servacode.directory.core.network.OwnerFacilityPatch
import com.servacode.directory.core.network.OwnerUploadPayload
import javax.inject.Inject

class OnboardingRepository @Inject constructor(
    private val api: OwnerApiBoundary,
) {
    suspend fun config(provinceId: String): Result<OwnerConfig> =
        runCatching { api.ownerConfig(provinceId) }

    suspend fun facility(id: String): Result<OwnerFacilityDetail> = runCatching { api.facility(id) }

    suspend fun create(input: OwnerFacilityDraftInput): Result<OwnerFacilityDetail> =
        runCatching { api.createFacility(input) }

    suspend fun patch(id: String, patch: OwnerFacilityPatch): Result<OwnerFacilityDetail> =
        runCatching { api.patchFacility(id, patch) }

    suspend fun location(id: String, latitude: Double, longitude: Double): Result<OwnerFacilityDetail> =
        runCatching { api.updateLocation(id, latitude, longitude) }

    suspend fun hours(id: String, hours: List<BusinessHour>): Result<List<BusinessHour>> =
        runCatching { api.replaceHours(id, hours) }

    suspend fun images(id: String): Result<List<OwnerFacilityImage>> = runCatching { api.images(id) }

    suspend fun uploadImage(id: String, payload: OwnerUploadPayload): Result<OwnerFacilityImage> =
        runCatching { api.uploadImage(id, payload) }

    suspend fun uploadEvidence(
        id: String,
        requirementId: String,
        payload: OwnerUploadPayload,
    ) = runCatching { api.uploadEvidence(id, requirementId, payload) }

    suspend fun submit(id: String): Result<OwnerSubmission> = runCatching { api.submitFacility(id) }
}
