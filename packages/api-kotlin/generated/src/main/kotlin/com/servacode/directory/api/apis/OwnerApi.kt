package com.servacode.directory.api.apis

import com.servacode.directory.api.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.servacode.directory.api.models.ApiError
import com.servacode.directory.api.models.Claim
import com.servacode.directory.api.models.ClaimList
import com.servacode.directory.api.models.ClaimStart
import com.servacode.directory.api.models.ClaimableFacilityList
import com.servacode.directory.api.models.FacilityCreate
import com.servacode.directory.api.models.FacilityLocation
import com.servacode.directory.api.models.FacilityMember
import com.servacode.directory.api.models.Invitation
import com.servacode.directory.api.models.InvitationList
import com.servacode.directory.api.models.InvitationRequest
import com.servacode.directory.api.models.OwnerConfig
import com.servacode.directory.api.models.OwnerFacilityDetail
import com.servacode.directory.api.models.OwnerFacilityInsights
import com.servacode.directory.api.models.OwnerFacilitySummaryList
import com.servacode.directory.api.models.OwnerHoursConfirmed
import com.servacode.directory.api.models.OwnerMemberList
import com.servacode.directory.api.models.OwnerMemberUpserted
import com.servacode.directory.api.models.OwnerSubmitResult
import com.servacode.directory.api.models.PatchedFacilityPatch

interface OwnerApi {
    /**
     * GET api/v1/owner/claims/{claim_id}/
     * One of this account&#39;s claims
     * 
     * Responses:
     *  - 200: 
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param claimId 
     * @return [Claim]
     */
    @GET("api/v1/owner/claims/{claim_id}/")
    suspend fun ownerClaimRetrieve(@Path("claim_id") claimId: java.util.UUID): Response<Claim>

    /**
     * POST api/v1/owner/claims/
     * Start claiming a facility
     * Returns the open claim this account already has for the facility, if any. 404 when the facility is not claimable; 409 FACILITY_ALREADY_OWNED when it has an owner, TOO_MANY_CLAIMS past five open claims.
     * Responses:
     *  - 201: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *  - 409: The request conflicts with the current state or with a domain rule.
     *
     * @param claimStart 
     * @return [Claim]
     */
    @POST("api/v1/owner/claims/")
    suspend fun ownerClaimStart(@Body claimStart: ClaimStart): Response<Claim>

    /**
     * POST api/v1/owner/claims/{claim_id}/submit/
     * Send a claim for review
     * Every required document must be uploaded to the claim. 409 CLAIM_PENDING while another claim on the same facility is being reviewed; FACILITY_ALREADY_OWNED if it gained an owner meanwhile.
     * Responses:
     *  - 200: 
     *  - 400: A domain rule rejected the request; `code` names the rule.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *  - 409: The request conflicts with the current state or with a domain rule.
     *
     * @param claimId 
     * @return [Claim]
     */
    @POST("api/v1/owner/claims/{claim_id}/submit/")
    suspend fun ownerClaimSubmit(@Path("claim_id") claimId: java.util.UUID): Response<Claim>

    /**
     * DELETE api/v1/owner/claims/{claim_id}/
     * Withdraw a claim and delete its documents
     * 
     * Responses:
     *  - 204: No response body
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *  - 409: The request conflicts with the current state or with a domain rule.
     *
     * @param claimId 
     * @return [Unit]
     */
    @DELETE("api/v1/owner/claims/{claim_id}/")
    suspend fun ownerClaimWithdraw(@Path("claim_id") claimId: java.util.UUID): Response<Unit>

    /**
     * GET api/v1/owner/claimable-facilities/
     * Find a published facility nobody owns yet
     * For «هذه منشأتي». Matches the Arabic or English name, Arabic spelling folded as in search. At most 20 results; &#x60;q&#x60; needs two characters.
     * Responses:
     *  - 200: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @param q 
     * @param categoryId Keep facilities of this category. (optional)
     * @param provinceId Keep facilities in this province. (optional)
     * @return [ClaimableFacilityList]
     */
    @GET("api/v1/owner/claimable-facilities/")
    suspend fun ownerClaimableFacilitiesList(@Query("q") q: kotlin.String, @Query("categoryId") categoryId: kotlin.String? = null, @Query("provinceId") provinceId: kotlin.String? = null): Response<ClaimableFacilityList>

    /**
     * GET api/v1/owner/claims/
     * This account&#39;s claims, newest first
     * 
     * Responses:
     *  - 200: 
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @return [ClaimList]
     */
    @GET("api/v1/owner/claims/")
    suspend fun ownerClaimsList(): Response<ClaimList>

    /**
     * GET api/v1/owner/config/
     * List categories open for owner onboarding in a province
     * Returns only categories whose per-province owner switch is on and whose capability set allows onboarding, together with the safe descriptors of the verification requirements the owner will have to satisfy, and the specialties and services the owner may pick for a facility of each.
     * Responses:
     *  - 200: 
     *  - 400: A domain rule rejected the request; `code` names the rule.
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
     *  - 400: A domain rule rejected the request; `code` names the rule.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @param facilityCreate 
     * @return [OwnerFacilityDetail]
     */
    @POST("api/v1/owner/facilities/")
    suspend fun ownerFacilityCreate(@Body facilityCreate: FacilityCreate): Response<OwnerFacilityDetail>

