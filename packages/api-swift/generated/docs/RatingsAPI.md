# RatingsAPI

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**facilityRatingDelete**](RatingsAPI.md#facilityratingdelete) | **DELETE** /api/v1/facilities/{facility_id}/rating/ | Remove the caller&#39;s rating for a facility
[**facilityRatingUpsert**](RatingsAPI.md#facilityratingupsert) | **PUT** /api/v1/facilities/{facility_id}/rating/ | Create or replace the caller&#39;s rating for a facility


# **facilityRatingDelete**
```swift
    open class func facilityRatingDelete(facilityId: UUID, completion: @escaping (_ data: Void?, _ error: Error?) -> Void)
```

Remove the caller's rating for a facility

Idempotent for a publicly visible facility; a facility that is not publicly visible answers 404, exactly as the write does.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let facilityId = 987 // UUID | 

// Remove the caller's rating for a facility
RatingsAPI.facilityRatingDelete(facilityId: facilityId) { (response, error) in
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

Void (empty response body)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **facilityRatingUpsert**
```swift
    open class func facilityRatingUpsert(facilityId: UUID, ratingWrite: RatingWrite, completion: @escaping (_ data: FacilityRating?, _ error: Error?) -> Void)
```

Create or replace the caller's rating for a facility

One rating per user per facility, so repeating the call replaces the previous value. Only categories that declare the ratings capability accept this.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let facilityId = 987 // UUID | 
let ratingWrite = RatingWrite(stars: 123) // RatingWrite | 

// Create or replace the caller's rating for a facility
RatingsAPI.facilityRatingUpsert(facilityId: facilityId, ratingWrite: ratingWrite) { (response, error) in
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
 **ratingWrite** | [**RatingWrite**](RatingWrite.md) |  | 

### Return type

[**FacilityRating**](FacilityRating.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

