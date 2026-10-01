# AdminAnalyticsAPI

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**adminAnalyticsRetrieve**](AdminAnalyticsAPI.md#adminanalyticsretrieve) | **GET** /api/v1/admin/analytics/ | Operational KPIs
[**adminAnalyticsStaffRetrieve**](AdminAnalyticsAPI.md#adminanalyticsstaffretrieve) | **GET** /api/v1/admin/analytics/staff/ | Reviewer performance in a period


# **adminAnalyticsRetrieve**
```swift
    open class func adminAnalyticsRetrieve(from: String? = nil, to: String? = nil, completion: @escaping (_ data: AdminAnalytics?, _ error: Error?) -> Void)
```

Operational KPIs

Period-bound KPIs (approval median and the four event counts) cover `from` to `to`, by default the last 30 days, and `previous` holds the same KPIs for the equally long period just before, for comparison. The remaining fields are current totals.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let from = "from_example" // String | ISO date or datetime; default 30 days before `to`. (optional)
let to = "to_example" // String | ISO date or datetime; a bare date includes that whole day. (optional)

// Operational KPIs
AdminAnalyticsAPI.adminAnalyticsRetrieve(from: from, to: to) { (response, error) in
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
 **from** | **String** | ISO date or datetime; default 30 days before &#x60;to&#x60;. | [optional] 
 **to** | **String** | ISO date or datetime; a bare date includes that whole day. | [optional] 

### Return type

[**AdminAnalytics**](AdminAnalytics.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminAnalyticsStaffRetrieve**
```swift
    open class func adminAnalyticsStaffRetrieve(from: String? = nil, to: String? = nil, completion: @escaping (_ data: AdminStaffPerformance?, _ error: Error?) -> Void)
```

Reviewer performance in a period

Per reviewer, over applications decided in [`from`, `to`) (default last 30 days): decisions, approvals, rejections and the median submit-to-decision hours; plus problem reports they resolved or dismissed in the period.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let from = "from_example" // String |  (optional)
let to = "to_example" // String |  (optional)

// Reviewer performance in a period
AdminAnalyticsAPI.adminAnalyticsStaffRetrieve(from: from, to: to) { (response, error) in
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
 **from** | **String** |  | [optional] 
 **to** | **String** |  | [optional] 

### Return type

[**AdminStaffPerformance**](AdminStaffPerformance.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

