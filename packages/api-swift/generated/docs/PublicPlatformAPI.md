# PublicPlatformAPI

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**publicPlatformStatusRetrieve**](PublicPlatformAPI.md#publicplatformstatusretrieve) | **GET** /api/v1/platform/status/ | Platform availability (maintenance mode)


# **publicPlatformStatusRetrieve**
```swift
    open class func publicPlatformStatusRetrieve(completion: @escaping (_ data: PlatformStatus?, _ error: Error?) -> Void)
```

Platform availability (maintenance mode)

Always served, even during maintenance. While `maintenance` is true every other public and owner API answers 503 with code MAINTENANCE, details {retryAfterSeconds} and a Retry-After header.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI


// Platform availability (maintenance mode)
PublicPlatformAPI.publicPlatformStatusRetrieve() { (response, error) in
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
This endpoint does not need any parameter.

### Return type

[**PlatformStatus**](PlatformStatus.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

