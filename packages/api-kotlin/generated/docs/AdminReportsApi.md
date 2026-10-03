# AdminReportsApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
| ------------- | ------------- | ------------- |
| [**adminReportDismiss**](AdminReportsApi.md#adminReportDismiss) | **POST** api/v1/admin/reports/{report_id}/dismiss/ | Dismiss a report |
| [**adminReportResolve**](AdminReportsApi.md#adminReportResolve) | **POST** api/v1/admin/reports/{report_id}/resolve/ | Mark a report resolved |
| [**adminReportsBulkDecide**](AdminReportsApi.md#adminReportsBulkDecide) | **POST** api/v1/admin/reports/bulk/ | Resolve or dismiss many reports at once |
| [**adminReportsList**](AdminReportsApi.md#adminReportsList) | **GET** api/v1/admin/reports/ | List facility problem reports |



Dismiss a report

Only OPEN reports can be decided; the decision is audited.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminReportsApi::class.java)
val reportId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 
val adminReportDecisionRequest : AdminReportDecisionRequest =  // AdminReportDecisionRequest | 

launch(Dispatchers.IO) {
    val result : AdminFacilityReport = webService.adminReportDismiss(reportId, adminReportDecisionRequest)
}
```

### Parameters
| **reportId** | **java.util.UUID**|  | |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **adminReportDecisionRequest** | [**AdminReportDecisionRequest**](AdminReportDecisionRequest.md)|  | [optional] |

### Return type

[**AdminFacilityReport**](AdminFacilityReport.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


Mark a report resolved

Only OPEN reports can be decided; the decision is audited.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminReportsApi::class.java)
val reportId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 
val adminReportDecisionRequest : AdminReportDecisionRequest =  // AdminReportDecisionRequest | 

launch(Dispatchers.IO) {
    val result : AdminFacilityReport = webService.adminReportResolve(reportId, adminReportDecisionRequest)
}
```

### Parameters
| **reportId** | **java.util.UUID**|  | |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **adminReportDecisionRequest** | [**AdminReportDecisionRequest**](AdminReportDecisionRequest.md)|  | [optional] |

### Return type

[**AdminFacilityReport**](AdminFacilityReport.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


Resolve or dismiss many reports at once

Up to 100 ids, in one transaction: every OPEN report is decided and audited individually, exactly as the single-report endpoints do. An id that does not exist or is no longer OPEN is reported per id (NOT_FOUND, NOT_OPEN) and left alone; it does not fail the others.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminReportsApi::class.java)
val adminReportBulkRequest : AdminReportBulkRequest =  // AdminReportBulkRequest | 

launch(Dispatchers.IO) {
    val result : AdminReportBulkResponse = webService.adminReportsBulkDecide(adminReportBulkRequest)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **adminReportBulkRequest** | [**AdminReportBulkRequest**](AdminReportBulkRequest.md)|  | |

### Return type

[**AdminReportBulkResponse**](AdminReportBulkResponse.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


List facility problem reports

Newest first, in cursor pages.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminReportsApi::class.java)
val cursor : kotlin.String = cursor_example // kotlin.String | Opaque token returned as `nextCursor` by the previous page.
val facility : kotlin.String = facility_example // kotlin.String | Facility id.
val limit : kotlin.Int = 56 // kotlin.Int | Page size, maximum 200, default 50.
val status : kotlin.String = status_example // kotlin.String | OPEN, RESOLVED or DISMISSED.

launch(Dispatchers.IO) {
    val result : AdminFacilityReportList = webService.adminReportsList(cursor, facility, limit, status)
}
```

### Parameters
| **cursor** | **kotlin.String**| Opaque token returned as &#x60;nextCursor&#x60; by the previous page. | [optional] |
| **facility** | **kotlin.String**| Facility id. | [optional] |
| **limit** | **kotlin.Int**| Page size, maximum 200, default 50. | [optional] |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **status** | **kotlin.String**| OPEN, RESOLVED or DISMISSED. | [optional] |

### Return type

[**AdminFacilityReportList**](AdminFacilityReportList.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

