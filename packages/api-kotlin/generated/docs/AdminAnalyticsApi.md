# AdminAnalyticsApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
| ------------- | ------------- | ------------- |
| [**adminAnalyticsRetrieve**](AdminAnalyticsApi.md#adminAnalyticsRetrieve) | **GET** api/v1/admin/analytics/ | Operational KPIs |
| [**adminAnalyticsSeriesRetrieve**](AdminAnalyticsApi.md#adminAnalyticsSeriesRetrieve) | **GET** api/v1/admin/analytics/series/ | The period&#39;s numbers, one Damascus day at a time |
| [**adminAnalyticsStaffRetrieve**](AdminAnalyticsApi.md#adminAnalyticsStaffRetrieve) | **GET** api/v1/admin/analytics/staff/ | Reviewer performance in a period |



Operational KPIs

Period-bound KPIs (approval median and the four event counts) cover &#x60;from&#x60; to &#x60;to&#x60;, by default the last 30 days, and &#x60;previous&#x60; holds the same KPIs for the equally long period just before, for comparison. The remaining fields are current totals.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminAnalyticsApi::class.java)
val from : kotlin.String = from_example // kotlin.String | ISO date or datetime; default 30 days before `to`.
val to : kotlin.String = to_example // kotlin.String | ISO date or datetime; a bare date includes that whole day.

launch(Dispatchers.IO) {
    val result : AdminAnalytics = webService.adminAnalyticsRetrieve(from, to)
}
```

### Parameters
| **from** | **kotlin.String**| ISO date or datetime; default 30 days before &#x60;to&#x60;. | [optional] |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **to** | **kotlin.String**| ISO date or datetime; a bare date includes that whole day. | [optional] |

### Return type

[**AdminAnalytics**](AdminAnalytics.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


The period&#39;s numbers, one Damascus day at a time

Every day from &#x60;from&#x60; to &#x60;to&#x60; is present, a quiet day as zeros. The event series are the same four the period totals count; &#x60;newUsers&#x60; are accounts created, &#x60;approvals&#x60; applications approved and &#x60;reports&#x60; problem reports received.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminAnalyticsApi::class.java)
val from : kotlin.String = from_example // kotlin.String | ISO date or datetime; default 30 days before `to`.
val to : kotlin.String = to_example // kotlin.String | ISO date or datetime; a bare date includes that whole day.

launch(Dispatchers.IO) {
    val result : AdminAnalyticsSeries = webService.adminAnalyticsSeriesRetrieve(from, to)
}
```

### Parameters
| **from** | **kotlin.String**| ISO date or datetime; default 30 days before &#x60;to&#x60;. | [optional] |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **to** | **kotlin.String**| ISO date or datetime; a bare date includes that whole day. | [optional] |

### Return type

[**AdminAnalyticsSeries**](AdminAnalyticsSeries.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Reviewer performance in a period

Per reviewer, over applications decided in [&#x60;from&#x60;, &#x60;to&#x60;) (default last 30 days): decisions, approvals, rejections and the median submit-to-decision hours; plus problem reports they resolved or dismissed in the period.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminAnalyticsApi::class.java)
val from : kotlin.String = from_example // kotlin.String | 
val to : kotlin.String = to_example // kotlin.String | 

launch(Dispatchers.IO) {
    val result : AdminStaffPerformance = webService.adminAnalyticsStaffRetrieve(from, to)
}
```

### Parameters
| **from** | **kotlin.String**|  | [optional] |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **to** | **kotlin.String**|  | [optional] |

### Return type

[**AdminStaffPerformance**](AdminStaffPerformance.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

