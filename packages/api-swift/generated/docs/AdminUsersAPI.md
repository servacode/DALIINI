# AdminUsersAPI

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**adminRolesList**](AdminUsersAPI.md#adminroleslist) | **GET** /api/v1/admin/roles/ | List admin roles and their permission codes
[**adminUserBlock**](AdminUsersAPI.md#adminuserblock) | **POST** /api/v1/admin/users/{user_id}/block/ | Block a user account
[**adminUserRetrieve**](AdminUsersAPI.md#adminuserretrieve) | **GET** /api/v1/admin/users/{user_id}/ | Retrieve one user with the roles assigned
[**adminUserRolesReplace**](AdminUsersAPI.md#adminuserrolesreplace) | **PUT** /api/v1/admin/users/{user_id}/roles/ | Replace the admin roles of a user
[**adminUserUnblock**](AdminUsersAPI.md#adminuserunblock) | **POST** /api/v1/admin/users/{user_id}/unblock/ | Unblock a user account
[**adminUsersList**](AdminUsersAPI.md#adminuserslist) | **GET** /api/v1/admin/users/ | Search user accounts


# **adminRolesList**
```swift
    open class func adminRolesList(completion: @escaping (_ data: AdminRoleList?, _ error: Error?) -> Void)
```

List admin roles and their permission codes

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI


// List admin roles and their permission codes
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
    open class func adminUsersList(cursor: String? = nil, limit: Int? = nil, q: String? = nil, role: String? = nil, status: String? = nil, completion: @escaping (_ data: AdminUserList?, _ error: Error?) -> Void)
```

Search user accounts

Password hashes and session secret material are never returned. Newest first, in cursor pages. Every filter is optional.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let cursor = "cursor_example" // String | Opaque token returned as `nextCursor` by the previous page. (optional)
let limit = 987 // Int | Page size, maximum 200, default 50. (optional)
let q = "q_example" // String | Free text matched against the account name and phone number. (optional)
let role = "role_example" // String | Admin role id or code; keeps accounts holding that role actively. The value `any` keeps every operator, `none` every non-operator. (optional)
let status = "status_example" // String | `active` keeps active accounts; any other value keeps blocked accounts. (optional)

// Search user accounts
AdminUsersAPI.adminUsersList(cursor: cursor, limit: limit, q: q, role: role, status: status) { (response, error) in
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
 **limit** | **Int** | Page size, maximum 200, default 50. | [optional] 
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

