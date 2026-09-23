# ContentApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
| ------------- | ------------- | ------------- |
| [**publicLegalDocumentRetrieve**](ContentApi.md#publicLegalDocumentRetrieve) | **GET** api/v1/public/legal/{key}/ | Retrieve one published page |
| [**publicLegalDocumentsList**](ContentApi.md#publicLegalDocumentsList) | **GET** api/v1/public/legal/ | List the published pages |



Retrieve one published page

One published page, in full.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
val webService = apiClient.createWebservice(ContentApi::class.java)
val key : kotlin.String = key_example // kotlin.String | 

launch(Dispatchers.IO) {
    val result : LegalDocument = webService.publicLegalDocumentRetrieve(key)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **key** | **kotlin.String**|  | |

### Return type

[**LegalDocument**](LegalDocument.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


List the published pages

Titles and versions only. A client compares the version it cached with the one here and fetches a page&#39;s words only when they have changed.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
val webService = apiClient.createWebservice(ContentApi::class.java)

launch(Dispatchers.IO) {
    val result : LegalDocumentList = webService.publicLegalDocumentsList()
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

