# AvailabilityApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
| ------------- | ------------- | ------------- |
| [**ownerFacilityHoursReplace**](AvailabilityApi.md#ownerFacilityHoursReplace) | **PUT** api/v1/owner/facilities/{facility_id}/hours/ | Replace the weekly opening hours of a facility |
| [**ownerFacilityTemporaryClosureCancel**](AvailabilityApi.md#ownerFacilityTemporaryClosureCancel) | **DELETE** api/v1/owner/facilities/{facility_id}/temporary-closures/{closure_id}/ | Cancel a temporary closure |
| [**ownerFacilityTemporaryClosureCreate**](AvailabilityApi.md#ownerFacilityTemporaryClosureCreate) | **POST** api/v1/owner/facilities/{facility_id}/temporary-closures/ | Open a temporary closure window |
| [**ownerFacilityTemporaryClosuresList**](AvailabilityApi.md#ownerFacilityTemporaryClosuresList) | **GET** api/v1/owner/facilities/{facility_id}/temporary-closures/ | List temporary closures of a facility |



Replace the weekly opening hours of a facility

The whole week is replaced in one call. Overnight spans are supported and same-day overlaps are rejected. Only categories that declare the hours capability accept this.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AvailabilityApi::class.java)
val facilityId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 
val businessHourInput : kotlin.collections.List<BusinessHourInput> =  // kotlin.collections.List<BusinessHourInput> | 

launch(Dispatchers.IO) {
    val result : BusinessHoursList = webService.ownerFacilityHoursReplace(facilityId, businessHourInput)
}
```

### Parameters
| **facilityId** | **java.util.UUID**|  | |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **businessHourInput** | [**kotlin.collections.List&lt;BusinessHourInput&gt;**](BusinessHourInput.md)|  | |

### Return type

[**BusinessHoursList**](BusinessHoursList.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


Cancel a temporary closure

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AvailabilityApi::class.java)
val closureId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 
val facilityId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 

launch(Dispatchers.IO) {
    webService.ownerFacilityTemporaryClosureCancel(closureId, facilityId)
}
```

### Parameters
| **closureId** | **java.util.UUID**|  | |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **facilityId** | **java.util.UUID**|  | |

### Return type

null (empty response body)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Open a temporary closure window

A temporary closure overrides both regular hours and duty.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AvailabilityApi::class.java)
val facilityId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 
val temporaryClosure : TemporaryClosure =  // TemporaryClosure | 

launch(Dispatchers.IO) {
    val result : TemporaryClosure = webService.ownerFacilityTemporaryClosureCreate(facilityId, temporaryClosure)
}
```

### Parameters
| **facilityId** | **java.util.UUID**|  | |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **temporaryClosure** | [**TemporaryClosure**](TemporaryClosure.md)|  | |

### Return type

[**TemporaryClosure**](TemporaryClosure.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


List temporary closures of a facility

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AvailabilityApi::class.java)
val facilityId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 

launch(Dispatchers.IO) {
    val result : TemporaryClosureList = webService.ownerFacilityTemporaryClosuresList(facilityId)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **facilityId** | **java.util.UUID**|  | |

### Return type

[**TemporaryClosureList**](TemporaryClosureList.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

