# AdminFacilitiesAPI

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**adminFacilitiesList**](AdminFacilitiesAPI.md#adminfacilitieslist) | **GET** /api/v1/admin/facilities/ | List facilities for operations
[**adminFacilityClose**](AdminFacilitiesAPI.md#adminfacilityclose) | **POST** /api/v1/admin/facilities/{facility_id}/close/ | Close a facility
[**adminFacilityReactivate**](AdminFacilitiesAPI.md#adminfacilityreactivate) | **POST** /api/v1/admin/facilities/{facility_id}/reactivate/ | Reactivate a suspended facility
[**adminFacilityRetrieve**](AdminFacilitiesAPI.md#adminfacilityretrieve) | **GET** /api/v1/admin/facilities/{facility_id}/ | Retrieve one facility
[**adminFacilitySuspend**](AdminFacilitiesAPI.md#adminfacilitysuspend) | **POST** /api/v1/admin/facilities/{facility_id}/suspend/ | Suspend a facility
[**adminFacilityTimelineRetrieve**](AdminFacilitiesAPI.md#adminfacilitytimelineretrieve) | **GET** /api/v1/admin/facilities/{facility_id}/timeline/ | Everything that happened to a facility, newest first


# **adminFacilitiesList**
```swift
    open class func adminFacilitiesList(category: String? = nil, issue: Issue_adminFacilitiesList? = nil, ordering: Ordering_adminFacilitiesList? = nil, province: String? = nil, q: String? = nil, status: String? = nil, completion: @escaping (_ data: AdminFacilityList?, _ error: Error?) -> Void)
```

List facilities for operations

Capped at 250 rows. Every filter is optional and combines with the rest. Each row carries `qualityScore` (0-100) and `qualityIssues`, computed in the same query.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let category = "category_example" // String | Category id. (optional)
let issue = "issue_example" // String | Keep facilities that have this quality issue. (optional)
let ordering = "ordering_example" // String | Sort order; the default is `-updatedAt` (most recently changed). (optional)
let province = "province_example" // String | Province id. (optional)
let q = "q_example" // String | Free text matched against the Arabic and English facility names. (optional)
let status = "status_example" // String | Facility status, for example ACTIVE or SUSPENDED. (optional)

// List facilities for operations
AdminFacilitiesAPI.adminFacilitiesList(category: category, issue: issue, ordering: ordering, province: province, q: q, status: status) { (response, error) in
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
 **issue** | **String** | Keep facilities that have this quality issue. | [optional] 
 **ordering** | **String** | Sort order; the default is &#x60;-updatedAt&#x60; (most recently changed). | [optional] 
 **province** | **String** | Province id. | [optional] 
 **q** | **String** | Free text matched against the Arabic and English facility names. | [optional] 
 **status** | **String** | Facility status, for example ACTIVE or SUSPENDED. | [optional] 

### Return type

[**AdminFacilityList**](AdminFacilityList.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminFacilityClose**
```swift
    open class func adminFacilityClose(facilityId: UUID, adminDecisionRequest: AdminDecisionRequest? = nil, completion: @escaping (_ data: AdminFacility?, _ error: Error?) -> Void)
```

Close a facility

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let facilityId = 987 // UUID | 
let adminDecisionRequest = AdminDecisionRequest(reason: "reason_example") // AdminDecisionRequest |  (optional)

// Close a facility
AdminFacilitiesAPI.adminFacilityClose(facilityId: facilityId, adminDecisionRequest: adminDecisionRequest) { (response, error) in
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
 **facilityId** | **UUID** |  | 
 **adminDecisionRequest** | [**AdminDecisionRequest**](AdminDecisionRequest.md) |  | [optional] 

### Return type

[**AdminFacility**](AdminFacility.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminFacilityReactivate**
```swift
    open class func adminFacilityReactivate(facilityId: UUID, adminDecisionRequest: AdminDecisionRequest? = nil, completion: @escaping (_ data: AdminFacility?, _ error: Error?) -> Void)
```

Reactivate a suspended facility

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let facilityId = 987 // UUID | 
let adminDecisionRequest = AdminDecisionRequest(reason: "reason_example") // AdminDecisionRequest |  (optional)

// Reactivate a suspended facility
AdminFacilitiesAPI.adminFacilityReactivate(facilityId: facilityId, adminDecisionRequest: adminDecisionRequest) { (response, error) in
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
 **facilityId** | **UUID** |  | 
 **adminDecisionRequest** | [**AdminDecisionRequest**](AdminDecisionRequest.md) |  | [optional] 

### Return type

[**AdminFacility**](AdminFacility.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminFacilityRetrieve**
```swift
    open class func adminFacilityRetrieve(facilityId: UUID, completion: @escaping (_ data: AdminFacilityQuality?, _ error: Error?) -> Void)
```

Retrieve one facility

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let facilityId = 987 // UUID | 

// Retrieve one facility
AdminFacilitiesAPI.adminFacilityRetrieve(facilityId: facilityId) { (response, error) in
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
 **facilityId** | **UUID** |  | 

### Return type

[**AdminFacilityQuality**](AdminFacilityQuality.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminFacilitySuspend**
```swift
    open class func adminFacilitySuspend(facilityId: UUID, adminDecisionRequest: AdminDecisionRequest? = nil, completion: @escaping (_ data: AdminFacility?, _ error: Error?) -> Void)
```

Suspend a facility

A suspended facility leaves public discovery and cannot self-reactivate.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let facilityId = 987 // UUID | 
let adminDecisionRequest = AdminDecisionRequest(reason: "reason_example") // AdminDecisionRequest |  (optional)

// Suspend a facility
AdminFacilitiesAPI.adminFacilitySuspend(facilityId: facilityId, adminDecisionRequest: adminDecisionRequest) { (response, error) in
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
 **facilityId** | **UUID** |  | 
 **adminDecisionRequest** | [**AdminDecisionRequest**](AdminDecisionRequest.md) |  | [optional] 

### Return type

[**AdminFacility**](AdminFacility.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminFacilityTimelineRetrieve**
```swift
    open class func adminFacilityTimelineRetrieve(facilityId: UUID, completion: @escaping (_ data: AdminTimeline?, _ error: Error?) -> Void)
```

Everything that happened to a facility, newest first

Merges applications (submitted, decided), problem reports (created, resolved or dismissed), audited changes to the facility and its applications, reports, images, evidence and duty shifts, and a summary of the next 14 days of duty. Up to 200 events.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let facilityId = 987 // UUID | 

// Everything that happened to a facility, newest first
AdminFacilitiesAPI.adminFacilityTimelineRetrieve(facilityId: facilityId) { (response, error) in
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
 **facilityId** | **UUID** |  | 

### Return type

[**AdminTimeline**](AdminTimeline.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

