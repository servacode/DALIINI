# ContentApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
| ------------- | ------------- | ------------- |
| [**publicAppReleaseRetrieve**](ContentApi.md#publicAppReleaseRetrieve) | **GET** api/v1/public/app-release/ | The minimum and newest build of the mobile app |
| [**publicContactCreate**](ContentApi.md#publicContactCreate) | **POST** api/v1/contact/ | Send a message to the platform team |
| [**publicContentPageRetrieve**](ContentApi.md#publicContentPageRetrieve) | **GET** api/v1/content/pages/{slug}/ | Retrieve one published content page |
| [**publicEmergencyNumbersList**](ContentApi.md#publicEmergencyNumbersList) | **GET** api/v1/emergency-numbers/ | Emergency numbers: national, plus the province&#39;s own |
| [**publicFaqList**](ContentApi.md#publicFaqList) | **GET** api/v1/content/faq/ | List the published questions and answers, in order |
| [**publicLegalDocumentRetrieve**](ContentApi.md#publicLegalDocumentRetrieve) | **GET** api/v1/public/legal/{key}/ | Retrieve one published page |
| [**publicLegalDocumentsList**](ContentApi.md#publicLegalDocumentsList) | **GET** api/v1/public/legal/ | List the published pages |



The minimum and newest build of the mobile app

A build below &#x60;minimumVersionCode&#x60; must stop and show &#x60;noticeAr&#x60;. A build below &#x60;latestVersionCode&#x60; may offer an update and carry on. Both are zero until an operator sets them, and zero blocks nothing. Cacheable for five minutes.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
val webService = apiClient.createWebservice(ContentApi::class.java)
val platform : kotlin.String = platform_example // kotlin.String | Defaults to ANDROID.

launch(Dispatchers.IO) {
    val result : AppRelease = webService.publicAppReleaseRetrieve(platform)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **platform** | **kotlin.String**| Defaults to ANDROID. | [optional] [enum: ANDROID, IOS] |

### Return type

[**AppRelease**](AppRelease.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Send a message to the platform team

Anonymous or signed in; a signed-in sender is linked to their account. Strictly throttled per account or per client address (3/hour by default). The client address is taken from X-Forwarded-For only when the server is configured with the number of trusted proxies.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(ContentApi::class.java)
val contactMessageRequest : ContactMessageRequest =  // ContactMessageRequest | 

launch(Dispatchers.IO) {
    val result : ContactMessageCreated = webService.publicContactCreate(contactMessageRequest)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **contactMessageRequest** | [**ContactMessageRequest**](ContactMessageRequest.md)|  | |

### Return type

[**ContactMessageCreated**](ContactMessageCreated.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


Retrieve one published content page

Only the published version is served; an unpublished or unknown slug is 404. Cacheable for five minutes (&#x60;Cache-Control: public, max-age&#x3D;300&#x60;).

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
val webService = apiClient.createWebservice(ContentApi::class.java)
val slug : kotlin.String = slug_example // kotlin.String | 

launch(Dispatchers.IO) {
    val result : ContentPage = webService.publicContentPageRetrieve(slug)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **slug** | **kotlin.String**|  | |

### Return type

[**ContentPage**](ContentPage.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Emergency numbers: national, plus the province&#39;s own

National numbers come first, then those of &#x60;provinceId&#x60; when one is given. Cacheable for five minutes.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
val webService = apiClient.createWebservice(ContentApi::class.java)
val provinceId : kotlin.String = provinceId_example // kotlin.String | Also include this province's numbers.

launch(Dispatchers.IO) {
    val result : EmergencyNumberList = webService.publicEmergencyNumbersList(provinceId)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **provinceId** | **kotlin.String**| Also include this province&#39;s numbers. | [optional] |

### Return type

[**EmergencyNumberList**](EmergencyNumberList.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


List the published questions and answers, in order

Cacheable for five minutes (&#x60;Cache-Control: public, max-age&#x3D;300&#x60;).

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
val webService = apiClient.createWebservice(ContentApi::class.java)

launch(Dispatchers.IO) {
    val result : FaqList = webService.publicFaqList()
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

