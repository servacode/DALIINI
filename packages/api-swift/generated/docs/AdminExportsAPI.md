# AdminExportsAPI

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**adminExportAuditCsv**](AdminExportsAPI.md#adminexportauditcsv) | **GET** /api/v1/admin/exports/audit.csv | Export the audit trail as CSV
[**adminExportFacilitiesCsv**](AdminExportsAPI.md#adminexportfacilitiescsv) | **GET** /api/v1/admin/exports/facilities.csv | Export the facility list as CSV
[**adminExportReportsCsv**](AdminExportsAPI.md#adminexportreportscsv) | **GET** /api/v1/admin/exports/reports.csv | Export problem reports as CSV


# **adminExportAuditCsv**
```swift
    open class func adminExportAuditCsv(action: String? = nil, actor: String? = nil, format: Format_adminExportAuditCsv? = nil, from: String? = nil, requestId: String? = nil, resource: String? = nil, to: String? = nil, completion: @escaping (_ data: String?, _ error: Error?) -> Void)
```

Export the audit trail as CSV

Same filters as the audit search, newest first, without its 250 cap. Snapshots are left out; metadata is included as recorded (already redacted).

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let action = "action_example" // String | Substring of the action code. (optional)
let actor = "actor_example" // String | Actor user id. (optional)
let format = "format_example" // String |  (optional)
let from = "from_example" // String | ISO date or datetime. (optional)
let requestId = "requestId_example" // String | Exact request correlation id. (optional)
let resource = "resource_example" // String | Substring of the target type, or an exact target id. (optional)
let to = "to_example" // String | ISO date or datetime; a bare date includes that whole day. (optional)

// Export the audit trail as CSV
AdminExportsAPI.adminExportAuditCsv(action: action, actor: actor, format: format, from: from, requestId: requestId, resource: resource, to: to) { (response, error) in
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
 **action** | **String** | Substring of the action code. | [optional] 
 **actor** | **String** | Actor user id. | [optional] 
 **format** | **String** |  | [optional] 
 **from** | **String** | ISO date or datetime. | [optional] 
 **requestId** | **String** | Exact request correlation id. | [optional] 
 **resource** | **String** | Substring of the target type, or an exact target id. | [optional] 
 **to** | **String** | ISO date or datetime; a bare date includes that whole day. | [optional] 

### Return type

**String**

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: text/csv, application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminExportFacilitiesCsv**
```swift
    open class func adminExportFacilitiesCsv(category: String? = nil, format: Format_adminExportFacilitiesCsv? = nil, issue: String? = nil, ordering: String? = nil, province: String? = nil, q: String? = nil, status: String? = nil, completion: @escaping (_ data: String?, _ error: Error?) -> Void)
```

Export the facility list as CSV

Same filters and ordering as the facility list, without its 250 cap.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let category = "category_example" // String | Category id. (optional)
let format = "format_example" // String |  (optional)
let issue = "issue_example" // String | One of NO_PHOTOS, NO_HOURS, NO_LOCATION, NO_PHONE, STALE, OPEN_REPORTS, NOT_VERIFIED_RECENTLY. (optional)
let ordering = "ordering_example" // String | qualityScore, -qualityScore, updatedAt or -updatedAt. (optional)
let province = "province_example" // String | Province id. (optional)
let q = "q_example" // String | Free text matched against the facility names. (optional)
let status = "status_example" // String | Facility status. (optional)

// Export the facility list as CSV
AdminExportsAPI.adminExportFacilitiesCsv(category: category, format: format, issue: issue, ordering: ordering, province: province, q: q, status: status) { (response, error) in
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
 **category** | **String** | Category id. | [optional] 
 **format** | **String** |  | [optional] 
 **issue** | **String** | One of NO_PHOTOS, NO_HOURS, NO_LOCATION, NO_PHONE, STALE, OPEN_REPORTS, NOT_VERIFIED_RECENTLY. | [optional] 
 **ordering** | **String** | qualityScore, -qualityScore, updatedAt or -updatedAt. | [optional] 
 **province** | **String** | Province id. | [optional] 
 **q** | **String** | Free text matched against the facility names. | [optional] 
 **status** | **String** | Facility status. | [optional] 

### Return type

**String**

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: text/csv, application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminExportReportsCsv**
```swift
    open class func adminExportReportsCsv(facility: String? = nil, format: Format_adminExportReportsCsv? = nil, status: String? = nil, completion: @escaping (_ data: String?, _ error: Error?) -> Void)
```

Export problem reports as CSV

Same filters as the report list, newest first, without its 250 cap.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let facility = "facility_example" // String | Facility id. (optional)
let format = "format_example" // String |  (optional)
let status = "status_example" // String | OPEN, RESOLVED or DISMISSED. (optional)

// Export problem reports as CSV
AdminExportsAPI.adminExportReportsCsv(facility: facility, format: format, status: status) { (response, error) in
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
 **format** | **String** |  | [optional] 
 **status** | **String** | OPEN, RESOLVED or DISMISSED. | [optional] 

### Return type

**String**

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: text/csv, application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

