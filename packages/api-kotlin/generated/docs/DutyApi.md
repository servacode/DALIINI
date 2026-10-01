# DutyApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
| ------------- | ------------- | ------------- |
| [**ownerFacilityDutyCreate**](DutyApi.md#ownerFacilityDutyCreate) | **POST** api/v1/owner/facilities/{facility_id}/duty/ | Schedule a duty shift |
| [**ownerFacilityDutyDelete**](DutyApi.md#ownerFacilityDutyDelete) | **DELETE** api/v1/owner/facilities/{facility_id}/duty/{shift_id}/ | Remove a duty shift |
| [**ownerFacilityDutyList**](DutyApi.md#ownerFacilityDutyList) | **GET** api/v1/owner/facilities/{facility_id}/duty/ | List duty shifts of a facility |
| [**ownerFacilityDutyUpdate**](DutyApi.md#ownerFacilityDutyUpdate) | **PATCH** api/v1/owner/facilities/{facility_id}/duty/{shift_id}/ | Adjust a duty shift |



Schedule a duty shift

Overlapping shifts for the same facility are refused by a PostgreSQL exclusion constraint, not only by application code. Only categories that declare the duty capability accept this, and a shift may not overlap a temporary closure of the facility (409 DUTY_DURING_CLOSURE).

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(DutyApi::class.java)
val facilityId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 
val dutyShiftInput : DutyShiftInput =  // DutyShiftInput | 

launch(Dispatchers.IO) {
    val result : DutyShift = webService.ownerFacilityDutyCreate(facilityId, dutyShiftInput)
}
```

### Parameters
| **facilityId** | **java.util.UUID**|  | |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **dutyShiftInput** | [**DutyShiftInput**](DutyShiftInput.md)|  | |

### Return type

[**DutyShift**](DutyShift.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


Remove a duty shift

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(DutyApi::class.java)
val facilityId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 
val shiftId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 

launch(Dispatchers.IO) {
    webService.ownerFacilityDutyDelete(facilityId, shiftId)
}
```

### Parameters
| **facilityId** | **java.util.UUID**|  | |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **shiftId** | **java.util.UUID**|  | |

### Return type

null (empty response body)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


List duty shifts of a facility

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(DutyApi::class.java)
val facilityId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 

launch(Dispatchers.IO) {
    val result : DutyShiftList = webService.ownerFacilityDutyList(facilityId)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **facilityId** | **java.util.UUID**|  | |

### Return type

[**DutyShiftList**](DutyShiftList.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Adjust a duty shift

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(DutyApi::class.java)
val facilityId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 
val shiftId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 
val patchedDutyShiftInput : PatchedDutyShiftInput =  // PatchedDutyShiftInput | 

launch(Dispatchers.IO) {
    val result : DutyShift = webService.ownerFacilityDutyUpdate(facilityId, shiftId, patchedDutyShiftInput)
}
```

### Parameters
| **facilityId** | **java.util.UUID**|  | |
| **shiftId** | **java.util.UUID**|  | |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **patchedDutyShiftInput** | [**PatchedDutyShiftInput**](PatchedDutyShiftInput.md)|  | [optional] |

### Return type

[**DutyShift**](DutyShift.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

