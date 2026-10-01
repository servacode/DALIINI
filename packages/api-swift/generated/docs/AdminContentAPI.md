# AdminContentAPI

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**adminContactMessageHandle**](AdminContentAPI.md#admincontactmessagehandle) | **POST** /api/v1/admin/contact-messages/{message_id}/handle/ | Mark a contact message handled
[**adminContactMessagesList**](AdminContentAPI.md#admincontactmessageslist) | **GET** /api/v1/admin/contact-messages/ | The contact inbox, newest first
[**adminContentPageCreate**](AdminContentAPI.md#admincontentpagecreate) | **POST** /api/v1/admin/content/pages/ | Create a content page
[**adminContentPageDelete**](AdminContentAPI.md#admincontentpagedelete) | **DELETE** /api/v1/admin/content/pages/{slug}/ | Delete a content page and all its versions
[**adminContentPageRetrieve**](AdminContentAPI.md#admincontentpageretrieve) | **GET** /api/v1/admin/content/pages/{slug}/ | Retrieve a content page with its newest words
[**adminContentPageUpdate**](AdminContentAPI.md#admincontentpageupdate) | **PUT** /api/v1/admin/content/pages/{slug}/ | Edit, publish or unpublish a content page
[**adminContentPagesList**](AdminContentAPI.md#admincontentpageslist) | **GET** /api/v1/admin/content/pages/ | List content pages, including the built-in legal pages
[**adminEmergencyNumberCreate**](AdminContentAPI.md#adminemergencynumbercreate) | **POST** /api/v1/admin/emergency-numbers/ | Add an emergency number
[**adminEmergencyNumberDelete**](AdminContentAPI.md#adminemergencynumberdelete) | **DELETE** /api/v1/admin/emergency-numbers/{number_id}/ | Delete an emergency number
[**adminEmergencyNumberUpdate**](AdminContentAPI.md#adminemergencynumberupdate) | **PUT** /api/v1/admin/emergency-numbers/{number_id}/ | Edit, move, reorder or deactivate an emergency number
[**adminEmergencyNumbersList**](AdminContentAPI.md#adminemergencynumberslist) | **GET** /api/v1/admin/emergency-numbers/ | List emergency numbers, national and provincial, active or not
[**adminFaqEntriesList**](AdminContentAPI.md#adminfaqentrieslist) | **GET** /api/v1/admin/content/faq/ | List FAQ entries, published or not
[**adminFaqEntryCreate**](AdminContentAPI.md#adminfaqentrycreate) | **POST** /api/v1/admin/content/faq/ | Add a FAQ entry
[**adminFaqEntryDelete**](AdminContentAPI.md#adminfaqentrydelete) | **DELETE** /api/v1/admin/content/faq/{entry_id}/ | Delete a FAQ entry
[**adminFaqEntryUpdate**](AdminContentAPI.md#adminfaqentryupdate) | **PUT** /api/v1/admin/content/faq/{entry_id}/ | Edit, reorder, publish or unpublish a FAQ entry


# **adminContactMessageHandle**
```swift
    open class func adminContactMessageHandle(messageId: UUID, adminContactHandleRequest: AdminContactHandleRequest? = nil, completion: @escaping (_ data: AdminContactMessage?, _ error: Error?) -> Void)
```

Mark a contact message handled

Idempotent: a handled message keeps who handled it and when.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let messageId = 987 // UUID | 
let adminContactHandleRequest = AdminContactHandleRequest(note: "note_example") // AdminContactHandleRequest |  (optional)

// Mark a contact message handled
AdminContentAPI.adminContactMessageHandle(messageId: messageId, adminContactHandleRequest: adminContactHandleRequest) { (response, error) in
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
 **messageId** | **UUID** |  | 
 **adminContactHandleRequest** | [**AdminContactHandleRequest**](AdminContactHandleRequest.md) |  | [optional] 

### Return type

[**AdminContactMessage**](AdminContactMessage.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminContactMessagesList**
```swift
    open class func adminContactMessagesList(cursor: String? = nil, kind: Kind_adminContactMessagesList? = nil, limit: Int? = nil, status: Status_adminContactMessagesList? = nil, completion: @escaping (_ data: AdminContactMessagePage?, _ error: Error?) -> Void)
```

The contact inbox, newest first

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let cursor = "cursor_example" // String |  (optional)
let kind = "kind_example" // String |  (optional)
let limit = 987 // Int |  (optional)
let status = "status_example" // String | `open` keeps unhandled messages, `handled` the rest. (optional)

// The contact inbox, newest first
AdminContentAPI.adminContactMessagesList(cursor: cursor, kind: kind, limit: limit, status: status) { (response, error) in
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
 **cursor** | **String** |  | [optional] 
 **kind** | **String** |  | [optional] 
 **limit** | **Int** |  | [optional] 
 **status** | **String** | &#x60;open&#x60; keeps unhandled messages, &#x60;handled&#x60; the rest. | [optional] 

### Return type

[**AdminContactMessagePage**](AdminContactMessagePage.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminContentPageCreate**
```swift
    open class func adminContentPageCreate(adminContentPageCreateRequest: AdminContentPageCreateRequest, completion: @escaping (_ data: AdminContentPage?, _ error: Error?) -> Void)
```

Create a content page

409 CONTENT_PAGE_EXISTS when the slug is taken (case-insensitive).

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let adminContentPageCreateRequest = AdminContentPageCreateRequest(slug: "slug_example", kind: ContentPageKindEnum(), titleAr: "titleAr_example", bodyAr: "bodyAr_example", published: false) // AdminContentPageCreateRequest | 

// Create a content page
AdminContentAPI.adminContentPageCreate(adminContentPageCreateRequest: adminContentPageCreateRequest) { (response, error) in
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
 **adminContentPageCreateRequest** | [**AdminContentPageCreateRequest**](AdminContentPageCreateRequest.md) |  | 

### Return type

[**AdminContentPage**](AdminContentPage.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminContentPageDelete**
```swift
    open class func adminContentPageDelete(slug: String, completion: @escaping (_ data: Void?, _ error: Error?) -> Void)
```

Delete a content page and all its versions

409 CONTENT_PAGE_BUILT_IN for the six built-in pages: unpublish them.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let slug = "slug_example" // String | 

// Delete a content page and all its versions
AdminContentAPI.adminContentPageDelete(slug: slug) { (response, error) in
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

Void (empty response body)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminContentPageRetrieve**
```swift
    open class func adminContentPageRetrieve(slug: String, completion: @escaping (_ data: AdminContentPage?, _ error: Error?) -> Void)
```

Retrieve a content page with its newest words

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let slug = "slug_example" // String | 

// Retrieve a content page with its newest words
AdminContentAPI.adminContentPageRetrieve(slug: slug) { (response, error) in
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

[**AdminContentPage**](AdminContentPage.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminContentPageUpdate**
```swift
    open class func adminContentPageUpdate(slug: String, adminContentPageUpdateRequest: AdminContentPageUpdateRequest? = nil, completion: @escaping (_ data: AdminContentPage?, _ error: Error?) -> Void)
```

Edit, publish or unpublish a content page

Changing the words of a page that was ever published writes a new version and keeps the old one as history; until `published: true` is sent the live version stays as it was (`hasUnpublishedChanges`). `published: false` takes the page offline. Omitted fields keep their value.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let slug = "slug_example" // String | 
let adminContentPageUpdateRequest = AdminContentPageUpdateRequest(kind: ContentPageKindEnum(), titleAr: "titleAr_example", bodyAr: "bodyAr_example", published: false) // AdminContentPageUpdateRequest |  (optional)

// Edit, publish or unpublish a content page
AdminContentAPI.adminContentPageUpdate(slug: slug, adminContentPageUpdateRequest: adminContentPageUpdateRequest) { (response, error) in
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
 **adminContentPageUpdateRequest** | [**AdminContentPageUpdateRequest**](AdminContentPageUpdateRequest.md) |  | [optional] 

### Return type

[**AdminContentPage**](AdminContentPage.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminContentPagesList**
```swift
    open class func adminContentPagesList(completion: @escaping (_ data: AdminContentPageList?, _ error: Error?) -> Void)
```

List content pages, including the built-in legal pages

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI


// List content pages, including the built-in legal pages
AdminContentAPI.adminContentPagesList() { (response, error) in
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

[**AdminContentPageList**](AdminContentPageList.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminEmergencyNumberCreate**
```swift
    open class func adminEmergencyNumberCreate(adminEmergencyNumberRequest: AdminEmergencyNumberRequest, completion: @escaping (_ data: AdminEmergencyNumber?, _ error: Error?) -> Void)
```

Add an emergency number

No `provinceId` (or null) makes it national.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let adminEmergencyNumberRequest = AdminEmergencyNumberRequest(provinceId: 123, labelAr: "labelAr_example", phone: "phone_example", kind: EmergencyNumberKindEnum(), sortOrder: 123, active: false, adminNote: "adminNote_example") // AdminEmergencyNumberRequest | 

// Add an emergency number
AdminContentAPI.adminEmergencyNumberCreate(adminEmergencyNumberRequest: adminEmergencyNumberRequest) { (response, error) in
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
 **adminEmergencyNumberRequest** | [**AdminEmergencyNumberRequest**](AdminEmergencyNumberRequest.md) |  | 

### Return type

[**AdminEmergencyNumber**](AdminEmergencyNumber.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminEmergencyNumberDelete**
```swift
    open class func adminEmergencyNumberDelete(numberId: UUID, completion: @escaping (_ data: Void?, _ error: Error?) -> Void)
```

Delete an emergency number

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let numberId = 987 // UUID | 

// Delete an emergency number
AdminContentAPI.adminEmergencyNumberDelete(numberId: numberId) { (response, error) in
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
 **numberId** | **UUID** |  | 

### Return type

Void (empty response body)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminEmergencyNumberUpdate**
```swift
    open class func adminEmergencyNumberUpdate(numberId: UUID, adminEmergencyNumberRequest: AdminEmergencyNumberRequest, completion: @escaping (_ data: AdminEmergencyNumber?, _ error: Error?) -> Void)
```

Edit, move, reorder or deactivate an emergency number

Omitted fields keep their value; `provinceId: null` makes it national.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let numberId = 987 // UUID | 
let adminEmergencyNumberRequest = AdminEmergencyNumberRequest(provinceId: 123, labelAr: "labelAr_example", phone: "phone_example", kind: EmergencyNumberKindEnum(), sortOrder: 123, active: false, adminNote: "adminNote_example") // AdminEmergencyNumberRequest | 

// Edit, move, reorder or deactivate an emergency number
AdminContentAPI.adminEmergencyNumberUpdate(numberId: numberId, adminEmergencyNumberRequest: adminEmergencyNumberRequest) { (response, error) in
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
 **numberId** | **UUID** |  | 
 **adminEmergencyNumberRequest** | [**AdminEmergencyNumberRequest**](AdminEmergencyNumberRequest.md) |  | 

### Return type

[**AdminEmergencyNumber**](AdminEmergencyNumber.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminEmergencyNumbersList**
```swift
    open class func adminEmergencyNumbersList(provinceId: String? = nil, completion: @escaping (_ data: AdminEmergencyNumberList?, _ error: Error?) -> Void)
```

List emergency numbers, national and provincial, active or not

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let provinceId = "provinceId_example" // String | Keep this province's numbers; `national` keeps national ones. (optional)

// List emergency numbers, national and provincial, active or not
AdminContentAPI.adminEmergencyNumbersList(provinceId: provinceId) { (response, error) in
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
 **provinceId** | **String** | Keep this province&#39;s numbers; &#x60;national&#x60; keeps national ones. | [optional] 

### Return type

[**AdminEmergencyNumberList**](AdminEmergencyNumberList.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminFaqEntriesList**
```swift
    open class func adminFaqEntriesList(completion: @escaping (_ data: AdminFaqEntryList?, _ error: Error?) -> Void)
```

List FAQ entries, published or not

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI


// List FAQ entries, published or not
AdminContentAPI.adminFaqEntriesList() { (response, error) in
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

[**AdminFaqEntryList**](AdminFaqEntryList.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminFaqEntryCreate**
```swift
    open class func adminFaqEntryCreate(adminFaqEntryRequest: AdminFaqEntryRequest, completion: @escaping (_ data: AdminFaqEntry?, _ error: Error?) -> Void)
```

Add a FAQ entry

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let adminFaqEntryRequest = AdminFaqEntryRequest(questionAr: "questionAr_example", answerAr: "answerAr_example", sortOrder: 123, published: false) // AdminFaqEntryRequest | 

// Add a FAQ entry
AdminContentAPI.adminFaqEntryCreate(adminFaqEntryRequest: adminFaqEntryRequest) { (response, error) in
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
 **adminFaqEntryRequest** | [**AdminFaqEntryRequest**](AdminFaqEntryRequest.md) |  | 

### Return type

[**AdminFaqEntry**](AdminFaqEntry.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminFaqEntryDelete**
```swift
    open class func adminFaqEntryDelete(entryId: UUID, completion: @escaping (_ data: Void?, _ error: Error?) -> Void)
```

Delete a FAQ entry

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let entryId = 987 // UUID | 

// Delete a FAQ entry
AdminContentAPI.adminFaqEntryDelete(entryId: entryId) { (response, error) in
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
 **entryId** | **UUID** |  | 

### Return type

Void (empty response body)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminFaqEntryUpdate**
```swift
    open class func adminFaqEntryUpdate(entryId: UUID, adminFaqEntryRequest: AdminFaqEntryRequest, completion: @escaping (_ data: AdminFaqEntry?, _ error: Error?) -> Void)
```

Edit, reorder, publish or unpublish a FAQ entry

Omitted fields keep their value.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let entryId = 987 // UUID | 
let adminFaqEntryRequest = AdminFaqEntryRequest(questionAr: "questionAr_example", answerAr: "answerAr_example", sortOrder: 123, published: false) // AdminFaqEntryRequest | 

// Edit, reorder, publish or unpublish a FAQ entry
AdminContentAPI.adminFaqEntryUpdate(entryId: entryId, adminFaqEntryRequest: adminFaqEntryRequest) { (response, error) in
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
 **entryId** | **UUID** |  | 
 **adminFaqEntryRequest** | [**AdminFaqEntryRequest**](AdminFaqEntryRequest.md) |  | 

### Return type

[**AdminFaqEntry**](AdminFaqEntry.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

