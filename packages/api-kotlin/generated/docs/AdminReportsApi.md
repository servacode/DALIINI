# AdminReportsApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
| ------------- | ------------- | ------------- |
| [**adminReportDismiss**](AdminReportsApi.md#adminReportDismiss) | **POST** api/v1/admin/reports/{report_id}/dismiss/ | Dismiss a report |
| [**adminReportResolve**](AdminReportsApi.md#adminReportResolve) | **POST** api/v1/admin/reports/{report_id}/resolve/ | Mark a report resolved |
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


List facility problem reports

Newest first, capped at 250 rows.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminReportsApi::class.java)
val facility : kotlin.String = facility_example // kotlin.String | Facility id.
val status : kotlin.String = status_example // kotlin.String | OPEN, RESOLVED or DISMISSED.

launch(Dispatchers.IO) {
    val result : AdminFacilityReportList = webService.adminReportsList(facility, status)
}
```

### Parameters
| **facility** | **kotlin.String**| Facility id. | [optional] |
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

