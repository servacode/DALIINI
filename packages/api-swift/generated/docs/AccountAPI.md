# AccountAPI

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**accountDeletionRequestCreate**](AccountAPI.md#accountdeletionrequestcreate) | **POST** /api/v1/account/deletion-request/ | Request deletion of the account of the caller
[**accountFavoriteAdd**](AccountAPI.md#accountfavoriteadd) | **POST** /api/v1/account/favorites/ | Save a facility
[**accountFavoriteRemove**](AccountAPI.md#accountfavoriteremove) | **DELETE** /api/v1/account/favorites/{facility_id}/ | Remove a facility the caller had saved
[**accountFavoritesList**](AccountAPI.md#accountfavoriteslist) | **GET** /api/v1/account/favorites/ | List the facilities the caller has saved
[**accountNotificationMarkRead**](AccountAPI.md#accountnotificationmarkread) | **POST** /api/v1/account/notifications/{notification_id}/read/ | Mark one notification as read
[**accountNotificationsList**](AccountAPI.md#accountnotificationslist) | **GET** /api/v1/account/notifications/ | List the caller&#39;s notifications, newest first
[**accountNotificationsMarkAllRead**](AccountAPI.md#accountnotificationsmarkallread) | **POST** /api/v1/account/notifications/read-all/ | Mark every unread notification as read
[**accountNotificationsUnreadCount**](AccountAPI.md#accountnotificationsunreadcount) | **GET** /api/v1/account/notifications/unread-count/ | How many of the caller&#39;s notifications are unread
[**accountPasswordChange**](AccountAPI.md#accountpasswordchange) | **POST** /api/v1/account/password/ | Change the caller&#39;s password
[**accountProfileRetrieve**](AccountAPI.md#accountprofileretrieve) | **GET** /api/v1/account/profile/ | Retrieve the profile of the caller
[**accountProfileUpdate**](AccountAPI.md#accountprofileupdate) | **PATCH** /api/v1/account/profile/ | Update the display name or profile province of the caller
[**accountPushTokenRegister**](AccountAPI.md#accountpushtokenregister) | **PUT** /api/v1/account/push-token/ | Register or refresh this device&#39;s push token
[**accountPushTokenUnregister**](AccountAPI.md#accountpushtokenunregister) | **POST** /api/v1/account/push-token/unregister/ | Stop sending pushes to a device token
[**accountRatingsList**](AccountAPI.md#accountratingslist) | **GET** /api/v1/account/ratings/ | List the ratings written by the caller


# **accountDeletionRequestCreate**
```swift
    open class func accountDeletionRequestCreate(deletionRequest: DeletionRequest, completion: @escaping (_ data: AccountDeletionRequested?, _ error: Error?) -> Void)
```

Request deletion of the account of the caller

Required by Play policy for any app that creates accounts. Ownership obligations and legally retained records are handled by the deletion policy.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let deletionRequest = DeletionRequest(confirm: false) // DeletionRequest | 

// Request deletion of the account of the caller
AccountAPI.accountDeletionRequestCreate(deletionRequest: deletionRequest) { (response, error) in
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
 **deletionRequest** | [**DeletionRequest**](DeletionRequest.md) |  | 

### Return type

[**AccountDeletionRequested**](AccountDeletionRequested.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **accountFavoriteAdd**
```swift
    open class func accountFavoriteAdd(favoriteWrite: FavoriteWrite, completion: @escaping (_ data: FavoriteState?, _ error: Error?) -> Void)
```

Save a facility

Idempotent: saving a facility that is already saved changes nothing.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let favoriteWrite = FavoriteWrite(facilityId: 123) // FavoriteWrite | 

// Save a facility
AccountAPI.accountFavoriteAdd(favoriteWrite: favoriteWrite) { (response, error) in
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
 **favoriteWrite** | [**FavoriteWrite**](FavoriteWrite.md) |  | 

### Return type

[**FavoriteState**](FavoriteState.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **accountFavoriteRemove**
```swift
    open class func accountFavoriteRemove(facilityId: UUID, completion: @escaping (_ data: FavoriteState?, _ error: Error?) -> Void)
```

Remove a facility the caller had saved

Idempotent: removing what was not saved is not an error.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let facilityId = 987 // UUID | 

// Remove a facility the caller had saved
AccountAPI.accountFavoriteRemove(facilityId: facilityId) { (response, error) in
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
 **facilityId** | **UUID** |  | 

### Return type

[**FavoriteState**](FavoriteState.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **accountFavoritesList**
```swift
    open class func accountFavoritesList(completion: @escaping (_ data: FavoriteList?, _ error: Error?) -> Void)
```

List the facilities the caller has saved

Newest first, cursor-paginated. A saved facility that is no longer public — closed, suspended, or in a category the province stopped serving — is not returned, because this list is served by the same public query every other list uses.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI


// List the facilities the caller has saved
AccountAPI.accountFavoritesList() { (response, error) in
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
This endpoint does not need any parameter.

### Return type

[**FavoriteList**](FavoriteList.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **accountNotificationMarkRead**
```swift
    open class func accountNotificationMarkRead(notificationId: UUID, completion: @escaping (_ data: UnreadCount?, _ error: Error?) -> Void)
```

Mark one notification as read

Idempotent: a message that was already read keeps the time it was read.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let notificationId = 987 // UUID | 

// Mark one notification as read
AccountAPI.accountNotificationMarkRead(notificationId: notificationId) { (response, error) in
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
 **notificationId** | **UUID** |  | 

### Return type

[**UnreadCount**](UnreadCount.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **accountNotificationsList**
```swift
    open class func accountNotificationsList(completion: @escaping (_ data: NotificationPage?, _ error: Error?) -> Void)
```

List the caller's notifications, newest first

The account's own inbox.  Every message the platform has sent this account is here whether or not a push ever reached the device, which is what makes the inbox the record and the push only an announcement.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI


// List the caller's notifications, newest first
AccountAPI.accountNotificationsList() { (response, error) in
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
This endpoint does not need any parameter.

### Return type

[**NotificationPage**](NotificationPage.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **accountNotificationsMarkAllRead**
```swift
    open class func accountNotificationsMarkAllRead(completion: @escaping (_ data: UnreadCount?, _ error: Error?) -> Void)
```

Mark every unread notification as read

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI


// Mark every unread notification as read
AccountAPI.accountNotificationsMarkAllRead() { (response, error) in
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
This endpoint does not need any parameter.

### Return type

[**UnreadCount**](UnreadCount.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **accountNotificationsUnreadCount**
```swift
    open class func accountNotificationsUnreadCount(completion: @escaping (_ data: UnreadCount?, _ error: Error?) -> Void)
```

How many of the caller's notifications are unread

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI


// How many of the caller's notifications are unread
AccountAPI.accountNotificationsUnreadCount() { (response, error) in
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
This endpoint does not need any parameter.

### Return type

[**UnreadCount**](UnreadCount.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **accountPasswordChange**
```swift
    open class func accountPasswordChange(passwordChange: PasswordChange, completion: @escaping (_ data: Void?, _ error: Error?) -> Void)
```

Change the caller's password

The caller proves the current password first. A successful change revokes every session, including this one, so the caller signs in again with the new password.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let passwordChange = PasswordChange(currentPassword: "currentPassword_example", newPassword: "newPassword_example") // PasswordChange | 

// Change the caller's password
AccountAPI.accountPasswordChange(passwordChange: passwordChange) { (response, error) in
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
 **passwordChange** | [**PasswordChange**](PasswordChange.md) |  | 

### Return type

Void (empty response body)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **accountProfileRetrieve**
```swift
    open class func accountProfileRetrieve(completion: @escaping (_ data: Profile?, _ error: Error?) -> Void)
```

Retrieve the profile of the caller

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI


// Retrieve the profile of the caller
AccountAPI.accountProfileRetrieve() { (response, error) in
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
This endpoint does not need any parameter.

### Return type

[**Profile**](Profile.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **accountProfileUpdate**
```swift
    open class func accountProfileUpdate(patchedProfilePatch: PatchedProfilePatch? = nil, completion: @escaping (_ data: Profile?, _ error: Error?) -> Void)
```

Update the display name or profile province of the caller

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let patchedProfilePatch = PatchedProfilePatch(displayName: "displayName_example", provinceId: 123) // PatchedProfilePatch |  (optional)

// Update the display name or profile province of the caller
AccountAPI.accountProfileUpdate(patchedProfilePatch: patchedProfilePatch) { (response, error) in
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
 **patchedProfilePatch** | [**PatchedProfilePatch**](PatchedProfilePatch.md) |  | [optional] 

### Return type

[**Profile**](Profile.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **accountPushTokenRegister**
```swift
    open class func accountPushTokenRegister(pushTokenRegister: PushTokenRegister, completion: @escaping (_ data: Void?, _ error: Error?) -> Void)
```

Register or refresh this device's push token

Idempotent. A new token from the same session replaces the previous one. The token is tied to the calling session and deactivated when that session ends.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let pushTokenRegister = PushTokenRegister(platform: PushPlatformEnum(), token: "token_example") // PushTokenRegister | 

// Register or refresh this device's push token
AccountAPI.accountPushTokenRegister(pushTokenRegister: pushTokenRegister) { (response, error) in
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
 **pushTokenRegister** | [**PushTokenRegister**](PushTokenRegister.md) |  | 

### Return type

Void (empty response body)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **accountPushTokenUnregister**
```swift
    open class func accountPushTokenUnregister(pushToken: PushToken, completion: @escaping (_ data: Void?, _ error: Error?) -> Void)
```

Stop sending pushes to a device token

Idempotent: an unknown or already inactive token also answers 204.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let pushToken = PushToken(token: "token_example") // PushToken | 

// Stop sending pushes to a device token
AccountAPI.accountPushTokenUnregister(pushToken: pushToken) { (response, error) in
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
 **pushToken** | [**PushToken**](PushToken.md) |  | 

### Return type

Void (empty response body)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **accountRatingsList**
```swift
    open class func accountRatingsList(completion: @escaping (_ data: AccountRatingList?, _ error: Error?) -> Void)
```

List the ratings written by the caller

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI


// List the ratings written by the caller
AccountAPI.accountRatingsList() { (response, error) in
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
This endpoint does not need any parameter.

### Return type

[**AccountRatingList**](AccountRatingList.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

