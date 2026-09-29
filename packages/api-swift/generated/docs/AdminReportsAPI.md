# AdminReportsAPI

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**adminReportDismiss**](AdminReportsAPI.md#adminreportdismiss) | **POST** /api/v1/admin/reports/{report_id}/dismiss/ | Dismiss a report
[**adminReportResolve**](AdminReportsAPI.md#adminreportresolve) | **POST** /api/v1/admin/reports/{report_id}/resolve/ | Mark a report resolved
[**adminReportsBulkDecide**](AdminReportsAPI.md#adminreportsbulkdecide) | **POST** /api/v1/admin/reports/bulk/ | Resolve or dismiss many reports at once
[**adminReportsList**](AdminReportsAPI.md#adminreportslist) | **GET** /api/v1/admin/reports/ | List facility problem reports


# **adminReportDismiss**
```swift
    open class func adminReportDismiss(reportId: UUID, adminReportDecisionRequest: AdminReportDecisionRequest? = nil, completion: @escaping (_ data: AdminFacilityReport?, _ error: Error?) -> Void)
```

Dismiss a report

Only OPEN reports can be decided; the decision is audited.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let reportId = 987 // UUID | 
let adminReportDecisionRequest = AdminReportDecisionRequest(note: "note_example") // AdminReportDecisionRequest |  (optional)

// Dismiss a report
AdminReportsAPI.adminReportDismiss(reportId: reportId, adminReportDecisionRequest: adminReportDecisionRequest) { (response, error) in
    guard error == nil else {
        print(error)
        return
    }

    if (response) {
        dump(response)
    }
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **reportId** | **UUID** |  | 
 **adminReportDecisionRequest** | [**AdminReportDecisionRequest**](AdminReportDecisionRequest.md) |  | [optional] 

### Return type

[**AdminFacilityReport**](AdminFacilityReport.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminReportResolve**
```swift
    open class func adminReportResolve(reportId: UUID, adminReportDecisionRequest: AdminReportDecisionRequest? = nil, completion: @escaping (_ data: AdminFacilityReport?, _ error: Error?) -> Void)
```

Mark a report resolved

Only OPEN reports can be decided; the decision is audited.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let reportId = 987 // UUID | 
let adminReportDecisionRequest = AdminReportDecisionRequest(note: "note_example") // AdminReportDecisionRequest |  (optional)

// Mark a report resolved
AdminReportsAPI.adminReportResolve(reportId: reportId, adminReportDecisionRequest: adminReportDecisionRequest) { (response, error) in
    guard error == nil else {
        print(error)
        return
    }

    if (response) {
        dump(response)
    }
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **reportId** | **UUID** |  | 
 **adminReportDecisionRequest** | [**AdminReportDecisionRequest**](AdminReportDecisionRequest.md) |  | [optional] 

### Return type

[**AdminFacilityReport**](AdminFacilityReport.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminReportsBulkDecide**
```swift
    open class func adminReportsBulkDecide(adminReportBulkRequest: AdminReportBulkRequest, completion: @escaping (_ data: AdminReportBulkResponse?, _ error: Error?) -> Void)
```

Resolve or dismiss many reports at once

Up to 100 ids, in one transaction: every OPEN report is decided and audited individually, exactly as the single-report endpoints do. An id that does not exist or is no longer OPEN is reported per id (NOT_FOUND, NOT_OPEN) and left alone; it does not fail the others.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let adminReportBulkRequest = AdminReportBulkRequest(ids: [123], action: AdminReportBulkActionEnum(), note: "note_example") // AdminReportBulkRequest | 

// Resolve or dismiss many reports at once
AdminReportsAPI.adminReportsBulkDecide(adminReportBulkRequest: adminReportBulkRequest) { (response, error) in
    guard error == nil else {
        print(error)
        return
    }

    if (response) {
        dump(response)
    }
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **adminReportBulkRequest** | [**AdminReportBulkRequest**](AdminReportBulkRequest.md) |  | 

### Return type

[**AdminReportBulkResponse**](AdminReportBulkResponse.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminReportsList**
```swift
    open class func adminReportsList(facility: String? = nil, status: String? = nil, completion: @escaping (_ data: AdminFacilityReportList?, _ error: Error?) -> Void)
```

List facility problem reports

Newest first, capped at 250 rows.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let facility = "facility_example" // String | Facility id. (optional)
let status = "status_example" // String | OPEN, RESOLVED or DISMISSED. (optional)

// List facility problem reports
AdminReportsAPI.adminReportsList(facility: facility, status: status) { (response, error) in
    guard error == nil else {
        print(error)
        return
    }

    if (response) {
        dump(response)
    }
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **facility** | **String** | Facility id. | [optional] 
 **status** | **String** | OPEN, RESOLVED or DISMISSED. | [optional] 

### Return type

[**AdminFacilityReportList**](AdminFacilityReportList.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

