# AdminAnalyticsApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
| ------------- | ------------- | ------------- |
| [**adminAnalyticsRetrieve**](AdminAnalyticsApi.md#adminAnalyticsRetrieve) | **GET** api/v1/admin/analytics/ | Operational KPIs |



Operational KPIs

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminAnalyticsApi::class.java)

launch(Dispatchers.IO) {
    val result : AdminAnalytics = webService.adminAnalyticsRetrieve()
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**AdminAnalytics**](AdminAnalytics.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

