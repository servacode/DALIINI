package com.servacode.directory.api.apis

import com.servacode.directory.api.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.servacode.directory.api.models.AdminRoleList
import com.servacode.directory.api.models.AdminUser
import com.servacode.directory.api.models.AdminUserDetail
import com.servacode.directory.api.models.AdminUserList
import com.servacode.directory.api.models.AdminUserRolesRequest
import com.servacode.directory.api.models.ApiError

interface AdminUsersApi {
    /**
     * GET api/v1/admin/roles/
     * List admin roles and their permission codes
     * 
     * Responses:
     *  - 200: 
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @return [AdminRoleList]
     */
    @GET("api/v1/admin/roles/")
    suspend fun adminRolesList(): Response<AdminRoleList>

    /**
     * POST api/v1/admin/users/{user_id}/block/
     * Block a user account
     * Blocking also revokes every active refresh session of that user.
     * Responses:
     *  - 200: 
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param userId 
     * @return [AdminUser]
     */
    @POST("api/v1/admin/users/{user_id}/block/")
    suspend fun adminUserBlock(@Path("user_id") userId: java.util.UUID): Response<AdminUser>

    /**
     * GET api/v1/admin/users/{user_id}/
     * Retrieve one user with the roles assigned
     * 
     * Responses:
     *  - 200: 
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param userId 
     * @return [AdminUserDetail]
     */
    @GET("api/v1/admin/users/{user_id}/")
    suspend fun adminUserRetrieve(@Path("user_id") userId: java.util.UUID): Response<AdminUserDetail>

    /**
     * PUT api/v1/admin/users/{user_id}/roles/
     * Replace the admin roles of a user
     * Authorization is always re-checked server-side; the Admin UI only hides actions as a convenience.
     * Responses:
     *  - 204: No response body
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param userId 
     * @param adminUserRolesRequest 
     * @return [Unit]
     */
    @PUT("api/v1/admin/users/{user_id}/roles/")
    suspend fun adminUserRolesReplace(@Path("user_id") userId: java.util.UUID, @Body adminUserRolesRequest: AdminUserRolesRequest): Response<Unit>

    /**
     * POST api/v1/admin/users/{user_id}/unblock/
     * Unblock a user account
     * Blocking also revokes every active refresh session of that user.
     * Responses:
     *  - 200: 
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param userId 
     * @return [AdminUser]
     */
    @POST("api/v1/admin/users/{user_id}/unblock/")
    suspend fun adminUserUnblock(@Path("user_id") userId: java.util.UUID): Response<AdminUser>

    /**
     * GET api/v1/admin/users/
     * Search user accounts
     * Password hashes and session secret material are never returned. Newest first, in cursor pages. Every filter is optional.
     * Responses:
     *  - 200: 
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @param cursor Opaque token returned as &#x60;nextCursor&#x60; by the previous page. (optional)
     * @param limit Page size, maximum 200, default 50. (optional)
     * @param q Free text matched against the account name and phone number. (optional)
     * @param role Admin role id or code; keeps accounts holding that role actively. The value &#x60;any&#x60; keeps every operator, &#x60;none&#x60; every non-operator. (optional)
     * @param status &#x60;active&#x60; keeps active accounts; any other value keeps blocked accounts. (optional)
     * @return [AdminUserList]
     */
    @GET("api/v1/admin/users/")
    suspend fun adminUsersList(@Query("cursor") cursor: kotlin.String? = null, @Query("limit") limit: kotlin.Int? = null, @Query("q") q: kotlin.String? = null, @Query("role") role: kotlin.String? = null, @Query("status") status: kotlin.String? = null): Response<AdminUserList>

}
