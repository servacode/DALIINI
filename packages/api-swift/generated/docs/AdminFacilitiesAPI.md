# AdminFacilitiesAPI

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**adminFacilitiesList**](AdminFacilitiesAPI.md#adminfacilitieslist) | **GET** /api/v1/admin/facilities/ | List facilities for operations
[**adminFacilitiesMap**](AdminFacilitiesAPI.md#adminfacilitiesmap) | **GET** /api/v1/admin/facilities/map/ | Located facilities as map points, with the same filters as the list
[**adminFacilityClose**](AdminFacilitiesAPI.md#adminfacilityclose) | **POST** /api/v1/admin/facilities/{facility_id}/close/ | Close a facility
[**adminFacilityCreate**](AdminFacilitiesAPI.md#adminfacilitycreate) | **POST** /api/v1/admin/facilities/ | Add a facility to the directory
[**adminFacilityHoursList**](AdminFacilitiesAPI.md#adminfacilityhourslist) | **GET** /api/v1/admin/facilities/{facility_id}/hours/ | A facility&#39;s weekly opening hours
[**adminFacilityHoursReplace**](AdminFacilitiesAPI.md#adminfacilityhoursreplace) | **PUT** /api/v1/admin/facilities/{facility_id}/hours/ | Replace a facility&#39;s weekly opening hours
[**adminFacilityImageCreate**](AdminFacilitiesAPI.md#adminfacilityimagecreate) | **POST** /api/v1/admin/facilities/{facility_id}/images/ | Add a public photo to a facility
[**adminFacilityImageDelete**](AdminFacilitiesAPI.md#adminfacilityimagedelete) | **DELETE** /api/v1/admin/facilities/{facility_id}/images/{image_id}/ | Remove a public photo from a facility
[**adminFacilityImagesList**](AdminFacilitiesAPI.md#adminfacilityimageslist) | **GET** /api/v1/admin/facilities/{facility_id}/images/ | A facility&#39;s public photos
[**adminFacilityOwnerTransfer**](AdminFacilitiesAPI.md#adminfacilityownertransfer) | **POST** /api/v1/admin/facilities/{facility_id}/owner/ | Move a facility to another owner
[**adminFacilityReactivate**](AdminFacilitiesAPI.md#adminfacilityreactivate) | **POST** /api/v1/admin/facilities/{facility_id}/reactivate/ | Reactivate a suspended facility
[**adminFacilityRetrieve**](AdminFacilitiesAPI.md#adminfacilityretrieve) | **GET** /api/v1/admin/facilities/{facility_id}/ | Retrieve one facility
[**adminFacilitySuspend**](AdminFacilitiesAPI.md#adminfacilitysuspend) | **POST** /api/v1/admin/facilities/{facility_id}/suspend/ | Suspend a facility
[**adminFacilityTimelineRetrieve**](AdminFacilitiesAPI.md#adminfacilitytimelineretrieve) | **GET** /api/v1/admin/facilities/{facility_id}/timeline/ | Everything that happened to a facility, newest first
[**adminFacilityUpdate**](AdminFacilitiesAPI.md#adminfacilityupdate) | **PATCH** /api/v1/admin/facilities/{facility_id}/ | Correct a facility&#39;s details


# **adminFacilitiesList**
```swift
    open class func adminFacilitiesList(category: String? = nil, city: String? = nil, cursor: String? = nil, id: String? = nil, issue: Issue_adminFacilitiesList? = nil, limit: Int? = nil, ordering: Ordering_adminFacilitiesList? = nil, province: String? = nil, q: String? = nil, status: String? = nil, completion: @escaping (_ data: AdminFacilityList?, _ error: Error?) -> Void)
```

List facilities for operations

In cursor pages. Every filter is optional and combines with the rest. Each row carries `qualityScore` (0-100) and `qualityIssues`, computed in the same query.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let category = "category_example" // String | Category id. (optional)
let city = "city_example" // String | City id. (optional)
let cursor = "cursor_example" // String | Opaque token returned as `nextCursor` by the previous page. (optional)
let id = "id_example" // String | One facility by id: what a link written before the console had cards resolves to. (optional)
let issue = "issue_example" // String | Keep facilities that have this quality issue. (optional)
let limit = 987 // Int | Page size, maximum 200, default 50. (optional)
let ordering = "ordering_example" // String | Sort order; the default is `-updatedAt` (most recently changed). (optional)
let province = "province_example" // String | Province id. (optional)
let q = "q_example" // String | Free text matched against the Arabic and English facility names. (optional)
let status = "status_example" // String | Facility status, for example ACTIVE or SUSPENDED. (optional)

// List facilities for operations
AdminFacilitiesAPI.adminFacilitiesList(category: category, city: city, cursor: cursor, id: id, issue: issue, limit: limit, ordering: ordering, province: province, q: q, status: status) { (response, error) in
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
 **city** | **String** | City id. | [optional] 
 **cursor** | **String** | Opaque token returned as &#x60;nextCursor&#x60; by the previous page. | [optional] 
 **id** | **String** | One facility by id: what a link written before the console had cards resolves to. | [optional] 
 **issue** | **String** | Keep facilities that have this quality issue. | [optional] 
 **limit** | **Int** | Page size, maximum 200, default 50. | [optional] 
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

# **adminFacilitiesMap**
```swift
    open class func adminFacilitiesMap(category: String? = nil, city: String? = nil, issue: String? = nil, province: String? = nil, q: String? = nil, status: String? = nil, completion: @escaping (_ data: AdminFacilityMap?, _ error: Error?) -> Void)
```

Located facilities as map points, with the same filters as the list

Every located facility the filters select, as points (DECISION-075).  The same filters as the list, so \"the map of what I am looking at\" is one click. Only what a pin needs travels: the name, the state and the coordinates. A facility without a location is counted rather than dropped silently, so the operator can go and fix it.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let category = "category_example" // String | Category id. (optional)
let city = "city_example" // String | City id. (optional)
let issue = "issue_example" // String | One of NO_PHOTOS, NO_HOURS, NO_LOCATION, NO_PHONE, STALE, OPEN_REPORTS, NOT_VERIFIED_RECENTLY. (optional)
let province = "province_example" // String | Province id. (optional)
let q = "q_example" // String | Free text matched against the facility names. (optional)
let status = "status_example" // String | Facility status. (optional)

// Located facilities as map points, with the same filters as the list
AdminFacilitiesAPI.adminFacilitiesMap(category: category, city: city, issue: issue, province: province, q: q, status: status) { (response, error) in
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
 **city** | **String** | City id. | [optional] 
 **issue** | **String** | One of NO_PHOTOS, NO_HOURS, NO_LOCATION, NO_PHONE, STALE, OPEN_REPORTS, NOT_VERIFIED_RECENTLY. | [optional] 
 **province** | **String** | Province id. | [optional] 
 **q** | **String** | Free text matched against the facility names. | [optional] 
 **status** | **String** | Facility status. | [optional] 

### Return type

[**AdminFacilityMap**](AdminFacilityMap.md)

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

# **adminFacilityCreate**
```swift
    open class func adminFacilityCreate(adminFacilityCreate: AdminFacilityCreate, completion: @escaping (_ data: AdminFacilityDetail?, _ error: Error?) -> Void)
```

Add a facility to the directory

Listed by the directory itself, with no owner; an owner can claim it later. ACTIVE (the default) publishes it at once and counts as verified. The same validation as an owner's edit applies. Requires `admin.facilities.edit`; audited.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let adminFacilityCreate = AdminFacilityCreate(categoryId: 123, provinceId: 123, cityId: 123, neighborhoodId: 123, nameAr: "nameAr_example", nameEn: "nameEn_example", descriptionAr: "descriptionAr_example", descriptionEn: "descriptionEn_example", phone: "phone_example", whatsapp: "whatsapp_example", addressAr: "addressAr_example", addressEn: "addressEn_example", location: Coordinates(latitude: 123, longitude: 123), specialtyIds: [123], serviceTagIds: [123], status: AdminFacilityCreateStatusEnum()) // AdminFacilityCreate | 

// Add a facility to the directory
AdminFacilitiesAPI.adminFacilityCreate(adminFacilityCreate: adminFacilityCreate) { (response, error) in
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
 **adminFacilityCreate** | [**AdminFacilityCreate**](AdminFacilityCreate.md) |  | 

### Return type

[**AdminFacilityDetail**](AdminFacilityDetail.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminFacilityHoursList**
```swift
    open class func adminFacilityHoursList(facilityId: UUID, completion: @escaping (_ data: BusinessHoursList?, _ error: Error?) -> Void)
```

A facility's weekly opening hours

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let facilityId = 987 // UUID | 

// A facility's weekly opening hours
AdminFacilitiesAPI.adminFacilityHoursList(facilityId: facilityId) { (response, error) in
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

[**BusinessHoursList**](BusinessHoursList.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminFacilityHoursReplace**
```swift
    open class func adminFacilityHoursReplace(facilityId: UUID, businessHourInput: [BusinessHourInput], completion: @escaping (_ data: BusinessHoursList?, _ error: Error?) -> Void)
```

Replace a facility's weekly opening hours

The whole week in one call, as the owner's own route: overnight spans are allowed and same-day overlaps refused. Requires `admin.facilities.edit`.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let facilityId = 987 // UUID | 
let businessHourInput = [BusinessHourInput(weekday: 123, opensAt: "opensAt_example", closesAt: "closesAt_example", sequence: 123)] // [BusinessHourInput] | 

// Replace a facility's weekly opening hours
AdminFacilitiesAPI.adminFacilityHoursReplace(facilityId: facilityId, businessHourInput: businessHourInput) { (response, error) in
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
 **businessHourInput** | [**[BusinessHourInput]**](BusinessHourInput.md) |  | 

### Return type

[**BusinessHoursList**](BusinessHoursList.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminFacilityImageCreate**
```swift
    open class func adminFacilityImageCreate(facilityId: UUID, file: URL, completion: @escaping (_ data: OwnerFacilityImage?, _ error: Error?) -> Void)
```

Add a public photo to a facility

Multipart, through the same pipeline as the owner's upload: the file is decoded, bounded, re-encoded to JPEG, stripped and stored under a random key. Requires `admin.facilities.edit`.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let facilityId = 987 // UUID | 
let file = URL(string: "https://example.com")! // URL | 

// Add a public photo to a facility
AdminFacilitiesAPI.adminFacilityImageCreate(facilityId: facilityId, file: file) { (response, error) in
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
 **file** | **URL** |  | 

### Return type

[**OwnerFacilityImage**](OwnerFacilityImage.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminFacilityImageDelete**
```swift
    open class func adminFacilityImageDelete(facilityId: UUID, imageId: UUID, completion: @escaping (_ data: Void?, _ error: Error?) -> Void)
```

Remove a public photo from a facility

Requires `admin.facilities.edit`. The stored file goes once the row does.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let facilityId = 987 // UUID | 
let imageId = 987 // UUID | 

// Remove a public photo from a facility
AdminFacilitiesAPI.adminFacilityImageDelete(facilityId: facilityId, imageId: imageId) { (response, error) in
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
 **imageId** | **UUID** |  | 

### Return type

Void (empty response body)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminFacilityImagesList**
```swift
    open class func adminFacilityImagesList(facilityId: UUID, completion: @escaping (_ data: OwnerFacilityImageList?, _ error: Error?) -> Void)
```

A facility's public photos

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let facilityId = 987 // UUID | 

// A facility's public photos
AdminFacilitiesAPI.adminFacilityImagesList(facilityId: facilityId) { (response, error) in
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

[**OwnerFacilityImageList**](OwnerFacilityImageList.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminFacilityOwnerTransfer**
```swift
    open class func adminFacilityOwnerTransfer(facilityId: UUID, adminFacilityOwnerRequest: AdminFacilityOwnerRequest, completion: @escaping (_ data: AdminFacilityOwner?, _ error: Error?) -> Void)
```

Move a facility to another owner

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let facilityId = 987 // UUID | 
let adminFacilityOwnerRequest = AdminFacilityOwnerRequest(phone: "phone_example", keepPreviousAsManager: false) // AdminFacilityOwnerRequest | 

// Move a facility to another owner
AdminFacilitiesAPI.adminFacilityOwnerTransfer(facilityId: facilityId, adminFacilityOwnerRequest: adminFacilityOwnerRequest) { (response, error) in
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
 **adminFacilityOwnerRequest** | [**AdminFacilityOwnerRequest**](AdminFacilityOwnerRequest.md) |  | 

### Return type

[**AdminFacilityOwner**](AdminFacilityOwner.md)

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
    open class func adminFacilityRetrieve(facilityId: UUID, completion: @escaping (_ data: AdminFacilityDetail?, _ error: Error?) -> Void)
```

Retrieve one facility

Everything the console shows and edits, with the quality score.

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

[**AdminFacilityDetail**](AdminFacilityDetail.md)

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

# **adminFacilityUpdate**
```swift
    open class func adminFacilityUpdate(facilityId: UUID, patchedAdminFacilityWrite: PatchedAdminFacilityWrite? = nil, completion: @escaping (_ data: AdminFacilityDetail?, _ error: Error?) -> Void)
```

Correct a facility's details

Only the fields sent change. The status is left as it is: an operator's correction does not send a live facility back for re-verification. Moving it to another province clears its city unless one is sent; another category clears its specialties and services unless they are sent. Requires `admin.facilities.edit`; audited with both snapshots, and the owners are notified.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let facilityId = 987 // UUID | 
let patchedAdminFacilityWrite = PatchedAdminFacilityWrite(categoryId: 123, provinceId: 123, cityId: 123, neighborhoodId: 123, nameAr: "nameAr_example", nameEn: "nameEn_example", descriptionAr: "descriptionAr_example", descriptionEn: "descriptionEn_example", phone: "phone_example", whatsapp: "whatsapp_example", addressAr: "addressAr_example", addressEn: "addressEn_example", location: Coordinates(latitude: 123, longitude: 123), specialtyIds: [123], serviceTagIds: [123]) // PatchedAdminFacilityWrite |  (optional)

// Correct a facility's details
AdminFacilitiesAPI.adminFacilityUpdate(facilityId: facilityId, patchedAdminFacilityWrite: patchedAdminFacilityWrite) { (response, error) in
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
 **patchedAdminFacilityWrite** | [**PatchedAdminFacilityWrite**](PatchedAdminFacilityWrite.md) |  | [optional] 

### Return type

[**AdminFacilityDetail**](AdminFacilityDetail.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

