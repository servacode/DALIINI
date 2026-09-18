package com.servacode.directory.api.apis

import com.servacode.directory.api.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.servacode.directory.api.models.ApiError
import com.servacode.directory.api.models.FacilityRating
import com.servacode.directory.api.models.RatingWrite

interface RatingsApi {
    /**
     * DELETE api/v1/facilities/{facility_id}/rating/
     * Remove the caller&#39;s rating for a facility
     * 
     * Responses:
     *  - 204: No response body
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @param facilityId 
     * @return [Unit]
     */
    @DELETE("api/v1/facilities/{facility_id}/rating/")
    suspend fun facilityRatingDelete(@Path("facility_id") facilityId: java.util.UUID): Response<Unit>

    /**
     * PUT api/v1/facilities/{facility_id}/rating/
     * Create or replace the caller&#39;s rating for a facility
     * One rating per user per facility, so repeating the call replaces the previous value. Only categories that declare the ratings capability accept this.
     * Responses:
     *  - 200: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param facilityId 
     * @param ratingWrite 
     * @return [FacilityRating]
     */
    @PUT("api/v1/facilities/{facility_id}/rating/")
    suspend fun facilityRatingUpsert(@Path("facility_id") facilityId: java.util.UUID, @Body ratingWrite: RatingWrite): Response<FacilityRating>

}
