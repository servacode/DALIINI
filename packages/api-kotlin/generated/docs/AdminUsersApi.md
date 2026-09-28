# AdminUsersApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
| ------------- | ------------- | ------------- |
| [**adminRolesList**](AdminUsersApi.md#adminRolesList) | **GET** api/v1/admin/roles/ | List admin roles and their permission codes |
| [**adminUserBlock**](AdminUsersApi.md#adminUserBlock) | **POST** api/v1/admin/users/{user_id}/block/ | Block a user account |
| [**adminUserRetrieve**](AdminUsersApi.md#adminUserRetrieve) | **GET** api/v1/admin/users/{user_id}/ | Retrieve one user with the roles assigned |
| [**adminUserRolesReplace**](AdminUsersApi.md#adminUserRolesReplace) | **PUT** api/v1/admin/users/{user_id}/roles/ | Replace the admin roles of a user |
| [**adminUserUnblock**](AdminUsersApi.md#adminUserUnblock) | **POST** api/v1/admin/users/{user_id}/unblock/ | Unblock a user account |
| [**adminUsersList**](AdminUsersApi.md#adminUsersList) | **GET** api/v1/admin/users/ | Search user accounts |



List admin roles and their permission codes

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

Password hashes and session secret material are never returned. Capped at 250 rows. Both filters are optional.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminUsersApi::class.java)
val q : kotlin.String = q_example // kotlin.String | Free text matched against the account name and phone number.
val role : kotlin.String = role_example // kotlin.String | Admin role id or code; keeps accounts holding that role actively. The value `any` keeps every operator, `none` every non-operator.
val status : kotlin.String = status_example // kotlin.String | `active` keeps active accounts; any other value keeps blocked accounts.

launch(Dispatchers.IO) {
    val result : AdminUserList = webService.adminUsersList(q, role, status)
}
```

### Parameters
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

