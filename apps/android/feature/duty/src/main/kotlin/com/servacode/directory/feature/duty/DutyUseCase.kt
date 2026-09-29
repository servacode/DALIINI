package com.servacode.directory.feature.duty

import com.servacode.directory.core.network.DutyShiftInput
import javax.inject.Inject

class LoadDutyUseCase @Inject constructor(
    private val repository: DutyRepository,
) {
    suspend operator fun invoke(facilityId: String) = repository.list(facilityId)

    suspend fun closures(facilityId: String) = repository.closures(facilityId)
}

class ManageDutyUseCase @Inject constructor(
    private val repository: DutyRepository,
) {
    suspend fun create(facilityId: String, input: DutyShiftInput) =
        repository.create(facilityId, input)
    suspend fun update(facilityId: String, shiftId: String, input: DutyShiftInput) =
        repository.update(facilityId, shiftId, input)
    suspend fun delete(facilityId: String, shiftId: String) = repository.delete(facilityId, shiftId)
}
