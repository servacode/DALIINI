# AdsApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
| ------------- | ------------- | ------------- |
| [**publicAdsList**](AdsApi.md#publicAdsList) | **GET** api/v1/public/ads/ | List advertisements currently in flight |



List advertisements currently in flight

First-party advertisements only, filtered server-side by schedule, enabled state and targeting scope. Capped at 50 items.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
val webService = apiClient.createWebservice(AdsApi::class.java)
val categoryId : kotlin.String = categoryId_example // kotlin.String | Restrict to advertisements targeting this category.
val provinceId : kotlin.String = provinceId_example // kotlin.String | Restrict to advertisements targeting this province.

launch(Dispatchers.IO) {
    val result : PublicAdvertisementList = webService.publicAdsList(categoryId, provinceId)
}
```

### Parameters
| **categoryId** | **kotlin.String**| Restrict to advertisements targeting this category. | [optional] |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **provinceId** | **kotlin.String**| Restrict to advertisements targeting this province. | [optional] |

### Return type

[**PublicAdvertisementList**](PublicAdvertisementList.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

