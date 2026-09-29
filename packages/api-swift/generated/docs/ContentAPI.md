# ContentAPI

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**publicContactCreate**](ContentAPI.md#publiccontactcreate) | **POST** /api/v1/contact/ | Send a message to the platform team
[**publicContentPageRetrieve**](ContentAPI.md#publiccontentpageretrieve) | **GET** /api/v1/content/pages/{slug}/ | Retrieve one published content page
[**publicEmergencyNumbersList**](ContentAPI.md#publicemergencynumberslist) | **GET** /api/v1/emergency-numbers/ | Emergency numbers: national, plus the province&#39;s own
[**publicFaqList**](ContentAPI.md#publicfaqlist) | **GET** /api/v1/content/faq/ | List the published questions and answers, in order
[**publicLegalDocumentRetrieve**](ContentAPI.md#publiclegaldocumentretrieve) | **GET** /api/v1/public/legal/{key}/ | Retrieve one published page
[**publicLegalDocumentsList**](ContentAPI.md#publiclegaldocumentslist) | **GET** /api/v1/public/legal/ | List the published pages


# **publicContactCreate**
```swift
    open class func publicContactCreate(contactMessageRequest: ContactMessageRequest, completion: @escaping (_ data: ContactMessageCreated?, _ error: Error?) -> Void)
```

Send a message to the platform team

Anonymous or signed in; a signed-in sender is linked to their account. Strictly throttled per account or per client address (3/hour by default). The client address is taken from X-Forwarded-For only when the server is configured with the number of trusted proxies.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let contactMessageRequest = ContactMessageRequest(name: "name_example", phone: "phone_example", message: "message_example", kind: ContactMessageKindEnum()) // ContactMessageRequest | 

// Send a message to the platform team
ContentAPI.publicContactCreate(contactMessageRequest: contactMessageRequest) { (response, error) in
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
 **contactMessageRequest** | [**ContactMessageRequest**](ContactMessageRequest.md) |  | 

### Return type

[**ContactMessageCreated**](ContactMessageCreated.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **publicContentPageRetrieve**
```swift
    open class func publicContentPageRetrieve(slug: String, completion: @escaping (_ data: ContentPage?, _ error: Error?) -> Void)
```

Retrieve one published content page

Only the published version is served; an unpublished or unknown slug is 404. Cacheable for five minutes (`Cache-Control: public, max-age=300`).

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let slug = "slug_example" // String | 

// Retrieve one published content page
ContentAPI.publicContentPageRetrieve(slug: slug) { (response, error) in
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
 **slug** | **String** |  | 

### Return type

[**ContentPage**](ContentPage.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **publicEmergencyNumbersList**
```swift
    open class func publicEmergencyNumbersList(provinceId: String? = nil, completion: @escaping (_ data: EmergencyNumberList?, _ error: Error?) -> Void)
```

Emergency numbers: national, plus the province's own

National numbers come first, then those of `provinceId` when one is given. Cacheable for five minutes.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let provinceId = "provinceId_example" // String | Also include this province's numbers. (optional)

// Emergency numbers: national, plus the province's own
ContentAPI.publicEmergencyNumbersList(provinceId: provinceId) { (response, error) in
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
 **provinceId** | **String** | Also include this province&#39;s numbers. | [optional] 

### Return type

[**EmergencyNumberList**](EmergencyNumberList.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **publicFaqList**
```swift
    open class func publicFaqList(completion: @escaping (_ data: FaqList?, _ error: Error?) -> Void)
```

List the published questions and answers, in order

Cacheable for five minutes (`Cache-Control: public, max-age=300`).

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI


// List the published questions and answers, in order
ContentAPI.publicFaqList() { (response, error) in
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

[**FaqList**](FaqList.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

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

