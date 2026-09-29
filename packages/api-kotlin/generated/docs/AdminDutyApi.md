# AdminDutyApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
| ------------- | ------------- | ------------- |
| [**adminDutyRosterRetrieve**](AdminDutyApi.md#adminDutyRosterRetrieve) | **GET** api/v1/admin/duty/ | The duty roster of a province (or city), day by day |
| [**adminDutyShiftCreate**](AdminDutyApi.md#adminDutyShiftCreate) | **POST** api/v1/admin/duty/ | Put a duty shift on a pharmacy&#39;s roster |
| [**adminDutyShiftDelete**](AdminDutyApi.md#adminDutyShiftDelete) | **DELETE** api/v1/admin/duty/{shift_id}/ | Cancel a duty shift |
| [**adminDutyShiftUpdate**](AdminDutyApi.md#adminDutyShiftUpdate) | **PATCH** api/v1/admin/duty/{shift_id}/ | Move a duty shift |



The duty roster of a province (or city), day by day

Days are Damascus calendar days from &#x60;from&#x60; to &#x60;to&#x60; inclusive, by default today and the next 13 days, at most 62. Each day lists the shifts of ACTIVE duty pharmacies overlapping it, and &#x60;gap&#x60; is true when there is none: the rule behind the DUTY_GAP alert. With &#x60;cityId&#x60;, shifts and gaps are those of that city&#39;s pharmacies.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminDutyApi::class.java)
val provinceId : kotlin.String = provinceId_example // kotlin.String | 
val cityId : kotlin.String = cityId_example // kotlin.String | 
val from : kotlin.String = from_example // kotlin.String | YYYY-MM-DD
val to : kotlin.String = to_example // kotlin.String | YYYY-MM-DD

launch(Dispatchers.IO) {
    val result : AdminDutyRoster = webService.adminDutyRosterRetrieve(provinceId, cityId, from, to)
}
```

### Parameters
| **provinceId** | **kotlin.String**|  | |
| **cityId** | **kotlin.String**|  | [optional] |
| **from** | **kotlin.String**| YYYY-MM-DD | [optional] |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **to** | **kotlin.String**| YYYY-MM-DD | [optional] |

### Return type

[**AdminDutyRoster**](AdminDutyRoster.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Put a duty shift on a pharmacy&#39;s roster

Same rules as the owner endpoint: the category must support duty (409 DUTY_NOT_SUPPORTED), the shift must not overlap another of the same pharmacy or be invalid (409 DUTY_OVERLAP_OR_INVALID) and must not fall in a temporary closure (409 DUTY_DURING_CLOSURE). Recorded with &#x60;createdBy&#x60; ADMIN, audited, and the pharmacy&#39;s owners are notified. Sending a shift identical to an existing one returns it with 200 and changes nothing.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminDutyApi::class.java)
val adminDutyShiftCreateRequest : AdminDutyShiftCreateRequest =  // AdminDutyShiftCreateRequest | 

launch(Dispatchers.IO) {
    val result : AdminDutyShift = webService.adminDutyShiftCreate(adminDutyShiftCreateRequest)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **adminDutyShiftCreateRequest** | [**AdminDutyShiftCreateRequest**](AdminDutyShiftCreateRequest.md)|  | |

### Return type

[**AdminDutyShift**](AdminDutyShift.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


Cancel a duty shift

Audited; the pharmacy&#39;s owners are notified.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminDutyApi::class.java)
val shiftId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 

launch(Dispatchers.IO) {
    webService.adminDutyShiftDelete(shiftId)
}
```

### Parameters
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


Move a duty shift

Same validation as creating one. &#x60;createdBy&#x60; keeps who created the shift. Audited; the pharmacy&#39;s owners are notified when the times change.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminDutyApi::class.java)
val shiftId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 
val patchedAdminDutyShiftUpdateRequest : PatchedAdminDutyShiftUpdateRequest =  // PatchedAdminDutyShiftUpdateRequest | 

launch(Dispatchers.IO) {
    val result : AdminDutyShift = webService.adminDutyShiftUpdate(shiftId, patchedAdminDutyShiftUpdateRequest)
}
```

### Parameters
| **shiftId** | **java.util.UUID**|  | |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **patchedAdminDutyShiftUpdateRequest** | [**PatchedAdminDutyShiftUpdateRequest**](PatchedAdminDutyShiftUpdateRequest.md)|  | [optional] |

### Return type

[**AdminDutyShift**](AdminDutyShift.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

