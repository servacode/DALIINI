# AdminUsersApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
| ------------- | ------------- | ------------- |
| [**adminPermissionsList**](AdminUsersApi.md#adminPermissionsList) | **GET** api/v1/admin/permissions/ | Every permission a role can carry |
| [**adminRoleCreate**](AdminUsersApi.md#adminRoleCreate) | **POST** api/v1/admin/roles/ | Create a role with the permissions it carries |
| [**adminRoleDelete**](AdminUsersApi.md#adminRoleDelete) | **DELETE** api/v1/admin/roles/{role_id}/ | Delete a role nobody holds |
| [**adminRoleUpdate**](AdminUsersApi.md#adminRoleUpdate) | **PATCH** api/v1/admin/roles/{role_id}/ | Rename a role or change the permissions it carries |
| [**adminRolesList**](AdminUsersApi.md#adminRolesList) | **GET** api/v1/admin/roles/ | List admin roles, their permission codes and how many hold each |
| [**adminUserBlock**](AdminUsersApi.md#adminUserBlock) | **POST** api/v1/admin/users/{user_id}/block/ | Block a user account |
| [**adminUserCreate**](AdminUsersApi.md#adminUserCreate) | **POST** api/v1/admin/users/ | Open an account from the console |
| [**adminUserMfaReset**](AdminUsersApi.md#adminUserMfaReset) | **POST** api/v1/admin/users/{user_id}/mfa/reset/ | Clear an operator&#39;s authenticator after they lost it |
| [**adminUserRecoverySend**](AdminUsersApi.md#adminUserRecoverySend) | **POST** api/v1/admin/users/{user_id}/recovery/ | Send this account a password-recovery code |
| [**adminUserRetrieve**](AdminUsersApi.md#adminUserRetrieve) | **GET** api/v1/admin/users/{user_id}/ | Retrieve one user with the roles assigned |
| [**adminUserRolesReplace**](AdminUsersApi.md#adminUserRolesReplace) | **PUT** api/v1/admin/users/{user_id}/roles/ | Replace the admin roles of a user |
| [**adminUserUnblock**](AdminUsersApi.md#adminUserUnblock) | **POST** api/v1/admin/users/{user_id}/unblock/ | Unblock a user account |
| [**adminUsersList**](AdminUsersApi.md#adminUsersList) | **GET** api/v1/admin/users/ | Search user accounts |



Every permission a role can carry

The full catalogue, ordered by code. Labels for display belong to the client.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminUsersApi::class.java)

launch(Dispatchers.IO) {
    val result : AdminPermissionList = webService.adminPermissionsList()
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**AdminPermissionList**](AdminPermissionList.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Create a role with the permissions it carries

Requires &#x60;admin.roles.manage&#x60;, which is re-checked inside the handler.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminUsersApi::class.java)
val adminRoleCreateRequest : AdminRoleCreateRequest =  // AdminRoleCreateRequest | 

launch(Dispatchers.IO) {
    val result : AdminRole = webService.adminRoleCreate(adminRoleCreateRequest)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **adminRoleCreateRequest** | [**AdminRoleCreateRequest**](AdminRoleCreateRequest.md)|  | |

### Return type

[**AdminRole**](AdminRole.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


Delete a role nobody holds

Refused (409) while any account holds the role, blocked accounts included, so a role is never taken from somebody as a side effect; and for the owner role.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminUsersApi::class.java)
val roleId : kotlin.Int = 56 // kotlin.Int | 

launch(Dispatchers.IO) {
    webService.adminRoleDelete(roleId)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **roleId** | **kotlin.Int**|  | |

### Return type

null (empty response body)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Rename a role or change the permissions it carries

Omitted fields keep their value. Takes effect for every holder on their next request. Refused (409) for the owner role, and when it would leave nobody able to grant roles.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminUsersApi::class.java)
val roleId : kotlin.Int = 56 // kotlin.Int | 
val patchedAdminRoleUpdateRequest : PatchedAdminRoleUpdateRequest =  // PatchedAdminRoleUpdateRequest | 

launch(Dispatchers.IO) {
    val result : AdminRole = webService.adminRoleUpdate(roleId, patchedAdminRoleUpdateRequest)
}
```

### Parameters
| **roleId** | **kotlin.Int**|  | |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **patchedAdminRoleUpdateRequest** | [**PatchedAdminRoleUpdateRequest**](PatchedAdminRoleUpdateRequest.md)|  | [optional] |

### Return type

[**AdminRole**](AdminRole.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


List admin roles, their permission codes and how many hold each

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminUsersApi::class.java)

launch(Dispatchers.IO) {
    val result : AdminRoleList = webService.adminRolesList()
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**AdminRoleList**](AdminRoleList.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Block a user account

Blocking also revokes every active refresh session of that user.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminUsersApi::class.java)
val userId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 

launch(Dispatchers.IO) {
    val result : AdminUser = webService.adminUserBlock(userId)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **userId** | **java.util.UUID**|  | |

### Return type

[**AdminUser**](AdminUser.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Open an account from the console

For appointing an operator without a shell on the server. No password is set: the account is opened without a usable one and the person chooses their own through recovery, which &#x60;adminUserRecoverySend&#x60; starts.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminUsersApi::class.java)
val adminUserCreateRequest : AdminUserCreateRequest =  // AdminUserCreateRequest | 

launch(Dispatchers.IO) {
    val result : AdminUserCreated = webService.adminUserCreate(adminUserCreateRequest)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **adminUserCreateRequest** | [**AdminUserCreateRequest**](AdminUserCreateRequest.md)|  | |

### Return type

[**AdminUserCreated**](AdminUserCreated.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


Clear an operator&#39;s authenticator after they lost it

They set up a new one at their next console sign-in. Their recovery codes are cleared too. Audited.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminUsersApi::class.java)
val userId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 

launch(Dispatchers.IO) {
    webService.adminUserMfaReset(userId)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **userId** | **java.util.UUID**|  | |

### Return type

null (empty response body)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Send this account a password-recovery code

The console never sets a password. This starts the ordinary recovery flow: the code goes to the account&#39;s own number, and the person chooses their own password. Neither the code nor the challenge id is returned, so an operator cannot complete someone else&#39;s recovery.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminUsersApi::class.java)
val userId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 

launch(Dispatchers.IO) {
    val result : AdminUserRecoverySent = webService.adminUserRecoverySend(userId)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **userId** | **java.util.UUID**|  | |

### Return type

[**AdminUserRecoverySent**](AdminUserRecoverySent.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Retrieve one user with the roles assigned

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminUsersApi::class.java)
val userId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 

launch(Dispatchers.IO) {
    val result : AdminUserDetail = webService.adminUserRetrieve(userId)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **userId** | **java.util.UUID**|  | |

### Return type

[**AdminUserDetail**](AdminUserDetail.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Replace the admin roles of a user

Authorization is always re-checked server-side; the Admin UI only hides actions as a convenience.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminUsersApi::class.java)
val userId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 
val adminUserRolesRequest : AdminUserRolesRequest =  // AdminUserRolesRequest | 

launch(Dispatchers.IO) {
    webService.adminUserRolesReplace(userId, adminUserRolesRequest)
}
```

### Parameters
| **userId** | **java.util.UUID**|  | |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **adminUserRolesRequest** | [**AdminUserRolesRequest**](AdminUserRolesRequest.md)|  | |

### Return type

null (empty response body)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


Unblock a user account

Blocking also revokes every active refresh session of that user.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminUsersApi::class.java)
val userId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 

launch(Dispatchers.IO) {
    val result : AdminUser = webService.adminUserUnblock(userId)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **userId** | **java.util.UUID**|  | |

### Return type

[**AdminUser**](AdminUser.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Search user accounts

Password hashes and session secret material are never returned. Newest first unless &#x60;ordering&#x60; says otherwise, in cursor pages. Every filter is optional.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminUsersApi::class.java)
val cursor : kotlin.String = cursor_example // kotlin.String | Opaque token returned as `nextCursor` by the previous page.
val id : kotlin.String = id_example // kotlin.String | One account by id. What a link to an account written before the console had cards resolves to, so it still arrives at that account alone.
val kind : kotlin.String = kind_example // kotlin.String | `owners` keeps accounts on at least one facility; `users` keeps the rest.
val limit : kotlin.Int = 56 // kotlin.Int | Page size, maximum 200, default 50.
val ordering : kotlin.String = ordering_example // kotlin.String | createdAt, -createdAt (the default), name or -name.
val q : kotlin.String = q_example // kotlin.String | Free text matched against the account name and phone number.
val role : kotlin.String = role_example // kotlin.String | Admin role id or code; keeps accounts holding that role actively. The value `any` keeps every operator, `none` every non-operator.
val status : kotlin.String = status_example // kotlin.String | `active` keeps active accounts; any other value keeps blocked accounts.

launch(Dispatchers.IO) {
    val result : AdminUserList = webService.adminUsersList(cursor, id, kind, limit, ordering, q, role, status)
}
```

### Parameters
| **cursor** | **kotlin.String**| Opaque token returned as &#x60;nextCursor&#x60; by the previous page. | [optional] |
| **id** | **kotlin.String**| One account by id. What a link to an account written before the console had cards resolves to, so it still arrives at that account alone. | [optional] |
| **kind** | **kotlin.String**| &#x60;owners&#x60; keeps accounts on at least one facility; &#x60;users&#x60; keeps the rest. | [optional] |
| **limit** | **kotlin.Int**| Page size, maximum 200, default 50. | [optional] |
| **ordering** | **kotlin.String**| createdAt, -createdAt (the default), name or -name. | [optional] |
| **q** | **kotlin.String**| Free text matched against the account name and phone number. | [optional] |
| **role** | **kotlin.String**| Admin role id or code; keeps accounts holding that role actively. The value &#x60;any&#x60; keeps every operator, &#x60;none&#x60; every non-operator. | [optional] |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **status** | **kotlin.String**| &#x60;active&#x60; keeps active accounts; any other value keeps blocked accounts. | [optional] |

### Return type

[**AdminUserList**](AdminUserList.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

