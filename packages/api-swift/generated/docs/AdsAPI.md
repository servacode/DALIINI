# AdsAPI

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**publicAdsList**](AdsAPI.md#publicadslist) | **GET** /api/v1/public/ads/ | List advertisements currently in flight


# **publicAdsList**
```swift
    open class func publicAdsList(categoryId: String? = nil, provinceId: String? = nil, completion: @escaping (_ data: PublicAdvertisementList?, _ error: Error?) -> Void)
```

List advertisements currently in flight

First-party advertisements only, filtered server-side by schedule, enabled state and targeting scope. Capped at 50 items.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let categoryId = "categoryId_example" // String | Restrict to advertisements targeting this category. (optional)
let provinceId = "provinceId_example" // String | Restrict to advertisements targeting this province. (optional)

// List advertisements currently in flight
AdsAPI.publicAdsList(categoryId: categoryId, provinceId: provinceId) { (response, error) in
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
 **categoryId** | **String** | Restrict to advertisements targeting this category. | [optional] 
 **provinceId** | **String** | Restrict to advertisements targeting this province. | [optional] 

### Return type

[**PublicAdvertisementList**](PublicAdvertisementList.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

