# PublicFacilitiesAPI

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**publicFacilityReportCreate**](PublicFacilitiesAPI.md#publicfacilityreportcreate) | **POST** /api/v1/facilities/{facility_id}/reports/ | Report a problem with a facility&#39;s listing


# **publicFacilityReportCreate**
```swift
    open class func publicFacilityReportCreate(facilityId: UUID, facilityReportRequest: FacilityReportRequest, completion: @escaping (_ data: FacilityReportCreated?, _ error: Error?) -> Void)
```

Report a problem with a facility's listing

Anonymous callers are allowed; a signed-in caller is recorded as the reporter. Strictly throttled per account or IP. Only publicly visible facilities accept reports.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let facilityId = 987 // UUID | 
let facilityReportRequest = FacilityReportRequest(reason: FacilityReportReasonEnum(), note: "note_example") // FacilityReportRequest | 

// Report a problem with a facility's listing
PublicFacilitiesAPI.publicFacilityReportCreate(facilityId: facilityId, facilityReportRequest: facilityReportRequest) { (response, error) in
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
 **facilityReportRequest** | [**FacilityReportRequest**](FacilityReportRequest.md) |  | 

### Return type

[**FacilityReportCreated**](FacilityReportCreated.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

