# AdminDutyApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
| ------------- | ------------- | ------------- |
| [**adminDutyImport**](AdminDutyApi.md#adminDutyImport) | **POST** api/v1/admin/duty/import/ | Read a duty roster from a spreadsheet; preview it, or apply it |
| [**adminDutyRosterRetrieve**](AdminDutyApi.md#adminDutyRosterRetrieve) | **GET** api/v1/admin/duty/ | The duty roster of a province (or city), day by day |
| [**adminDutyRotationCreate**](AdminDutyApi.md#adminDutyRotationCreate) | **POST** api/v1/admin/duty/rotations/ | Save a duty rotation |
| [**adminDutyRotationDelete**](AdminDutyApi.md#adminDutyRotationDelete) | **DELETE** api/v1/admin/duty/rotations/{rotation_id}/ | Delete a saved duty rotation |
| [**adminDutyRotationGenerate**](AdminDutyApi.md#adminDutyRotationGenerate) | **POST** api/v1/admin/duty/rotations/{rotation_id}/generate/ | Generate a period&#39;s shifts from a rotation; preview them, or apply them |
| [**adminDutyRotationUpdate**](AdminDutyApi.md#adminDutyRotationUpdate) | **PATCH** api/v1/admin/duty/rotations/{rotation_id}/ | Change a saved duty rotation |
| [**adminDutyRotationsList**](AdminDutyApi.md#adminDutyRotationsList) | **GET** api/v1/admin/duty/rotations/ | Saved duty rotations |
| [**adminDutyShiftCreate**](AdminDutyApi.md#adminDutyShiftCreate) | **POST** api/v1/admin/duty/ | Put a duty shift on a pharmacy&#39;s roster |
| [**adminDutyShiftDelete**](AdminDutyApi.md#adminDutyShiftDelete) | **DELETE** api/v1/admin/duty/{shift_id}/ | Cancel a duty shift |
| [**adminDutyShiftUpdate**](AdminDutyApi.md#adminDutyShiftUpdate) | **PATCH** api/v1/admin/duty/{shift_id}/ | Move a duty shift |



Read a duty roster from a spreadsheet; preview it, or apply it

Columns in Arabic or English: the pharmacy (&#x60;facilityId&#x60;, &#x60;pharmacy&#x60;/&#x60;الصيدلية&#x60; by name, or &#x60;phone&#x60;/&#x60;الهاتف&#x60;) and either &#x60;date&#x60;/&#x60;التاريخ&#x60; with &#x60;from&#x60;/&#x60;من&#x60; and &#x60;to&#x60;/&#x60;إلى&#x60; in Damascus time (an end at or before the start is the next morning), or &#x60;startsAt&#x60; and &#x60;endsAt&#x60;. Every row is checked against the province&#39;s pharmacies and the stored shifts. &#x60;apply&#x60; writes all rows or none, and only when no row has an error; re-applying the same file changes nothing. At most 2000 rows.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminDutyApi::class.java)
val file : java.io.File = BINARY_DATA_HERE // java.io.File | CSV (UTF-8) or XLSX, first sheet, header row first.
val provinceId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 
val apply : kotlin.Boolean = true // kotlin.Boolean | False previews; true writes, refused if any row has an error.

launch(Dispatchers.IO) {
    val result : DutyImportResult = webService.adminDutyImport(file, provinceId, apply)
}
```

### Parameters
| **file** | **java.io.File**| CSV (UTF-8) or XLSX, first sheet, header row first. | |
| **provinceId** | **java.util.UUID**|  | |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **apply** | **kotlin.Boolean**| False previews; true writes, refused if any row has an error. | [optional] [default to false] |

### Return type

[**DutyImportResult**](DutyImportResult.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json


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


Save a duty rotation

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminDutyApi::class.java)
val dutyRotationRequest : DutyRotationRequest =  // DutyRotationRequest | 

launch(Dispatchers.IO) {
    val result : DutyRotation = webService.adminDutyRotationCreate(dutyRotationRequest)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **dutyRotationRequest** | [**DutyRotationRequest**](DutyRotationRequest.md)|  | |

### Return type

[**DutyRotation**](DutyRotation.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


Delete a saved duty rotation

The shifts it generated stay; only the template goes.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminDutyApi::class.java)
val rotationId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 

launch(Dispatchers.IO) {
    webService.adminDutyRotationDelete(rotationId)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **rotationId** | **java.util.UUID**|  | |

### Return type

null (empty response body)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Generate a period&#39;s shifts from a rotation; preview them, or apply them

Up to three months at a time. The same checks and all-or-nothing writing as an import; applying a period twice changes nothing.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminDutyApi::class.java)
val rotationId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 
val dutyRotationGenerate : DutyRotationGenerate =  // DutyRotationGenerate | 

launch(Dispatchers.IO) {
    val result : DutyImportResult = webService.adminDutyRotationGenerate(rotationId, dutyRotationGenerate)
}
```

### Parameters
| **rotationId** | **java.util.UUID**|  | |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **dutyRotationGenerate** | [**DutyRotationGenerate**](DutyRotationGenerate.md)|  | |

### Return type

[**DutyImportResult**](DutyImportResult.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


Change a saved duty rotation

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminDutyApi::class.java)
val rotationId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 
val patchedDutyRotationRequest : PatchedDutyRotationRequest =  // PatchedDutyRotationRequest | 

launch(Dispatchers.IO) {
    val result : DutyRotation = webService.adminDutyRotationUpdate(rotationId, patchedDutyRotationRequest)
}
```

### Parameters
| **rotationId** | **java.util.UUID**|  | |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **patchedDutyRotationRequest** | [**PatchedDutyRotationRequest**](PatchedDutyRotationRequest.md)|  | [optional] |

### Return type

[**DutyRotation**](DutyRotation.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


Saved duty rotations

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminDutyApi::class.java)

launch(Dispatchers.IO) {
    val result : DutyRotationList = webService.adminDutyRotationsList()
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**DutyRotationList**](DutyRotationList.md)

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

