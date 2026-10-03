package com.servacode.directory.api.apis

import com.servacode.directory.api.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.servacode.directory.api.models.ApiError
import com.servacode.directory.api.models.OwnerEvidenceCreated
import com.servacode.directory.api.models.OwnerFacilityImage
import com.servacode.directory.api.models.OwnerFacilityImageList

import okhttp3.MultipartBody

interface MediaApi {
    /**
     * POST api/v1/owner/facilities/{facility_id}/evidence/
     * Upload private verification evidence
     * Sent as multipart/form-data and stored in the private namespace. The response carries identifiers only: evidence is never served through a public URL and its storage key is never returned.
     * Responses:
     *  - 201: 
     *  - 400: A domain rule rejected the request; `code` names the rule.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *  - 409: The request conflicts with the current state or with a domain rule.
     *
     * @param facilityId 
     * @param requirementId 
     * @param file 
     * @return [OwnerEvidenceCreated]
     */
    @Multipart
    @POST("api/v1/owner/facilities/{facility_id}/evidence/")
    suspend fun ownerFacilityEvidenceCreate(@Path("facility_id") facilityId: java.util.UUID, @Part("requirementId") requirementId: kotlin.Int, @Part file: MultipartBody.Part): Response<OwnerEvidenceCreated>

    /**
     * DELETE api/v1/owner/facilities/{facility_id}/evidence/{evidence_id}/
     * Delete a piece of verification evidence
     * Evidence is locked while an application is under review.
     * Responses:
     *  - 204: No response body
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *  - 409: The request conflicts with the current state or with a domain rule.
     *
     * @param evidenceId 
     * @param facilityId 
     * @return [Unit]
     */
    @DELETE("api/v1/owner/facilities/{facility_id}/evidence/{evidence_id}/")
    suspend fun ownerFacilityEvidenceDelete(@Path("evidence_id") evidenceId: java.util.UUID, @Path("facility_id") facilityId: java.util.UUID): Response<Unit>

    /**
     * POST api/v1/owner/facilities/{facility_id}/images/
     * Upload a public facility image
     * Sent as multipart/form-data. The server decodes the file, enforces byte and pixel limits, re-encodes to JPEG, strips metadata and stores it under a random key. The declared extension and MIME type are not trusted.
     * Responses:
     *  - 201: 
     *  - 400: A domain rule rejected the request; `code` names the rule.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *  - 409: The request conflicts with the current state or with a domain rule.
     *
     * @param facilityId 
     * @param file 
     * @return [OwnerFacilityImage]
     */
    @Multipart
    @POST("api/v1/owner/facilities/{facility_id}/images/")
    suspend fun ownerFacilityImageCreate(@Path("facility_id") facilityId: java.util.UUID, @Part file: MultipartBody.Part): Response<OwnerFacilityImage>

    /**
     * DELETE api/v1/owner/facilities/{facility_id}/images/{image_id}/
     * Delete a public facility image
     * 
     * Responses:
     *  - 204: No response body
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *  - 409: The request conflicts with the current state or with a domain rule.
     *
     * @param facilityId 
     * @param imageId 
     * @return [Unit]
     */
    @DELETE("api/v1/owner/facilities/{facility_id}/images/{image_id}/")
    suspend fun ownerFacilityImageDelete(@Path("facility_id") facilityId: java.util.UUID, @Path("image_id") imageId: java.util.UUID): Response<Unit>

    /**
     * GET api/v1/owner/facilities/{facility_id}/images/
     * List the public images of a facility
     * 
     * Responses:
     *  - 200: 
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param facilityId 
     * @return [OwnerFacilityImageList]
     */
    @GET("api/v1/owner/facilities/{facility_id}/images/")
    suspend fun ownerFacilityImagesList(@Path("facility_id") facilityId: java.util.UUID): Response<OwnerFacilityImageList>

}
