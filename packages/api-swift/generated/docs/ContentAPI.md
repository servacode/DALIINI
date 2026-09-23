# ContentAPI

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**publicLegalDocumentRetrieve**](ContentAPI.md#publiclegaldocumentretrieve) | **GET** /api/v1/public/legal/{key}/ | Retrieve one published page
[**publicLegalDocumentsList**](ContentAPI.md#publiclegaldocumentslist) | **GET** /api/v1/public/legal/ | List the published pages


# **publicLegalDocumentRetrieve**
```swift
    open class func publicLegalDocumentRetrieve(key: String, completion: @escaping (_ data: LegalDocument?, _ error: Error?) -> Void)
```

Retrieve one published page

One published page, in full.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let key = "key_example" // String | 

// Retrieve one published page
ContentAPI.publicLegalDocumentRetrieve(key: key) { (response, error) in
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
 **key** | **String** |  | 

### Return type

[**LegalDocument**](LegalDocument.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **publicLegalDocumentsList**
```swift
    open class func publicLegalDocumentsList(completion: @escaping (_ data: LegalDocumentList?, _ error: Error?) -> Void)
```

List the published pages

Titles and versions only. A client compares the version it cached with the one here and fetches a page's words only when they have changed.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI


// List the published pages
ContentAPI.publicLegalDocumentsList() { (response, error) in
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

[**LegalDocumentList**](LegalDocumentList.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