    /**
     * POST api/v1/owner/facilities/{facility_id}/confirm-hours/
     * Confirm that the facility&#39;s opening hours are still right
     * Any owner or manager may confirm. Sets &#x60;hoursConfirmedAt&#x60;, which also moves the public &#x60;infoConfirmedAt&#x60;; &#x60;lastVerifiedAt&#x60; keeps meaning an operator approval. Replacing the hours confirms them too. 409 HOURS_NOT_SUPPORTED when the category has no opening hours.
     * Responses:
     *  - 200: 
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *  - 409: The request conflicts with the current state or with a domain rule.
     *
     * @param facilityId 
     * @return [OwnerHoursConfirmed]
     */
    @POST("api/v1/owner/facilities/{facility_id}/confirm-hours/")
    suspend fun ownerFacilityHoursConfirm(@Path("facility_id") facilityId: java.util.UUID): Response<OwnerHoursConfirmed>

    /**
     * GET api/v1/owner/facilities/{facility_id}/insights/
     * Engagement with a facility over the last 30 days
     * Counts of product analytics events that reference this facility.
     * Responses:
     *  - 200: 
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param facilityId 
     * @return [OwnerFacilityInsights]
     */
    @GET("api/v1/owner/facilities/{facility_id}/insights/")
    suspend fun ownerFacilityInsightsRetrieve(@Path("facility_id") facilityId: java.util.UUID): Response<OwnerFacilityInsights>

    /**
     * POST api/v1/owner/facilities/{facility_id}/invitations/
     * Invite someone to help run a facility, by phone number
     * Owners only. The answer is the same whether or not the number has an account, so this cannot be used to find out who is registered. A person with an account is notified at once; anyone else finds the invitation when they sign up with that number. It lasts seven days; inviting the same number again renews it. 409 ALREADY_MEMBER when the number belongs to a member already.
     * Responses:
     *  - 201: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *  - 409: The request conflicts with the current state or with a domain rule.
     *
     * @param facilityId 
     * @param invitationRequest 
     * @return [Invitation]
     */
    @POST("api/v1/owner/facilities/{facility_id}/invitations/")
    suspend fun ownerFacilityInvitationCreate(@Path("facility_id") facilityId: java.util.UUID, @Body invitationRequest: InvitationRequest): Response<Invitation>

    /**
     * DELETE api/v1/owner/facilities/{facility_id}/invitations/{invitation_id}/
     * Withdraw an invitation that has not been answered
     * 
     * Responses:
     *  - 204: No response body
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *  - 409: The request conflicts with the current state or with a domain rule.
     *
     * @param facilityId 
     * @param invitationId 
     * @return [Unit]
     */
    @DELETE("api/v1/owner/facilities/{facility_id}/invitations/{invitation_id}/")
    suspend fun ownerFacilityInvitationRevoke(@Path("facility_id") facilityId: java.util.UUID, @Path("invitation_id") invitationId: java.util.UUID): Response<Unit>

    /**
     * GET api/v1/owner/facilities/{facility_id}/invitations/
     * Invitations sent for a facility
     * Owners only. Newest first.
     * Responses:
     *  - 200: 
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param facilityId 
     * @return [InvitationList]
     */
    @GET("api/v1/owner/facilities/{facility_id}/invitations/")
    suspend fun ownerFacilityInvitationsList(@Path("facility_id") facilityId: java.util.UUID): Response<InvitationList>

    /**
     * PUT api/v1/owner/facilities/{facility_id}/location/
     * Set the map point of a facility
     * WGS84 decimal degrees. PostGIS remains the source of truth for geo. On an ACTIVE facility the new point waits for review and the published one stays.
     * Responses:
     *  - 200: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *  - 409: The request conflicts with the current state or with a domain rule.
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
     * Only an owner may call this, and the last owner cannot be demoted. Adding a new member by account id is deprecated: invite them by phone number with ownerFacilityInvitationCreate, which they accept themselves. Changing the role of an existing member stays here.
     * Responses:
     *  - 201: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
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
     * Submission re-validates the current onboarding policy and the completeness of the current evidence requirements. Only one submitted application of a given kind can exist per facility at a time. An ACTIVE facility is never taken down to be reviewed: its edits are sent as they are saved, and submitting answers with the change already waiting, or 400 when there is none.
     * Responses:
     *  - 200: 
     *  - 400: A domain rule rejected the request; `code` names the rule.
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
     * On an ACTIVE facility the facility stays published: its name, address, city, neighbourhood and map point wait for an operator as a CHANGE application (&#x60;pendingChange&#x60; in the response), and every other field applies at once. A second edit while one waits is merged into it. Elsewhere the edit applies as it stands and is reviewed at the next submission.
     * Responses:
     *  - 200: 
     *  - 400: A domain rule rejected the request; `code` names the rule.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *  - 409: The request conflicts with the current state or with a domain rule.
     *
     * @param facilityId 
     * @param patchedFacilityPatch  (optional)
     * @return [OwnerFacilityDetail]
     */
    @PATCH("api/v1/owner/facilities/{facility_id}/")
    suspend fun ownerFacilityUpdate(@Path("facility_id") facilityId: java.util.UUID, @Body patchedFacilityPatch: PatchedFacilityPatch? = null): Response<OwnerFacilityDetail>

}
