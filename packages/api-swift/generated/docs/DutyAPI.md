# DutyAPI

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**ownerFacilityDutyCreate**](DutyAPI.md#ownerfacilitydutycreate) | **POST** /api/v1/owner/facilities/{facility_id}/duty/ | Schedule a duty shift
[**ownerFacilityDutyDelete**](DutyAPI.md#ownerfacilitydutydelete) | **DELETE** /api/v1/owner/facilities/{facility_id}/duty/{shift_id}/ | Remove a duty shift
[**ownerFacilityDutyList**](DutyAPI.md#ownerfacilitydutylist) | **GET** /api/v1/owner/facilities/{facility_id}/duty/ | List duty shifts of a facility
[**ownerFacilityDutyUpdate**](DutyAPI.md#ownerfacilitydutyupdate) | **PATCH** /api/v1/owner/facilities/{facility_id}/duty/{shift_id}/ | Adjust a duty shift


# **ownerFacilityDutyCreate**
```swift
    open class func ownerFacilityDutyCreate(facilityId: UUID, dutyShiftInput: DutyShiftInput, completion: @escaping (_ data: DutyShift?, _ error: Error?) -> Void)
```

Schedule a duty shift

Overlapping shifts for the same facility are refused by a PostgreSQL exclusion constraint, not only by application code. Only categories that declare the duty capability accept this.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let facilityId = 987 // UUID | 
let dutyShiftInput = DutyShiftInput(startsAt: Date(), endsAt: Date()) // DutyShiftInput | 

// Schedule a duty shift
DutyAPI.ownerFacilityDutyCreate(facilityId: facilityId, dutyShiftInput: dutyShiftInput) { (response, error) in
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
 **dutyShiftInput** | [**DutyShiftInput**](DutyShiftInput.md) |  | 

### Return type

[**DutyShift**](DutyShift.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **ownerFacilityDutyDelete**
```swift
    open class func ownerFacilityDutyDelete(facilityId: UUID, shiftId: UUID, completion: @escaping (_ data: Void?, _ error: Error?) -> Void)
```

Remove a duty shift

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let facilityId = 987 // UUID | 
let shiftId = 987 // UUID | 

// Remove a duty shift
DutyAPI.ownerFacilityDutyDelete(facilityId: facilityId, shiftId: shiftId) { (response, error) in
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
 **shiftId** | **UUID** |  | 

### Return type

Void (empty response body)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **ownerFacilityDutyList**
```swift
    open class func ownerFacilityDutyList(facilityId: UUID, completion: @escaping (_ data: DutyShiftList?, _ error: Error?) -> Void)
```

List duty shifts of a facility

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let facilityId = 987 // UUID | 

// List duty shifts of a facility
DutyAPI.ownerFacilityDutyList(facilityId: facilityId) { (response, error) in
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

[**DutyShiftList**](DutyShiftList.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **ownerFacilityDutyUpdate**
```swift
    open class func ownerFacilityDutyUpdate(facilityId: UUID, shiftId: UUID, patchedDutyShiftInput: PatchedDutyShiftInput? = nil, completion: @escaping (_ data: DutyShift?, _ error: Error?) -> Void)
```

Adjust a duty shift

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let facilityId = 987 // UUID | 
let shiftId = 987 // UUID | 
let patchedDutyShiftInput = PatchedDutyShiftInput(startsAt: Date(), endsAt: Date()) // PatchedDutyShiftInput |  (optional)

// Adjust a duty shift
DutyAPI.ownerFacilityDutyUpdate(facilityId: facilityId, shiftId: shiftId, patchedDutyShiftInput: patchedDutyShiftInput) { (response, error) in
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
 **shiftId** | **UUID** |  | 
 **patchedDutyShiftInput** | [**PatchedDutyShiftInput**](PatchedDutyShiftInput.md) |  | [optional] 

### Return type

[**DutyShift**](DutyShift.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

