# AccountApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
| ------------- | ------------- | ------------- |
| [**accountDeletionRequestCreate**](AccountApi.md#accountDeletionRequestCreate) | **POST** api/v1/account/deletion-request/ | Request deletion of the account of the caller |
| [**accountProfileRetrieve**](AccountApi.md#accountProfileRetrieve) | **GET** api/v1/account/profile/ | Retrieve the profile of the caller |
| [**accountProfileUpdate**](AccountApi.md#accountProfileUpdate) | **PATCH** api/v1/account/profile/ | Update the display name or profile province of the caller |
| [**accountPushTokenRegister**](AccountApi.md#accountPushTokenRegister) | **PUT** api/v1/account/push-token/ | Register or refresh this device&#39;s push token |
| [**accountPushTokenUnregister**](AccountApi.md#accountPushTokenUnregister) | **POST** api/v1/account/push-token/unregister/ | Stop sending pushes to a device token |
| [**accountRatingsList**](AccountApi.md#accountRatingsList) | **GET** api/v1/account/ratings/ | List the ratings written by the caller |



Request deletion of the account of the caller

Required by Play policy for any app that creates accounts. Ownership obligations and legally retained records are handled by the deletion policy.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AccountApi::class.java)
val deletionRequest : DeletionRequest =  // DeletionRequest | 

launch(Dispatchers.IO) {
    val result : AccountDeletionRequested = webService.accountDeletionRequestCreate(deletionRequest)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **deletionRequest** | [**DeletionRequest**](DeletionRequest.md)|  | |

### Return type

[**AccountDeletionRequested**](AccountDeletionRequested.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


Retrieve the profile of the caller

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AccountApi::class.java)

launch(Dispatchers.IO) {
    val result : Profile = webService.accountProfileRetrieve()
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**Profile**](Profile.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Update the display name or profile province of the caller

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AccountApi::class.java)
val patchedProfilePatch : PatchedProfilePatch =  // PatchedProfilePatch | 

launch(Dispatchers.IO) {
    val result : Profile = webService.accountProfileUpdate(patchedProfilePatch)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **patchedProfilePatch** | [**PatchedProfilePatch**](PatchedProfilePatch.md)|  | [optional] |

### Return type

[**Profile**](Profile.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


Register or refresh this device&#39;s push token

Idempotent. A new token from the same session replaces the previous one. The token is tied to the calling session and deactivated when that session ends.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AccountApi::class.java)
val pushTokenRegister : PushTokenRegister =  // PushTokenRegister | 

launch(Dispatchers.IO) {
    webService.accountPushTokenRegister(pushTokenRegister)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **pushTokenRegister** | [**PushTokenRegister**](PushTokenRegister.md)|  | |

### Return type

null (empty response body)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


Stop sending pushes to a device token

Idempotent: an unknown or already inactive token also answers 204.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AccountApi::class.java)
val pushToken : PushToken =  // PushToken | 

launch(Dispatchers.IO) {
    webService.accountPushTokenUnregister(pushToken)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **pushToken** | [**PushToken**](PushToken.md)|  | |

### Return type

null (empty response body)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


List the ratings written by the caller

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AccountApi::class.java)

launch(Dispatchers.IO) {
    val result : AccountRatingList = webService.accountRatingsList()
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**AccountRatingList**](AccountRatingList.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

