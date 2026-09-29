# AdminNotificationsAPI

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**adminNotificationBroadcast**](AdminNotificationsAPI.md#adminnotificationbroadcast) | **POST** /api/v1/admin/notifications/broadcast/ | Send a notification to many users
[**adminNotificationBroadcastsList**](AdminNotificationsAPI.md#adminnotificationbroadcastslist) | **GET** /api/v1/admin/notifications/broadcasts/ | Broadcast history, newest first


# **adminNotificationBroadcast**
```swift
    open class func adminNotificationBroadcast(adminBroadcastRequest: AdminBroadcastRequest, completion: @escaping (_ data: AdminBroadcast?, _ error: Error?) -> Void)
```

Send a notification to many users

ALL reaches every active account (narrowed to accounts that chose `provinceId` when given); OWNERS reaches every active owner or manager (narrowed to facilities in `provinceId`). Each recipient gets an inbox notification of type `platform.broadcast`, and a push is queued for accounts with a device. Audited. At most 5 broadcasts per operator per hour (429).

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let adminBroadcastRequest = AdminBroadcastRequest(titleAr: "titleAr_example", bodyAr: "bodyAr_example", provinceId: 123, audience: BroadcastAudienceEnum()) // AdminBroadcastRequest | 

// Send a notification to many users
AdminNotificationsAPI.adminNotificationBroadcast(adminBroadcastRequest: adminBroadcastRequest) { (response, error) in
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
 **adminBroadcastRequest** | [**AdminBroadcastRequest**](AdminBroadcastRequest.md) |  | 

### Return type

[**AdminBroadcast**](AdminBroadcast.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminNotificationBroadcastsList**
```swift
    open class func adminNotificationBroadcastsList(cursor: String? = nil, limit: Int? = nil, completion: @escaping (_ data: AdminBroadcastPage?, _ error: Error?) -> Void)
```

Broadcast history, newest first

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let cursor = "cursor_example" // String |  (optional)
let limit = 987 // Int |  (optional)

// Broadcast history, newest first
AdminNotificationsAPI.adminNotificationBroadcastsList(cursor: cursor, limit: limit) { (response, error) in
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
 **limit** | **Int** |  | [optional] 

### Return type

[**AdminBroadcastPage**](AdminBroadcastPage.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

