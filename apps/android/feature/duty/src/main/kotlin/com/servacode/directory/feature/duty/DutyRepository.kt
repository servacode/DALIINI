package com.servacode.directory.feature.duty

import com.servacode.directory.core.model.TemporaryClosure
import com.servacode.directory.core.model.DutyShift
import com.servacode.directory.core.network.DutyShiftInput
import com.servacode.directory.core.network.OwnerApiBoundary
import javax.inject.Inject

class DutyRepository @Inject constructor(
    private val api: OwnerApiBoundary,
) {
    suspend fun list(facilityId: String): Result<List<DutyShift>> =
        runCatching { api.duty(facilityId) }

    /** The facility's temporary closures, to warn before a shift falls inside one. */
    suspend fun closures(facilityId: String): Result<List<TemporaryClosure>> =
        runCatching { api.temporaryClosures(facilityId) }
    suspend fun create(facilityId: String, input: DutyShiftInput): Result<DutyShift> =
        runCatching { api.createDuty(facilityId, input) }
    suspend fun update(
        facilityId: String,
        shiftId: String,
        input: DutyShiftInput,
    ): Result<DutyShift> = runCatching { api.updateDuty(facilityId, shiftId, input) }
    suspend fun delete(facilityId: String, shiftId: String): Result<Unit> =
        runCatching { api.deleteDuty(facilityId, shiftId) }
}
