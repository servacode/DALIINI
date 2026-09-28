# AdminFacilitiesApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
| ------------- | ------------- | ------------- |
| [**adminFacilitiesList**](AdminFacilitiesApi.md#adminFacilitiesList) | **GET** api/v1/admin/facilities/ | List facilities for operations |
| [**adminFacilityClose**](AdminFacilitiesApi.md#adminFacilityClose) | **POST** api/v1/admin/facilities/{facility_id}/close/ | Close a facility |
| [**adminFacilityReactivate**](AdminFacilitiesApi.md#adminFacilityReactivate) | **POST** api/v1/admin/facilities/{facility_id}/reactivate/ | Reactivate a suspended facility |
| [**adminFacilityRetrieve**](AdminFacilitiesApi.md#adminFacilityRetrieve) | **GET** api/v1/admin/facilities/{facility_id}/ | Retrieve one facility |
| [**adminFacilitySuspend**](AdminFacilitiesApi.md#adminFacilitySuspend) | **POST** api/v1/admin/facilities/{facility_id}/suspend/ | Suspend a facility |
| [**adminFacilityTimelineRetrieve**](AdminFacilitiesApi.md#adminFacilityTimelineRetrieve) | **GET** api/v1/admin/facilities/{facility_id}/timeline/ | Everything that happened to a facility, newest first |



List facilities for operations

Capped at 250 rows. Every filter is optional and combines with the rest. Each row carries &#x60;qualityScore&#x60; (0-100) and &#x60;qualityIssues&#x60;, computed in the same query.

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
val issue : kotlin.String = issue_example // kotlin.String | Keep facilities that have this quality issue.
val ordering : kotlin.String = ordering_example // kotlin.String | Sort order; the default is `-updatedAt` (most recently changed).
val province : kotlin.String = province_example // kotlin.String | Province id.
val q : kotlin.String = q_example // kotlin.String | Free text matched against the Arabic and English facility names.
val status : kotlin.String = status_example // kotlin.String | Facility status, for example ACTIVE or SUSPENDED.

launch(Dispatchers.IO) {
    val result : AdminFacilityList = webService.adminFacilitiesList(category, issue, ordering, province, q, status)
}
```

### Parameters
| **category** | **kotlin.String**| Category id. | [optional] |
| **issue** | **kotlin.String**| Keep facilities that have this quality issue. | [optional] [enum: NOT_VERIFIED_RECENTLY, NO_HOURS, NO_LOCATION, NO_PHONE, NO_PHOTOS, OPEN_REPORTS, STALE] |
| **ordering** | **kotlin.String**| Sort order; the default is &#x60;-updatedAt&#x60; (most recently changed). | [optional] [enum: -qualityScore, -updatedAt, qualityScore, updatedAt] |
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
    val result : AdminFacilityQuality = webService.adminFacilityRetrieve(facilityId)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **facilityId** | **java.util.UUID**|  | |

### Return type

[**AdminFacilityQuality**](AdminFacilityQuality.md)

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


Everything that happened to a facility, newest first

Merges applications (submitted, decided), problem reports (created, resolved or dismissed), audited changes to the facility and its applications, reports, images, evidence and duty shifts, and a summary of the next 14 days of duty. Up to 200 events.

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
    val result : AdminTimeline = webService.adminFacilityTimelineRetrieve(facilityId)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **facilityId** | **java.util.UUID**|  | |

### Return type

[**AdminTimeline**](AdminTimeline.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

