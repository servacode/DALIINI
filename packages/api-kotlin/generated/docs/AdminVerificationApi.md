# AdminVerificationApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
| ------------- | ------------- | ------------- |
| [**adminVerificationRequirementCreate**](AdminVerificationApi.md#adminVerificationRequirementCreate) | **POST** api/v1/admin/verification-requirements/ | Create a verification requirement |
| [**adminVerificationRequirementUpdate**](AdminVerificationApi.md#adminVerificationRequirementUpdate) | **PUT** api/v1/admin/verification-requirements/{requirement_id}/ | Edit a verification requirement, or retire it |
| [**adminVerificationRequirementsList**](AdminVerificationApi.md#adminVerificationRequirementsList) | **GET** api/v1/admin/verification-requirements/ | List verification requirements |



Create a verification requirement

Requires the manage permission, which is re-checked inside the handler.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminVerificationApi::class.java)
val adminVerificationRequirementRequest : AdminVerificationRequirementRequest =  // AdminVerificationRequirementRequest | 

launch(Dispatchers.IO) {
    val result : AdminId = webService.adminVerificationRequirementCreate(adminVerificationRequirementRequest)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **adminVerificationRequirementRequest** | [**AdminVerificationRequirementRequest**](AdminVerificationRequirementRequest.md)|  | |

### Return type

[**AdminId**](AdminId.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


Edit a verification requirement, or retire it

The owning category cannot change: evidence already submitted points at a (facility, requirement) pair. Retirement is &#x60;active &#x3D; false&#x60;; there is no delete, because evidence references the row.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminVerificationApi::class.java)
val requirementId : kotlin.Int = 56 // kotlin.Int | 
val adminVerificationRequirementUpdateRequest : AdminVerificationRequirementUpdateRequest =  // AdminVerificationRequirementUpdateRequest | 

launch(Dispatchers.IO) {
    val result : AdminVerificationRequirement = webService.adminVerificationRequirementUpdate(requirementId, adminVerificationRequirementUpdateRequest)
}
```

### Parameters
| **requirementId** | **kotlin.Int**|  | |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **adminVerificationRequirementUpdateRequest** | [**AdminVerificationRequirementUpdateRequest**](AdminVerificationRequirementUpdateRequest.md)|  | [optional] |

### Return type

[**AdminVerificationRequirement**](AdminVerificationRequirement.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


List verification requirements

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminVerificationApi::class.java)

launch(Dispatchers.IO) {
    val result : AdminVerificationRequirementList = webService.adminVerificationRequirementsList()
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**AdminVerificationRequirementList**](AdminVerificationRequirementList.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

