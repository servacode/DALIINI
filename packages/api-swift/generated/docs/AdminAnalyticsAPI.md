# AdminAnalyticsAPI

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**adminAnalyticsRetrieve**](AdminAnalyticsAPI.md#adminanalyticsretrieve) | **GET** /api/v1/admin/analytics/ | Operational KPIs


# **adminAnalyticsRetrieve**
```swift
    open class func adminAnalyticsRetrieve(completion: @escaping (_ data: AdminAnalytics?, _ error: Error?) -> Void)
```

Operational KPIs

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI


// Operational KPIs
AdminAnalyticsAPI.adminAnalyticsRetrieve() { (response, error) in
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

[**AdminAnalytics**](AdminAnalytics.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

