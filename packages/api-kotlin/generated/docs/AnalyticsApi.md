# AnalyticsApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
| ------------- | ------------- | ------------- |
| [**analyticsEventCreate**](AnalyticsApi.md#analyticsEventCreate) | **POST** api/v1/analytics/events/ | Record a product analytics event |



Record a product analytics event

The event name must exist in the central registry and its properties are checked against the keys declared for that event. Forbidden keys such as raw coordinates, phone numbers and tokens are rejected rather than stored.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
val webService = apiClient.createWebservice(AnalyticsApi::class.java)
val analyticsEventRequest : AnalyticsEventRequest =  // AnalyticsEventRequest | 
val xAnonymousId : kotlin.String = xAnonymousId_example // kotlin.String | Client-generated pseudonymous id for unauthenticated callers.

launch(Dispatchers.IO) {
    val result : AnalyticsEventAccepted = webService.analyticsEventCreate(analyticsEventRequest, xAnonymousId)
}
```

### Parameters
| **analyticsEventRequest** | [**AnalyticsEventRequest**](AnalyticsEventRequest.md)|  | |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **xAnonymousId** | **kotlin.String**| Client-generated pseudonymous id for unauthenticated callers. | [optional] |

### Return type

[**AnalyticsEventAccepted**](AnalyticsEventAccepted.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

