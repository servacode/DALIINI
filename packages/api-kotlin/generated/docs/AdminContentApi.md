# AdminContentApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
| ------------- | ------------- | ------------- |
| [**adminContactMessageHandle**](AdminContentApi.md#adminContactMessageHandle) | **POST** api/v1/admin/contact-messages/{message_id}/handle/ | Mark a contact message handled |
| [**adminContactMessagesList**](AdminContentApi.md#adminContactMessagesList) | **GET** api/v1/admin/contact-messages/ | The contact inbox, newest first |
| [**adminContentPageCreate**](AdminContentApi.md#adminContentPageCreate) | **POST** api/v1/admin/content/pages/ | Create a content page |
| [**adminContentPageDelete**](AdminContentApi.md#adminContentPageDelete) | **DELETE** api/v1/admin/content/pages/{slug}/ | Delete a content page and all its versions |
| [**adminContentPageRetrieve**](AdminContentApi.md#adminContentPageRetrieve) | **GET** api/v1/admin/content/pages/{slug}/ | Retrieve a content page with its newest words |
| [**adminContentPageUpdate**](AdminContentApi.md#adminContentPageUpdate) | **PUT** api/v1/admin/content/pages/{slug}/ | Edit, publish or unpublish a content page |
| [**adminContentPagesList**](AdminContentApi.md#adminContentPagesList) | **GET** api/v1/admin/content/pages/ | List content pages, including the built-in legal pages |
| [**adminEmergencyNumberCreate**](AdminContentApi.md#adminEmergencyNumberCreate) | **POST** api/v1/admin/emergency-numbers/ | Add an emergency number |
| [**adminEmergencyNumberDelete**](AdminContentApi.md#adminEmergencyNumberDelete) | **DELETE** api/v1/admin/emergency-numbers/{number_id}/ | Delete an emergency number |
| [**adminEmergencyNumberUpdate**](AdminContentApi.md#adminEmergencyNumberUpdate) | **PUT** api/v1/admin/emergency-numbers/{number_id}/ | Edit, move, reorder or deactivate an emergency number |
| [**adminEmergencyNumbersList**](AdminContentApi.md#adminEmergencyNumbersList) | **GET** api/v1/admin/emergency-numbers/ | List emergency numbers, national and provincial, active or not |
| [**adminFaqEntriesList**](AdminContentApi.md#adminFaqEntriesList) | **GET** api/v1/admin/content/faq/ | List FAQ entries, published or not |
| [**adminFaqEntryCreate**](AdminContentApi.md#adminFaqEntryCreate) | **POST** api/v1/admin/content/faq/ | Add a FAQ entry |
| [**adminFaqEntryDelete**](AdminContentApi.md#adminFaqEntryDelete) | **DELETE** api/v1/admin/content/faq/{entry_id}/ | Delete a FAQ entry |
| [**adminFaqEntryUpdate**](AdminContentApi.md#adminFaqEntryUpdate) | **PUT** api/v1/admin/content/faq/{entry_id}/ | Edit, reorder, publish or unpublish a FAQ entry |



Mark a contact message handled

Idempotent: a handled message keeps who handled it and when.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminContentApi::class.java)
val messageId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 
val adminContactHandleRequest : AdminContactHandleRequest =  // AdminContactHandleRequest | 

launch(Dispatchers.IO) {
    val result : AdminContactMessage = webService.adminContactMessageHandle(messageId, adminContactHandleRequest)
}
```

### Parameters
| **messageId** | **java.util.UUID**|  | |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **adminContactHandleRequest** | [**AdminContactHandleRequest**](AdminContactHandleRequest.md)|  | [optional] |

### Return type

[**AdminContactMessage**](AdminContactMessage.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


The contact inbox, newest first

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminContentApi::class.java)
val cursor : kotlin.String = cursor_example // kotlin.String | 
val kind : kotlin.String = kind_example // kotlin.String | 
val limit : kotlin.Int = 56 // kotlin.Int | 
val status : kotlin.String = status_example // kotlin.String | `open` keeps unhandled messages, `handled` the rest.

launch(Dispatchers.IO) {
    val result : AdminContactMessagePage = webService.adminContactMessagesList(cursor, kind, limit, status)
}
```

### Parameters
| **cursor** | **kotlin.String**|  | [optional] |
| **kind** | **kotlin.String**|  | [optional] [enum: CORRECTION, GENERAL, OWNER] |
| **limit** | **kotlin.Int**|  | [optional] |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **status** | **kotlin.String**| &#x60;open&#x60; keeps unhandled messages, &#x60;handled&#x60; the rest. | [optional] [enum: handled, open] |

### Return type

[**AdminContactMessagePage**](AdminContactMessagePage.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Create a content page

409 CONTENT_PAGE_EXISTS when the slug is taken (case-insensitive).

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminContentApi::class.java)
val adminContentPageCreateRequest : AdminContentPageCreateRequest =  // AdminContentPageCreateRequest | 

launch(Dispatchers.IO) {
    val result : AdminContentPage = webService.adminContentPageCreate(adminContentPageCreateRequest)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **adminContentPageCreateRequest** | [**AdminContentPageCreateRequest**](AdminContentPageCreateRequest.md)|  | |

### Return type

[**AdminContentPage**](AdminContentPage.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


Delete a content page and all its versions

409 CONTENT_PAGE_BUILT_IN for the six built-in pages: unpublish them.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminContentApi::class.java)
val slug : kotlin.String = slug_example // kotlin.String | 

launch(Dispatchers.IO) {
    webService.adminContentPageDelete(slug)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **slug** | **kotlin.String**|  | |

### Return type

null (empty response body)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Retrieve a content page with its newest words

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminContentApi::class.java)
val slug : kotlin.String = slug_example // kotlin.String | 

launch(Dispatchers.IO) {
    val result : AdminContentPage = webService.adminContentPageRetrieve(slug)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **slug** | **kotlin.String**|  | |

### Return type

[**AdminContentPage**](AdminContentPage.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Edit, publish or unpublish a content page

Changing the words of a page that was ever published writes a new version and keeps the old one as history; until &#x60;published: true&#x60; is sent the live version stays as it was (&#x60;hasUnpublishedChanges&#x60;). &#x60;published: false&#x60; takes the page offline. Omitted fields keep their value.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminContentApi::class.java)
val slug : kotlin.String = slug_example // kotlin.String | 
val adminContentPageUpdateRequest : AdminContentPageUpdateRequest =  // AdminContentPageUpdateRequest | 

launch(Dispatchers.IO) {
    val result : AdminContentPage = webService.adminContentPageUpdate(slug, adminContentPageUpdateRequest)
}
```

### Parameters
| **slug** | **kotlin.String**|  | |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **adminContentPageUpdateRequest** | [**AdminContentPageUpdateRequest**](AdminContentPageUpdateRequest.md)|  | [optional] |

### Return type

[**AdminContentPage**](AdminContentPage.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


List content pages, including the built-in legal pages

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminContentApi::class.java)

launch(Dispatchers.IO) {
    val result : AdminContentPageList = webService.adminContentPagesList()
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**AdminContentPageList**](AdminContentPageList.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Add an emergency number

No &#x60;provinceId&#x60; (or null) makes it national.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminContentApi::class.java)
val adminEmergencyNumberRequest : AdminEmergencyNumberRequest =  // AdminEmergencyNumberRequest | 

launch(Dispatchers.IO) {
    val result : AdminEmergencyNumber = webService.adminEmergencyNumberCreate(adminEmergencyNumberRequest)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **adminEmergencyNumberRequest** | [**AdminEmergencyNumberRequest**](AdminEmergencyNumberRequest.md)|  | |

### Return type

[**AdminEmergencyNumber**](AdminEmergencyNumber.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


Delete an emergency number

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminContentApi::class.java)
val numberId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 

launch(Dispatchers.IO) {
    webService.adminEmergencyNumberDelete(numberId)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **numberId** | **java.util.UUID**|  | |

### Return type

null (empty response body)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Edit, move, reorder or deactivate an emergency number

Omitted fields keep their value; &#x60;provinceId: null&#x60; makes it national.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminContentApi::class.java)
val numberId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 
val adminEmergencyNumberRequest : AdminEmergencyNumberRequest =  // AdminEmergencyNumberRequest | 

launch(Dispatchers.IO) {
    val result : AdminEmergencyNumber = webService.adminEmergencyNumberUpdate(numberId, adminEmergencyNumberRequest)
}
```

### Parameters
| **numberId** | **java.util.UUID**|  | |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **adminEmergencyNumberRequest** | [**AdminEmergencyNumberRequest**](AdminEmergencyNumberRequest.md)|  | |

### Return type

[**AdminEmergencyNumber**](AdminEmergencyNumber.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


List emergency numbers, national and provincial, active or not

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminContentApi::class.java)
val provinceId : kotlin.String = provinceId_example // kotlin.String | Keep this province's numbers; `national` keeps national ones.

launch(Dispatchers.IO) {
    val result : AdminEmergencyNumberList = webService.adminEmergencyNumbersList(provinceId)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **provinceId** | **kotlin.String**| Keep this province&#39;s numbers; &#x60;national&#x60; keeps national ones. | [optional] |

### Return type

[**AdminEmergencyNumberList**](AdminEmergencyNumberList.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


List FAQ entries, published or not

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminContentApi::class.java)

launch(Dispatchers.IO) {
    val result : AdminFaqEntryList = webService.adminFaqEntriesList()
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**AdminFaqEntryList**](AdminFaqEntryList.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Add a FAQ entry

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminContentApi::class.java)
val adminFaqEntryRequest : AdminFaqEntryRequest =  // AdminFaqEntryRequest | 

launch(Dispatchers.IO) {
    val result : AdminFaqEntry = webService.adminFaqEntryCreate(adminFaqEntryRequest)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **adminFaqEntryRequest** | [**AdminFaqEntryRequest**](AdminFaqEntryRequest.md)|  | |

### Return type

[**AdminFaqEntry**](AdminFaqEntry.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


Delete a FAQ entry

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminContentApi::class.java)
val entryId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 

launch(Dispatchers.IO) {
    webService.adminFaqEntryDelete(entryId)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **entryId** | **java.util.UUID**|  | |

### Return type

null (empty response body)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Edit, reorder, publish or unpublish a FAQ entry

Omitted fields keep their value.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminContentApi::class.java)
val entryId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 
val adminFaqEntryRequest : AdminFaqEntryRequest =  // AdminFaqEntryRequest | 

launch(Dispatchers.IO) {
    val result : AdminFaqEntry = webService.adminFaqEntryUpdate(entryId, adminFaqEntryRequest)
}
```

### Parameters
| **entryId** | **java.util.UUID**|  | |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **adminFaqEntryRequest** | [**AdminFaqEntryRequest**](AdminFaqEntryRequest.md)|  | |

### Return type

[**AdminFaqEntry**](AdminFaqEntry.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

