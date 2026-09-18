# AdminFacilitiesApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
| ------------- | ------------- | ------------- |
| [**adminFacilitiesList**](AdminFacilitiesApi.md#adminFacilitiesList) | **GET** api/v1/admin/facilities/ | List facilities for operations |
| [**adminFacilityClose**](AdminFacilitiesApi.md#adminFacilityClose) | **POST** api/v1/admin/facilities/{facility_id}/close/ | Close a facility |
| [**adminFacilityReactivate**](AdminFacilitiesApi.md#adminFacilityReactivate) | **POST** api/v1/admin/facilities/{facility_id}/reactivate/ | Reactivate a suspended facility |
| [**adminFacilityRetrieve**](AdminFacilitiesApi.md#adminFacilityRetrieve) | **GET** api/v1/admin/facilities/{facility_id}/ | Retrieve one facility |
| [**adminFacilitySuspend**](AdminFacilitiesApi.md#adminFacilitySuspend) | **POST** api/v1/admin/facilities/{facility_id}/suspend/ | Suspend a facility |



List facilities for operations

Capped at 250 rows. Every filter is optional and combines with the rest.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminFacilitiesApi::class.java)
val category : kotlin.String = category_example // kotlin.String | Category id.
val province : kotlin.String = province_example // kotlin.String | Province id.
val q : kotlin.String = q_example // kotlin.String | Free text matched against the Arabic and English facility names.
val status : kotlin.String = status_example // kotlin.String | Facility status, for example ACTIVE or SUSPENDED.

launch(Dispatchers.IO) {
    val result : AdminFacilityList = webService.adminFacilitiesList(category, province, q, status)
}
```

### Parameters
| **category** | **kotlin.String**| Category id. | [optional] |
| **province** | **kotlin.String**| Province id. | [optional] |
| **q** | **kotlin.String**| Free text matched against the Arabic and English facility names. | [optional] |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **status** | **kotlin.String**| Facility status, for example ACTIVE or SUSPENDED. | [optional] |

### Return type

[**AdminFacilityList**](AdminFacilityList.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Close a facility

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminFacilitiesApi::class.java)
val facilityId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 
val adminDecisionRequest : AdminDecisionRequest =  // AdminDecisionRequest | 

launch(Dispatchers.IO) {
    val result : AdminFacility = webService.adminFacilityClose(facilityId, adminDecisionRequest)
}
```

### Parameters
| **facilityId** | **java.util.UUID**|  | |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **adminDecisionRequest** | [**AdminDecisionRequest**](AdminDecisionRequest.md)|  | [optional] |

### Return type

[**AdminFacility**](AdminFacility.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


Reactivate a suspended facility

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminFacilitiesApi::class.java)
val facilityId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 
val adminDecisionRequest : AdminDecisionRequest =  // AdminDecisionRequest | 

launch(Dispatchers.IO) {
    val result : AdminFacility = webService.adminFacilityReactivate(facilityId, adminDecisionRequest)
}
```

### Parameters
| **facilityId** | **java.util.UUID**|  | |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **adminDecisionRequest** | [**AdminDecisionRequest**](AdminDecisionRequest.md)|  | [optional] |

### Return type

[**AdminFacility**](AdminFacility.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


Retrieve one facility

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminFacilitiesApi::class.java)
val facilityId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 

launch(Dispatchers.IO) {
    val result : AdminFacility = webService.adminFacilityRetrieve(facilityId)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **facilityId** | **java.util.UUID**|  | |

### Return type

[**AdminFacility**](AdminFacility.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Suspend a facility

A suspended facility leaves public discovery and cannot self-reactivate.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminFacilitiesApi::class.java)
val facilityId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 
val adminDecisionRequest : AdminDecisionRequest =  // AdminDecisionRequest | 

launch(Dispatchers.IO) {
    val result : AdminFacility = webService.adminFacilitySuspend(facilityId, adminDecisionRequest)
}
```

### Parameters
| **facilityId** | **java.util.UUID**|  | |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **adminDecisionRequest** | [**AdminDecisionRequest**](AdminDecisionRequest.md)|  | [optional] |

### Return type

[**AdminFacility**](AdminFacility.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

