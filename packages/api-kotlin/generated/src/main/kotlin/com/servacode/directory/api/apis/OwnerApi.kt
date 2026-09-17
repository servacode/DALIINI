package com.servacode.directory.api.apis

import com.servacode.directory.api.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.servacode.directory.api.models.DetailError
import com.servacode.directory.api.models.DomainError
import com.servacode.directory.api.models.FacilityCreate
import com.servacode.directory.api.models.FacilityLocation
import com.servacode.directory.api.models.FacilityMember
import com.servacode.directory.api.models.OwnerConfig
import com.servacode.directory.api.models.OwnerFacilityDetail
import com.servacode.directory.api.models.OwnerFacilitySummaryList
import com.servacode.directory.api.models.OwnerMemberList
import com.servacode.directory.api.models.OwnerMemberUpserted
import com.servacode.directory.api.models.OwnerSubmitResult
import com.servacode.directory.api.models.PatchedFacilityPatch

interface OwnerApi {
    /**
     * GET api/v1/owner/config/
     * List categories open for owner onboarding in a province
     * Returns only categories whose per-province owner switch is on and whose capability set allows onboarding, together with the safe descriptors of the verification requirements the owner will have to satisfy.
     * Responses:
     *  - 200: 
     *  - 400: The request was rejected by a domain rule.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param provinceId Province to inspect.
     * @return [OwnerConfig]
     */
    @GET("api/v1/owner/config/")
    suspend fun ownerConfigRetrieve(@Query("provinceId") provinceId: kotlin.String): Response<OwnerConfig>

    /**
     * GET api/v1/owner/facilities/
     * List the facilities the caller belongs to
     * 
     * Responses:
     *  - 200: 
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @return [OwnerFacilitySummaryList]
     */
    @GET("api/v1/owner/facilities/")
    suspend fun ownerFacilitiesList(): Response<OwnerFacilitySummaryList>

    /**
     * POST api/v1/owner/facilities/
     * Create a facility draft
     * The caller becomes the owner of the new draft. Creation re-checks the current province and category onboarding policy rather than any cached value.
     * Responses:
     *  - 201: 
     *  - 400: The request was rejected by a domain rule.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @param facilityCreate 
     * @return [OwnerFacilityDetail]
     */
    @POST("api/v1/owner/facilities/")
    suspend fun ownerFacilityCreate(@Body facilityCreate: FacilityCreate): Response<OwnerFacilityDetail>

    /**
     * PUT api/v1/owner/facilities/{facility_id}/location/
     * Set the map point of a facility
     * WGS84 decimal degrees. PostGIS remains the source of truth for geo.
     * Responses:
     *  - 200: 
     *  - 400: Request validation failed.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param facilityId 
     * @param facilityLocation 
     * @return [OwnerFacilityDetail]
     */
    @PUT("api/v1/owner/facilities/{facility_id}/location/")
    suspend fun ownerFacilityLocationReplace(@Path("facility_id") facilityId: java.util.UUID, @Body facilityLocation: FacilityLocation): Response<OwnerFacilityDetail>

    /**
     * DELETE api/v1/owner/facilities/{facility_id}/members/{user_id}/
     * Remove a member from a facility
     * Only an owner may call this, and the last owner cannot be removed.
     * Responses:
     *  - 204: No response body
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *  - 409: The request conflicts with the current state or with a domain rule.
     *
     * @param facilityId 
     * @param userId 
     * @return [Unit]
     */
    @DELETE("api/v1/owner/facilities/{facility_id}/members/{user_id}/")
    suspend fun ownerFacilityMemberDelete(@Path("facility_id") facilityId: java.util.UUID, @Path("user_id") userId: java.util.UUID): Response<Unit>

    /**
     * POST api/v1/owner/facilities/{facility_id}/members/
     * Add a member or change a member role
     * Only an owner may call this, and the last owner cannot be demoted.
     * Responses:
     *  - 201: 
     *  - 400: Request validation failed.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *  - 409: The request conflicts with the current state or with a domain rule.
     *
     * @param facilityId 
     * @param facilityMember 
     * @return [OwnerMemberUpserted]
     */
    @POST("api/v1/owner/facilities/{facility_id}/members/")
    suspend fun ownerFacilityMemberUpsert(@Path("facility_id") facilityId: java.util.UUID, @Body facilityMember: FacilityMember): Response<OwnerMemberUpserted>

    /**
     * GET api/v1/owner/facilities/{facility_id}/members/
     * List the members of a facility
     * 
     * Responses:
     *  - 200: 
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param facilityId 
     * @return [OwnerMemberList]
     */
    @GET("api/v1/owner/facilities/{facility_id}/members/")
    suspend fun ownerFacilityMembersList(@Path("facility_id") facilityId: java.util.UUID): Response<OwnerMemberList>

    /**
     * GET api/v1/owner/facilities/{facility_id}/
     * Retrieve one facility the caller belongs to
     * 
     * Responses:
     *  - 200: 
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param facilityId 
     * @return [OwnerFacilityDetail]
     */
    @GET("api/v1/owner/facilities/{facility_id}/")
    suspend fun ownerFacilityRetrieve(@Path("facility_id") facilityId: java.util.UUID): Response<OwnerFacilityDetail>

    /**
     * POST api/v1/owner/facilities/{facility_id}/submit/
     * Submit a facility for review
     * Submission re-validates the current onboarding policy and the completeness of the current evidence requirements. Only one submitted application of a given kind can exist per facility at a time.
     * Responses:
     *  - 200: 
     *  - 400: The request was rejected by a domain rule.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param facilityId 
     * @return [OwnerSubmitResult]
     */
    @POST("api/v1/owner/facilities/{facility_id}/submit/")
    suspend fun ownerFacilitySubmit(@Path("facility_id") facilityId: java.util.UUID): Response<OwnerSubmitResult>

    /**
     * PATCH api/v1/owner/facilities/{facility_id}/
     * Update the core fields of a facility
     * Editing a sensitive field on an active facility moves it into REVERIFICATION_REQUIRED, so the change is reviewed before it becomes public.
     * Responses:
     *  - 200: 
     *  - 400: The request was rejected by a domain rule.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param facilityId 
     * @param patchedFacilityPatch  (optional)
     * @return [OwnerFacilityDetail]
     */
    @PATCH("api/v1/owner/facilities/{facility_id}/")
    suspend fun ownerFacilityUpdate(@Path("facility_id") facilityId: java.util.UUID, @Body patchedFacilityPatch: PatchedFacilityPatch? = null): Response<OwnerFacilityDetail>

}
