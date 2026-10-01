package com.servacode.directory.api.apis

import com.servacode.directory.api.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.servacode.directory.api.models.ApiError
import com.servacode.directory.api.models.FacilityReportCreated
import com.servacode.directory.api.models.FacilityReportRequest

interface PublicFacilitiesApi {
    /**
     * POST api/v1/facilities/{facility_id}/reports/
     * Report a problem with a facility&#39;s listing
     * Anonymous callers are allowed; a signed-in caller is recorded as the reporter. Strictly throttled per account or IP. Only publicly visible facilities accept reports.
     * Responses:
     *  - 201: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param facilityId 
     * @param facilityReportRequest 
     * @return [FacilityReportCreated]
     */
    @POST("api/v1/facilities/{facility_id}/reports/")
    suspend fun publicFacilityReportCreate(@Path("facility_id") facilityId: java.util.UUID, @Body facilityReportRequest: FacilityReportRequest): Response<FacilityReportCreated>

}
