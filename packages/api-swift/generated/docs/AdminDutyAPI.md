# AdminDutyAPI

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**adminDutyRosterRetrieve**](AdminDutyAPI.md#admindutyrosterretrieve) | **GET** /api/v1/admin/duty/ | The duty roster of a province (or city), day by day
[**adminDutyShiftCreate**](AdminDutyAPI.md#admindutyshiftcreate) | **POST** /api/v1/admin/duty/ | Put a duty shift on a pharmacy&#39;s roster
[**adminDutyShiftDelete**](AdminDutyAPI.md#admindutyshiftdelete) | **DELETE** /api/v1/admin/duty/{shift_id}/ | Cancel a duty shift
[**adminDutyShiftUpdate**](AdminDutyAPI.md#admindutyshiftupdate) | **PATCH** /api/v1/admin/duty/{shift_id}/ | Move a duty shift


# **adminDutyRosterRetrieve**
```swift
    open class func adminDutyRosterRetrieve(provinceId: String, cityId: String? = nil, from: String? = nil, to: String? = nil, completion: @escaping (_ data: AdminDutyRoster?, _ error: Error?) -> Void)
```

The duty roster of a province (or city), day by day

Days are Damascus calendar days from `from` to `to` inclusive, by default today and the next 13 days, at most 62. Each day lists the shifts of ACTIVE duty pharmacies overlapping it, and `gap` is true when there is none: the rule behind the DUTY_GAP alert. With `cityId`, shifts and gaps are those of that city's pharmacies.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let provinceId = "provinceId_example" // String | 
let cityId = "cityId_example" // String |  (optional)
let from = "from_example" // String | YYYY-MM-DD (optional)
let to = "to_example" // String | YYYY-MM-DD (optional)

// The duty roster of a province (or city), day by day
AdminDutyAPI.adminDutyRosterRetrieve(provinceId: provinceId, cityId: cityId, from: from, to: to) { (response, error) in
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
 **provinceId** | **String** |  | 
 **cityId** | **String** |  | [optional] 
 **from** | **String** | YYYY-MM-DD | [optional] 
 **to** | **String** | YYYY-MM-DD | [optional] 

### Return type

[**AdminDutyRoster**](AdminDutyRoster.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminDutyShiftCreate**
```swift
    open class func adminDutyShiftCreate(adminDutyShiftCreateRequest: AdminDutyShiftCreateRequest, completion: @escaping (_ data: AdminDutyShift?, _ error: Error?) -> Void)
```

Put a duty shift on a pharmacy's roster

Same rules as the owner endpoint: the category must support duty (409 DUTY_NOT_SUPPORTED), the shift must not overlap another of the same pharmacy or be invalid (409 DUTY_OVERLAP_OR_INVALID) and must not fall in a temporary closure (409 DUTY_DURING_CLOSURE). Recorded with `createdBy` ADMIN, audited, and the pharmacy's owners are notified. Sending a shift identical to an existing one returns it with 200 and changes nothing.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let adminDutyShiftCreateRequest = AdminDutyShiftCreateRequest(facilityId: 123, startsAt: Date(), endsAt: Date()) // AdminDutyShiftCreateRequest | 

// Put a duty shift on a pharmacy's roster
AdminDutyAPI.adminDutyShiftCreate(adminDutyShiftCreateRequest: adminDutyShiftCreateRequest) { (response, error) in
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
 **adminDutyShiftCreateRequest** | [**AdminDutyShiftCreateRequest**](AdminDutyShiftCreateRequest.md) |  | 

### Return type

[**AdminDutyShift**](AdminDutyShift.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminDutyShiftDelete**
```swift
    open class func adminDutyShiftDelete(shiftId: UUID, completion: @escaping (_ data: Void?, _ error: Error?) -> Void)
```

Cancel a duty shift

Audited; the pharmacy's owners are notified.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let shiftId = 987 // UUID | 

// Cancel a duty shift
AdminDutyAPI.adminDutyShiftDelete(shiftId: shiftId) { (response, error) in
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
 **shiftId** | **UUID** |  | 

### Return type

Void (empty response body)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminDutyShiftUpdate**
```swift
    open class func adminDutyShiftUpdate(shiftId: UUID, patchedAdminDutyShiftUpdateRequest: PatchedAdminDutyShiftUpdateRequest? = nil, completion: @escaping (_ data: AdminDutyShift?, _ error: Error?) -> Void)
```

Move a duty shift

Same validation as creating one. `createdBy` keeps who created the shift. Audited; the pharmacy's owners are notified when the times change.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let shiftId = 987 // UUID | 
let patchedAdminDutyShiftUpdateRequest = PatchedAdminDutyShiftUpdateRequest(startsAt: Date(), endsAt: Date()) // PatchedAdminDutyShiftUpdateRequest |  (optional)

// Move a duty shift
AdminDutyAPI.adminDutyShiftUpdate(shiftId: shiftId, patchedAdminDutyShiftUpdateRequest: patchedAdminDutyShiftUpdateRequest) { (response, error) in
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
 **shiftId** | **UUID** |  | 
 **patchedAdminDutyShiftUpdateRequest** | [**PatchedAdminDutyShiftUpdateRequest**](PatchedAdminDutyShiftUpdateRequest.md) |  | [optional] 

### Return type

[**AdminDutyShift**](AdminDutyShift.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

