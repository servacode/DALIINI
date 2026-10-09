# AdminUsersAPI

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**adminPermissionsList**](AdminUsersAPI.md#adminpermissionslist) | **GET** /api/v1/admin/permissions/ | Every permission a role can carry
[**adminRoleCreate**](AdminUsersAPI.md#adminrolecreate) | **POST** /api/v1/admin/roles/ | Create a role with the permissions it carries
[**adminRoleDelete**](AdminUsersAPI.md#adminroledelete) | **DELETE** /api/v1/admin/roles/{role_id}/ | Delete a role nobody holds
[**adminRoleUpdate**](AdminUsersAPI.md#adminroleupdate) | **PATCH** /api/v1/admin/roles/{role_id}/ | Rename a role or change the permissions it carries
[**adminRolesList**](AdminUsersAPI.md#adminroleslist) | **GET** /api/v1/admin/roles/ | List admin roles, their permission codes and how many hold each
[**adminUserBlock**](AdminUsersAPI.md#adminuserblock) | **POST** /api/v1/admin/users/{user_id}/block/ | Block a user account
[**adminUserCreate**](AdminUsersAPI.md#adminusercreate) | **POST** /api/v1/admin/users/ | Open an account from the console
[**adminUserDelete**](AdminUsersAPI.md#adminuserdelete) | **POST** /api/v1/admin/users/{user_id}/delete/ | Delete an account at its owner&#39;s request
[**adminUserMfaReset**](AdminUsersAPI.md#adminusermfareset) | **POST** /api/v1/admin/users/{user_id}/mfa/reset/ | Clear an operator&#39;s authenticator after they lost it
[**adminUserRecoverySend**](AdminUsersAPI.md#adminuserrecoverysend) | **POST** /api/v1/admin/users/{user_id}/recovery/ | Send this account a password-recovery code
[**adminUserRetrieve**](AdminUsersAPI.md#adminuserretrieve) | **GET** /api/v1/admin/users/{user_id}/ | Retrieve one user with the roles assigned
[**adminUserRolesReplace**](AdminUsersAPI.md#adminuserrolesreplace) | **PUT** /api/v1/admin/users/{user_id}/roles/ | Replace the admin roles of a user
[**adminUserUnblock**](AdminUsersAPI.md#adminuserunblock) | **POST** /api/v1/admin/users/{user_id}/unblock/ | Unblock a user account
[**adminUsersList**](AdminUsersAPI.md#adminuserslist) | **GET** /api/v1/admin/users/ | Search user accounts


# **adminPermissionsList**
```swift
    open class func adminPermissionsList(completion: @escaping (_ data: AdminPermissionList?, _ error: Error?) -> Void)
```

Every permission a role can carry

The full catalogue, ordered by code. Labels for display belong to the client.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI


// Every permission a role can carry
AdminUsersAPI.adminPermissionsList() { (response, error) in
    guard error == nil else {
        print(error)
        return
    }

    if (response) {
        dump(response)
    }
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**AdminPermissionList**](AdminPermissionList.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminRoleCreate**
```swift
    open class func adminRoleCreate(adminRoleCreateRequest: AdminRoleCreateRequest, completion: @escaping (_ data: AdminRole?, _ error: Error?) -> Void)
```

Create a role with the permissions it carries

Requires `admin.roles.manage`, which is re-checked inside the handler.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let adminRoleCreateRequest = AdminRoleCreateRequest(name: "name_example", code: "code_example", permissions: ["permissions_example"]) // AdminRoleCreateRequest | 

// Create a role with the permissions it carries
AdminUsersAPI.adminRoleCreate(adminRoleCreateRequest: adminRoleCreateRequest) { (response, error) in
    guard error == nil else {
        print(error)
        return
    }

    if (response) {
        dump(response)
    }
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **adminRoleCreateRequest** | [**AdminRoleCreateRequest**](AdminRoleCreateRequest.md) |  | 

### Return type

[**AdminRole**](AdminRole.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminRoleDelete**
```swift
    open class func adminRoleDelete(roleId: Int, completion: @escaping (_ data: Void?, _ error: Error?) -> Void)
```

Delete a role nobody holds

Refused (409) while any account holds the role, blocked accounts included, so a role is never taken from somebody as a side effect; and for the owner role.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let roleId = 987 // Int | 

// Delete a role nobody holds
AdminUsersAPI.adminRoleDelete(roleId: roleId) { (response, error) in
    guard error == nil else {
        print(error)
        return
    }

    if (response) {
        dump(response)
    }
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **roleId** | **Int** |  | 

### Return type

Void (empty response body)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminRoleUpdate**
```swift
    open class func adminRoleUpdate(roleId: Int, patchedAdminRoleUpdateRequest: PatchedAdminRoleUpdateRequest? = nil, completion: @escaping (_ data: AdminRole?, _ error: Error?) -> Void)
```

Rename a role or change the permissions it carries

Omitted fields keep their value. Takes effect for every holder on their next request. Refused (409) for the owner role, and when it would leave nobody able to grant roles.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let roleId = 987 // Int | 
let patchedAdminRoleUpdateRequest = PatchedAdminRoleUpdateRequest(name: "name_example", permissions: ["permissions_example"]) // PatchedAdminRoleUpdateRequest |  (optional)

// Rename a role or change the permissions it carries
AdminUsersAPI.adminRoleUpdate(roleId: roleId, patchedAdminRoleUpdateRequest: patchedAdminRoleUpdateRequest) { (response, error) in
    guard error == nil else {
        print(error)
        return
    }

    if (response) {
        dump(response)
    }
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **roleId** | **Int** |  | 
 **patchedAdminRoleUpdateRequest** | [**PatchedAdminRoleUpdateRequest**](PatchedAdminRoleUpdateRequest.md) |  | [optional] 

### Return type

[**AdminRole**](AdminRole.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminRolesList**
```swift
    open class func adminRolesList(completion: @escaping (_ data: AdminRoleList?, _ error: Error?) -> Void)
```

List admin roles, their permission codes and how many hold each

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI


// List admin roles, their permission codes and how many hold each
AdminUsersAPI.adminRolesList() { (response, error) in
    guard error == nil else {
        print(error)
        return
    }

    if (response) {
        dump(response)
    }
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**AdminRoleList**](AdminRoleList.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminUserBlock**
```swift
    open class func adminUserBlock(userId: UUID, completion: @escaping (_ data: AdminUser?, _ error: Error?) -> Void)
```

Block a user account

Blocking also revokes every active refresh session of that user.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let userId = 987 // UUID | 

// Block a user account
AdminUsersAPI.adminUserBlock(userId: userId) { (response, error) in
    guard error == nil else {
        print(error)
        return
    }

    if (response) {
        dump(response)
    }
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **userId** | **UUID** |  | 

### Return type

[**AdminUser**](AdminUser.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminUserCreate**
```swift
    open class func adminUserCreate(adminUserCreateRequest: AdminUserCreateRequest, completion: @escaping (_ data: AdminUserCreated?, _ error: Error?) -> Void)
```

Open an account from the console

For appointing an operator without a shell on the server. No password is set: the account is opened without a usable one and the person chooses their own through recovery, which `adminUserRecoverySend` starts.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let adminUserCreateRequest = AdminUserCreateRequest(name: "name_example", phone: "phone_example", provinceId: 123) // AdminUserCreateRequest | 

// Open an account from the console
AdminUsersAPI.adminUserCreate(adminUserCreateRequest: adminUserCreateRequest) { (response, error) in
    guard error == nil else {
        print(error)
        return
    }

    if (response) {
        dump(response)
    }
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **adminUserCreateRequest** | [**AdminUserCreateRequest**](AdminUserCreateRequest.md) |  | 

### Return type

[**AdminUserCreated**](AdminUserCreated.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminUserDelete**
```swift
    open class func adminUserDelete(userId: UUID, completion: @escaping (_ data: Void?, _ error: Error?) -> Void)
```

Delete an account at its owner's request

For a request made through the site's deletion page or by email: Google Play requires an account to be deletable from outside the app. The same rules and anonymisation as the app's own deletion; the sole owner of a live facility is refused with 409 and the reason. Recorded under the operator's name.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let userId = 987 // UUID | 

// Delete an account at its owner's request
AdminUsersAPI.adminUserDelete(userId: userId) { (response, error) in
    guard error == nil else {
        print(error)
        return
    }

    if (response) {
        dump(response)
    }
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **userId** | **UUID** |  | 

### Return type

Void (empty response body)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminUserMfaReset**
```swift
    open class func adminUserMfaReset(userId: UUID, completion: @escaping (_ data: Void?, _ error: Error?) -> Void)
```

Clear an operator's authenticator after they lost it

They set up a new one at their next console sign-in. Their recovery codes are cleared too. Audited.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let userId = 987 // UUID | 

// Clear an operator's authenticator after they lost it
AdminUsersAPI.adminUserMfaReset(userId: userId) { (response, error) in
    guard error == nil else {
        print(error)
        return
    }

    if (response) {
        dump(response)
    }
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **userId** | **UUID** |  | 

### Return type

Void (empty response body)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminUserRecoverySend**
```swift
    open class func adminUserRecoverySend(userId: UUID, completion: @escaping (_ data: AdminUserRecoverySent?, _ error: Error?) -> Void)
```

Send this account a password-recovery code

The console never sets a password. This starts the ordinary recovery flow: the code goes to the account's own number, and the person chooses their own password. Neither the code nor the challenge id is returned, so an operator cannot complete someone else's recovery.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let userId = 987 // UUID | 

// Send this account a password-recovery code
AdminUsersAPI.adminUserRecoverySend(userId: userId) { (response, error) in
    guard error == nil else {
        print(error)
        return
    }

    if (response) {
        dump(response)
    }
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **userId** | **UUID** |  | 

### Return type

[**AdminUserRecoverySent**](AdminUserRecoverySent.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminUserRetrieve**
```swift
    open class func adminUserRetrieve(userId: UUID, completion: @escaping (_ data: AdminUserDetail?, _ error: Error?) -> Void)
```

Retrieve one user with the roles assigned

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let userId = 987 // UUID | 

// Retrieve one user with the roles assigned
AdminUsersAPI.adminUserRetrieve(userId: userId) { (response, error) in
    guard error == nil else {
        print(error)
        return
    }

    if (response) {
        dump(response)
    }
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **userId** | **UUID** |  | 

### Return type

[**AdminUserDetail**](AdminUserDetail.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminUserRolesReplace**
```swift
    open class func adminUserRolesReplace(userId: UUID, adminUserRolesRequest: AdminUserRolesRequest, completion: @escaping (_ data: Void?, _ error: Error?) -> Void)
```

Replace the admin roles of a user

Authorization is always re-checked server-side; the Admin UI only hides actions as a convenience.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let userId = 987 // UUID | 
let adminUserRolesRequest = AdminUserRolesRequest(roleIds: [123]) // AdminUserRolesRequest | 

// Replace the admin roles of a user
AdminUsersAPI.adminUserRolesReplace(userId: userId, adminUserRolesRequest: adminUserRolesRequest) { (response, error) in
    guard error == nil else {
        print(error)
        return
    }

    if (response) {
        dump(response)
    }
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **userId** | **UUID** |  | 
 **adminUserRolesRequest** | [**AdminUserRolesRequest**](AdminUserRolesRequest.md) |  | 

### Return type

Void (empty response body)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminUserUnblock**
```swift
    open class func adminUserUnblock(userId: UUID, completion: @escaping (_ data: AdminUser?, _ error: Error?) -> Void)
```

Unblock a user account

Blocking also revokes every active refresh session of that user.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let userId = 987 // UUID | 

// Unblock a user account
AdminUsersAPI.adminUserUnblock(userId: userId) { (response, error) in
    guard error == nil else {
        print(error)
        return
    }

    if (response) {
        dump(response)
    }
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **userId** | **UUID** |  | 

### Return type

[**AdminUser**](AdminUser.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminUsersList**
```swift
    open class func adminUsersList(cursor: String? = nil, id: String? = nil, kind: String? = nil, limit: Int? = nil, ordering: String? = nil, q: String? = nil, role: String? = nil, status: String? = nil, completion: @escaping (_ data: AdminUserList?, _ error: Error?) -> Void)
```

Search user accounts

Password hashes and session secret material are never returned. Newest first unless `ordering` says otherwise, in cursor pages. Every filter is optional.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let cursor = "cursor_example" // String | Opaque token returned as `nextCursor` by the previous page. (optional)
let id = "id_example" // String | One account by id. What a link to an account written before the console had cards resolves to, so it still arrives at that account alone. (optional)
let kind = "kind_example" // String | `owners` keeps accounts on at least one facility; `users` keeps the rest. (optional)
let limit = 987 // Int | Page size, maximum 200, default 50. (optional)
let ordering = "ordering_example" // String | createdAt, -createdAt (the default), name or -name. (optional)
let q = "q_example" // String | Free text matched against the account name and phone number. (optional)
let role = "role_example" // String | Admin role id or code; keeps accounts holding that role actively. The value `any` keeps every operator, `none` every non-operator. (optional)
let status = "status_example" // String | `active` keeps active accounts; any other value keeps blocked accounts. (optional)

// Search user accounts
AdminUsersAPI.adminUsersList(cursor: cursor, id: id, kind: kind, limit: limit, ordering: ordering, q: q, role: role, status: status) { (response, error) in
    guard error == nil else {
        print(error)
        return
    }

    if (response) {
        dump(response)
    }
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **cursor** | **String** | Opaque token returned as &#x60;nextCursor&#x60; by the previous page. | [optional] 
 **id** | **String** | One account by id. What a link to an account written before the console had cards resolves to, so it still arrives at that account alone. | [optional] 
 **kind** | **String** | &#x60;owners&#x60; keeps accounts on at least one facility; &#x60;users&#x60; keeps the rest. | [optional] 
 **limit** | **Int** | Page size, maximum 200, default 50. | [optional] 
 **ordering** | **String** | createdAt, -createdAt (the default), name or -name. | [optional] 
 **q** | **String** | Free text matched against the account name and phone number. | [optional] 
 **role** | **String** | Admin role id or code; keeps accounts holding that role actively. The value &#x60;any&#x60; keeps every operator, &#x60;none&#x60; every non-operator. | [optional] 
 **status** | **String** | &#x60;active&#x60; keeps active accounts; any other value keeps blocked accounts. | [optional] 

### Return type

[**AdminUserList**](AdminUserList.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

