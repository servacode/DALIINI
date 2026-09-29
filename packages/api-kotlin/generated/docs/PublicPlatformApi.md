# PublicPlatformApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
| ------------- | ------------- | ------------- |
| [**publicPlatformStatusRetrieve**](PublicPlatformApi.md#publicPlatformStatusRetrieve) | **GET** api/v1/platform/status/ | Platform availability (maintenance mode) |



Platform availability (maintenance mode)

Always served, even during maintenance. While &#x60;maintenance&#x60; is true every other public and owner API answers 503 with code MAINTENANCE, details {retryAfterSeconds} and a Retry-After header.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
val webService = apiClient.createWebservice(PublicPlatformApi::class.java)

launch(Dispatchers.IO) {
    val result : PlatformStatus = webService.publicPlatformStatusRetrieve()
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**PlatformStatus**](PlatformStatus.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

