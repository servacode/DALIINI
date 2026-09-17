# MediaAPI

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**ownerFacilityEvidenceCreate**](MediaAPI.md#ownerfacilityevidencecreate) | **POST** /api/v1/owner/facilities/{facility_id}/evidence/ | Upload private verification evidence
[**ownerFacilityEvidenceDelete**](MediaAPI.md#ownerfacilityevidencedelete) | **DELETE** /api/v1/owner/facilities/{facility_id}/evidence/{evidence_id}/ | Delete a piece of verification evidence
[**ownerFacilityImageCreate**](MediaAPI.md#ownerfacilityimagecreate) | **POST** /api/v1/owner/facilities/{facility_id}/images/ | Upload a public facility image
[**ownerFacilityImageDelete**](MediaAPI.md#ownerfacilityimagedelete) | **DELETE** /api/v1/owner/facilities/{facility_id}/images/{image_id}/ | Delete a public facility image
[**ownerFacilityImagesList**](MediaAPI.md#ownerfacilityimageslist) | **GET** /api/v1/owner/facilities/{facility_id}/images/ | List the public images of a facility


# **ownerFacilityEvidenceCreate**
```swift
    open class func ownerFacilityEvidenceCreate(facilityId: UUID, requirementId: UUID, file: String, completion: @escaping (_ data: OwnerEvidenceCreated?, _ error: Error?) -> Void)
```

Upload private verification evidence

Sent as multipart/form-data and stored in the private namespace. The response carries identifiers only: evidence is never served through a public URL and its storage key is never returned.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let facilityId = 987 // UUID | 
let requirementId = 987 // UUID | 
let file = "file_example" // String | 

// Upload private verification evidence
MediaAPI.ownerFacilityEvidenceCreate(facilityId: facilityId, requirementId: requirementId, file: file) { (response, error) in
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
 **requirementId** | **UUID** |  | 
 **file** | **String** |  | 

### Return type

[**OwnerEvidenceCreated**](OwnerEvidenceCreated.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **ownerFacilityEvidenceDelete**
```swift
    open class func ownerFacilityEvidenceDelete(evidenceId: UUID, facilityId: UUID, completion: @escaping (_ data: Void?, _ error: Error?) -> Void)
```

Delete a piece of verification evidence

Evidence is locked while an application is under review.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let evidenceId = 987 // UUID | 
let facilityId = 987 // UUID | 

// Delete a piece of verification evidence
MediaAPI.ownerFacilityEvidenceDelete(evidenceId: evidenceId, facilityId: facilityId) { (response, error) in
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
 **evidenceId** | **UUID** |  | 
 **facilityId** | **UUID** |  | 

### Return type

Void (empty response body)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **ownerFacilityImageCreate**
```swift
    open class func ownerFacilityImageCreate(facilityId: UUID, file: String, completion: @escaping (_ data: OwnerFacilityImage?, _ error: Error?) -> Void)
```

Upload a public facility image

Sent as multipart/form-data. The server decodes the file, enforces byte and pixel limits, re-encodes to JPEG, strips metadata and stores it under a random key. The declared extension and MIME type are not trusted.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let facilityId = 987 // UUID | 
let file = "file_example" // String | 

// Upload a public facility image
MediaAPI.ownerFacilityImageCreate(facilityId: facilityId, file: file) { (response, error) in
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
 **file** | **String** |  | 

### Return type

[**OwnerFacilityImage**](OwnerFacilityImage.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **ownerFacilityImageDelete**
```swift
    open class func ownerFacilityImageDelete(facilityId: UUID, imageId: UUID, completion: @escaping (_ data: Void?, _ error: Error?) -> Void)
```

Delete a public facility image

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let facilityId = 987 // UUID | 
let imageId = 987 // UUID | 

// Delete a public facility image
MediaAPI.ownerFacilityImageDelete(facilityId: facilityId, imageId: imageId) { (response, error) in
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

# **ownerFacilityImagesList**
```swift
    open class func ownerFacilityImagesList(facilityId: UUID, completion: @escaping (_ data: OwnerFacilityImageList?, _ error: Error?) -> Void)
```

List the public images of a facility

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let facilityId = 987 // UUID | 

// List the public images of a facility
MediaAPI.ownerFacilityImagesList(facilityId: facilityId) { (response, error) in
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

