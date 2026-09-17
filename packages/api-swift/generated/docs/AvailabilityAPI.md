# AvailabilityAPI

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**ownerFacilityHoursReplace**](AvailabilityAPI.md#ownerfacilityhoursreplace) | **PUT** /api/v1/owner/facilities/{facility_id}/hours/ | Replace the weekly opening hours of a facility
[**ownerFacilityTemporaryClosureCancel**](AvailabilityAPI.md#ownerfacilitytemporaryclosurecancel) | **DELETE** /api/v1/owner/facilities/{facility_id}/temporary-closures/{closure_id}/ | Cancel a temporary closure
[**ownerFacilityTemporaryClosureCreate**](AvailabilityAPI.md#ownerfacilitytemporaryclosurecreate) | **POST** /api/v1/owner/facilities/{facility_id}/temporary-closures/ | Open a temporary closure window
[**ownerFacilityTemporaryClosuresList**](AvailabilityAPI.md#ownerfacilitytemporaryclosureslist) | **GET** /api/v1/owner/facilities/{facility_id}/temporary-closures/ | List temporary closures of a facility


# **ownerFacilityHoursReplace**
```swift
    open class func ownerFacilityHoursReplace(facilityId: UUID, businessHourInput: [BusinessHourInput], completion: @escaping (_ data: BusinessHoursList?, _ error: Error?) -> Void)
```

Replace the weekly opening hours of a facility

The whole week is replaced in one call. Overnight spans are supported and same-day overlaps are rejected. Only categories that declare the hours capability accept this.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let facilityId = 987 // UUID | 
let businessHourInput = [BusinessHourInput(weekday: 123, opensAt: "opensAt_example", closesAt: "closesAt_example", sortOrder: 123)] // [BusinessHourInput] | 

// Replace the weekly opening hours of a facility
AvailabilityAPI.ownerFacilityHoursReplace(facilityId: facilityId, businessHourInput: businessHourInput) { (response, error) in
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

# **ownerFacilityTemporaryClosureCancel**
```swift
    open class func ownerFacilityTemporaryClosureCancel(closureId: UUID, facilityId: UUID, completion: @escaping (_ data: Void?, _ error: Error?) -> Void)
```

Cancel a temporary closure

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let closureId = 987 // UUID | 
let facilityId = 987 // UUID | 

// Cancel a temporary closure
AvailabilityAPI.ownerFacilityTemporaryClosureCancel(closureId: closureId, facilityId: facilityId) { (response, error) in
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
 **closureId** | **UUID** |  | 
 **facilityId** | **UUID** |  | 

### Return type

Void (empty response body)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **ownerFacilityTemporaryClosureCreate**
```swift
    open class func ownerFacilityTemporaryClosureCreate(facilityId: UUID, temporaryClosure: TemporaryClosure, completion: @escaping (_ data: TemporaryClosure?, _ error: Error?) -> Void)
```

Open a temporary closure window

A temporary closure overrides both regular hours and duty.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let facilityId = 987 // UUID | 
let temporaryClosure = TemporaryClosure(id: 123, startsAt: Date(), endsAt: Date(), reason: "reason_example") // TemporaryClosure | 

// Open a temporary closure window
AvailabilityAPI.ownerFacilityTemporaryClosureCreate(facilityId: facilityId, temporaryClosure: temporaryClosure) { (response, error) in
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
 **temporaryClosure** | [**TemporaryClosure**](TemporaryClosure.md) |  | 

### Return type

[**TemporaryClosure**](TemporaryClosure.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **ownerFacilityTemporaryClosuresList**
```swift
    open class func ownerFacilityTemporaryClosuresList(facilityId: UUID, completion: @escaping (_ data: TemporaryClosureList?, _ error: Error?) -> Void)
```

List temporary closures of a facility

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let facilityId = 987 // UUID | 

// List temporary closures of a facility
AvailabilityAPI.ownerFacilityTemporaryClosuresList(facilityId: facilityId) { (response, error) in
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

[**TemporaryClosureList**](TemporaryClosureList.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

