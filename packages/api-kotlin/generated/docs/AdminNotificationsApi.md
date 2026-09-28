# AdminNotificationsApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
| ------------- | ------------- | ------------- |
| [**adminNotificationBroadcast**](AdminNotificationsApi.md#adminNotificationBroadcast) | **POST** api/v1/admin/notifications/broadcast/ | Send a notification to many users |
| [**adminNotificationBroadcastsList**](AdminNotificationsApi.md#adminNotificationBroadcastsList) | **GET** api/v1/admin/notifications/broadcasts/ | Broadcast history, newest first |



Send a notification to many users

ALL reaches every active account (narrowed to accounts that chose &#x60;provinceId&#x60; when given); OWNERS reaches every active owner or manager (narrowed to facilities in &#x60;provinceId&#x60;). Each recipient gets an inbox notification of type &#x60;platform.broadcast&#x60;, and a push is queued for accounts with a device. Audited. At most 5 broadcasts per operator per hour (429).

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminNotificationsApi::class.java)
val adminBroadcastRequest : AdminBroadcastRequest =  // AdminBroadcastRequest | 

launch(Dispatchers.IO) {
    val result : AdminBroadcast = webService.adminNotificationBroadcast(adminBroadcastRequest)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **adminBroadcastRequest** | [**AdminBroadcastRequest**](AdminBroadcastRequest.md)|  | |

### Return type

[**AdminBroadcast**](AdminBroadcast.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


Broadcast history, newest first

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminNotificationsApi::class.java)
val cursor : kotlin.String = cursor_example // kotlin.String | 
val limit : kotlin.Int = 56 // kotlin.Int | 

launch(Dispatchers.IO) {
    val result : AdminBroadcastPage = webService.adminNotificationBroadcastsList(cursor, limit)
}
```

### Parameters
| **cursor** | **kotlin.String**|  | [optional] |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **limit** | **kotlin.Int**|  | [optional] |

### Return type

[**AdminBroadcastPage**](AdminBroadcastPage.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

