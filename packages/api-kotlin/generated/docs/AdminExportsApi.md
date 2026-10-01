# AdminExportsApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
| ------------- | ------------- | ------------- |
| [**adminExportAuditCsv**](AdminExportsApi.md#adminExportAuditCsv) | **GET** api/v1/admin/exports/audit.csv | Export the audit trail as CSV |
| [**adminExportFacilitiesCsv**](AdminExportsApi.md#adminExportFacilitiesCsv) | **GET** api/v1/admin/exports/facilities.csv | Export the facility list as CSV |
| [**adminExportReportsCsv**](AdminExportsApi.md#adminExportReportsCsv) | **GET** api/v1/admin/exports/reports.csv | Export problem reports as CSV |



Export the audit trail as CSV

Same filters as the audit search, newest first, without its 250 cap. Snapshots are left out; metadata is included as recorded (already redacted).

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminExportsApi::class.java)
val action : kotlin.String = action_example // kotlin.String | Substring of the action code.
val actor : kotlin.String = actor_example // kotlin.String | Actor user id.
val format : kotlin.String = format_example // kotlin.String | 
val from : kotlin.String = from_example // kotlin.String | ISO date or datetime.
val requestId : kotlin.String = requestId_example // kotlin.String | Exact request correlation id.
val resource : kotlin.String = resource_example // kotlin.String | Substring of the target type, or an exact target id.
val to : kotlin.String = to_example // kotlin.String | ISO date or datetime; a bare date includes that whole day.

launch(Dispatchers.IO) {
    val result : kotlin.String = webService.adminExportAuditCsv(action, actor, format, from, requestId, resource, to)
}
```

### Parameters
| **action** | **kotlin.String**| Substring of the action code. | [optional] |
| **actor** | **kotlin.String**| Actor user id. | [optional] |
| **format** | **kotlin.String**|  | [optional] [enum: csv, json] |
| **from** | **kotlin.String**| ISO date or datetime. | [optional] |
| **requestId** | **kotlin.String**| Exact request correlation id. | [optional] |
| **resource** | **kotlin.String**| Substring of the target type, or an exact target id. | [optional] |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **to** | **kotlin.String**| ISO date or datetime; a bare date includes that whole day. | [optional] |

### Return type

**kotlin.String**

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: text/csv, application/json


Export the facility list as CSV

Same filters and ordering as the facility list, without its 250 cap.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminExportsApi::class.java)
val category : kotlin.String = category_example // kotlin.String | Category id.
val format : kotlin.String = format_example // kotlin.String | 
val issue : kotlin.String = issue_example // kotlin.String | One of NO_PHOTOS, NO_HOURS, NO_LOCATION, NO_PHONE, STALE, OPEN_REPORTS, NOT_VERIFIED_RECENTLY.
val ordering : kotlin.String = ordering_example // kotlin.String | qualityScore, -qualityScore, updatedAt or -updatedAt.
val province : kotlin.String = province_example // kotlin.String | Province id.
val q : kotlin.String = q_example // kotlin.String | Free text matched against the facility names.
val status : kotlin.String = status_example // kotlin.String | Facility status.

launch(Dispatchers.IO) {
    val result : kotlin.String = webService.adminExportFacilitiesCsv(category, format, issue, ordering, province, q, status)
}
```

### Parameters
| **category** | **kotlin.String**| Category id. | [optional] |
| **format** | **kotlin.String**|  | [optional] [enum: csv, json] |
| **issue** | **kotlin.String**| One of NO_PHOTOS, NO_HOURS, NO_LOCATION, NO_PHONE, STALE, OPEN_REPORTS, NOT_VERIFIED_RECENTLY. | [optional] |
| **ordering** | **kotlin.String**| qualityScore, -qualityScore, updatedAt or -updatedAt. | [optional] |
| **province** | **kotlin.String**| Province id. | [optional] |
| **q** | **kotlin.String**| Free text matched against the facility names. | [optional] |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **status** | **kotlin.String**| Facility status. | [optional] |

### Return type

**kotlin.String**

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: text/csv, application/json


Export problem reports as CSV

Same filters as the report list, newest first, without its 250 cap.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminExportsApi::class.java)
val facility : kotlin.String = facility_example // kotlin.String | Facility id.
val format : kotlin.String = format_example // kotlin.String | 
val status : kotlin.String = status_example // kotlin.String | OPEN, RESOLVED or DISMISSED.

launch(Dispatchers.IO) {
    val result : kotlin.String = webService.adminExportReportsCsv(facility, format, status)
}
```

### Parameters
| **facility** | **kotlin.String**| Facility id. | [optional] |
| **format** | **kotlin.String**|  | [optional] [enum: csv, json] |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **status** | **kotlin.String**| OPEN, RESOLVED or DISMISSED. | [optional] |

### Return type

**kotlin.String**

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: text/csv, application/json

