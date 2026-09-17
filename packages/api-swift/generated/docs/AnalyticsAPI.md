# AnalyticsAPI

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**analyticsEventCreate**](AnalyticsAPI.md#analyticseventcreate) | **POST** /api/v1/analytics/events/ | Record a product analytics event


# **analyticsEventCreate**
```swift
    open class func analyticsEventCreate(analyticsEventRequest: AnalyticsEventRequest, xAnonymousId: String? = nil, completion: @escaping (_ data: AnalyticsEventAccepted?, _ error: Error?) -> Void)
```

Record a product analytics event

The event name must exist in the central registry and its properties are checked against the keys declared for that event. Forbidden keys such as raw coordinates, phone numbers and tokens are rejected rather than stored.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let analyticsEventRequest = AnalyticsEventRequest(name: "name_example", properties: "TODO", occurredAt: Date()) // AnalyticsEventRequest | 
let xAnonymousId = "xAnonymousId_example" // String | Client-generated pseudonymous id for unauthenticated callers. (optional)

// Record a product analytics event
AnalyticsAPI.analyticsEventCreate(analyticsEventRequest: analyticsEventRequest, xAnonymousId: xAnonymousId) { (response, error) in
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
 **analyticsEventRequest** | [**AnalyticsEventRequest**](AnalyticsEventRequest.md) |  | 
 **xAnonymousId** | **String** | Client-generated pseudonymous id for unauthenticated callers. | [optional] 

### Return type

[**AnalyticsEventAccepted**](AnalyticsEventAccepted.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

