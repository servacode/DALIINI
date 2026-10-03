package com.servacode.directory.feature.onboarding

import com.servacode.directory.core.inject.Inject
import com.servacode.directory.core.model.BusinessHour
import com.servacode.directory.core.network.OwnerFacilityDraftInput
import com.servacode.directory.core.network.OwnerFacilityPatch
import com.servacode.directory.core.network.OwnerUploadPayload

class LoadOnboardingUseCase @Inject constructor(
    private val repository: OnboardingRepository,
) {
    suspend fun config(provinceId: String) = repository.config(provinceId)
    suspend fun facility(id: String) = repository.facility(id)
    suspend fun images(id: String) = repository.images(id)
}

class SaveOnboardingUseCase @Inject constructor(
    private val repository: OnboardingRepository,
) {
    suspend fun create(input: OwnerFacilityDraftInput) = repository.create(input)
    suspend fun patch(id: String, patch: OwnerFacilityPatch) = repository.patch(id, patch)
    suspend fun location(id: String, latitude: Double, longitude: Double) =
        repository.location(id, latitude, longitude)
    suspend fun hours(id: String, rows: List<BusinessHour>) = repository.hours(id, rows)
    suspend fun image(id: String, payload: OwnerUploadPayload) = repository.uploadImage(id, payload)
    suspend fun evidence(id: String, requirementId: String, payload: OwnerUploadPayload) =
        repository.uploadEvidence(id, requirementId, payload)
    suspend fun submit(id: String) = repository.submit(id)
}
