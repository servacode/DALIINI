# AccountApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
| ------------- | ------------- | ------------- |
| [**accountDeletionRequestCreate**](AccountApi.md#accountDeletionRequestCreate) | **POST** api/v1/account/deletion-request/ | Request deletion of the account of the caller |
| [**accountFavoriteAdd**](AccountApi.md#accountFavoriteAdd) | **POST** api/v1/account/favorites/ | Save a facility |
| [**accountFavoriteRemove**](AccountApi.md#accountFavoriteRemove) | **DELETE** api/v1/account/favorites/{facility_id}/ | Remove a facility the caller had saved |
| [**accountFavoritesList**](AccountApi.md#accountFavoritesList) | **GET** api/v1/account/favorites/ | List the facilities the caller has saved |
| [**accountNotificationMarkRead**](AccountApi.md#accountNotificationMarkRead) | **POST** api/v1/account/notifications/{notification_id}/read/ | Mark one notification as read |
| [**accountNotificationsList**](AccountApi.md#accountNotificationsList) | **GET** api/v1/account/notifications/ | List the caller&#39;s notifications, newest first |
| [**accountNotificationsMarkAllRead**](AccountApi.md#accountNotificationsMarkAllRead) | **POST** api/v1/account/notifications/read-all/ | Mark every unread notification as read |
| [**accountNotificationsUnreadCount**](AccountApi.md#accountNotificationsUnreadCount) | **GET** api/v1/account/notifications/unread-count/ | How many of the caller&#39;s notifications are unread |
| [**accountPasswordChange**](AccountApi.md#accountPasswordChange) | **POST** api/v1/account/password/ | Change the caller&#39;s password |
| [**accountProfileRetrieve**](AccountApi.md#accountProfileRetrieve) | **GET** api/v1/account/profile/ | Retrieve the profile of the caller |
| [**accountProfileUpdate**](AccountApi.md#accountProfileUpdate) | **PATCH** api/v1/account/profile/ | Update the display name or profile province of the caller |
| [**accountPushTokenRegister**](AccountApi.md#accountPushTokenRegister) | **PUT** api/v1/account/push-token/ | Register or refresh this device&#39;s push token |
| [**accountPushTokenUnregister**](AccountApi.md#accountPushTokenUnregister) | **POST** api/v1/account/push-token/unregister/ | Stop sending pushes to a device token |
| [**accountRatingsList**](AccountApi.md#accountRatingsList) | **GET** api/v1/account/ratings/ | List the ratings written by the caller |



Request deletion of the account of the caller

Required by Play policy for any app that creates accounts. Ownership obligations and legally retained records are handled by the deletion policy.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AccountApi::class.java)
val deletionRequest : DeletionRequest =  // DeletionRequest | 

launch(Dispatchers.IO) {
    val result : AccountDeletionRequested = webService.accountDeletionRequestCreate(deletionRequest)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **deletionRequest** | [**DeletionRequest**](DeletionRequest.md)|  | |

### Return type

[**AccountDeletionRequested**](AccountDeletionRequested.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


Save a facility

Idempotent: saving a facility that is already saved changes nothing.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AccountApi::class.java)
val favoriteWrite : FavoriteWrite =  // FavoriteWrite | 

launch(Dispatchers.IO) {
    val result : FavoriteState = webService.accountFavoriteAdd(favoriteWrite)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **favoriteWrite** | [**FavoriteWrite**](FavoriteWrite.md)|  | |

### Return type

[**FavoriteState**](FavoriteState.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


Remove a facility the caller had saved

Idempotent: removing what was not saved is not an error.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AccountApi::class.java)
val facilityId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 

launch(Dispatchers.IO) {
    val result : FavoriteState = webService.accountFavoriteRemove(facilityId)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **facilityId** | **java.util.UUID**|  | |

### Return type

[**FavoriteState**](FavoriteState.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


List the facilities the caller has saved

Newest first, cursor-paginated. A saved facility that is no longer public — closed, suspended, or in a category the province stopped serving — is not returned, because this list is served by the same public query every other list uses.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AccountApi::class.java)

launch(Dispatchers.IO) {
    val result : FavoriteList = webService.accountFavoritesList()
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**FavoriteList**](FavoriteList.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Mark one notification as read

Idempotent: a message that was already read keeps the time it was read.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AccountApi::class.java)
val notificationId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 

launch(Dispatchers.IO) {
    val result : UnreadCount = webService.accountNotificationMarkRead(notificationId)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **notificationId** | **java.util.UUID**|  | |

### Return type

[**UnreadCount**](UnreadCount.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


List the caller&#39;s notifications, newest first

The account&#39;s own inbox.  Every message the platform has sent this account is here whether or not a push ever reached the device, which is what makes the inbox the record and the push only an announcement.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AccountApi::class.java)

launch(Dispatchers.IO) {
    val result : NotificationPage = webService.accountNotificationsList()
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**NotificationPage**](NotificationPage.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Mark every unread notification as read

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AccountApi::class.java)

launch(Dispatchers.IO) {
    val result : UnreadCount = webService.accountNotificationsMarkAllRead()
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**UnreadCount**](UnreadCount.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


How many of the caller&#39;s notifications are unread

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AccountApi::class.java)

launch(Dispatchers.IO) {
    val result : UnreadCount = webService.accountNotificationsUnreadCount()
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**UnreadCount**](UnreadCount.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Change the caller&#39;s password

The caller proves the current password first. A successful change revokes every session, including this one, so the caller signs in again with the new password.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AccountApi::class.java)
val passwordChange : PasswordChange =  // PasswordChange | 

launch(Dispatchers.IO) {
    webService.accountPasswordChange(passwordChange)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **passwordChange** | [**PasswordChange**](PasswordChange.md)|  | |

### Return type

null (empty response body)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


Retrieve the profile of the caller

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AccountApi::class.java)

launch(Dispatchers.IO) {
    val result : Profile = webService.accountProfileRetrieve()
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**Profile**](Profile.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Update the display name or profile province of the caller

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AccountApi::class.java)
val patchedProfilePatch : PatchedProfilePatch =  // PatchedProfilePatch | 

launch(Dispatchers.IO) {
    val result : Profile = webService.accountProfileUpdate(patchedProfilePatch)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **patchedProfilePatch** | [**PatchedProfilePatch**](PatchedProfilePatch.md)|  | [optional] |

### Return type

[**Profile**](Profile.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


Register or refresh this device&#39;s push token

Idempotent. A new token from the same session replaces the previous one. The token is tied to the calling session and deactivated when that session ends.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AccountApi::class.java)
val pushTokenRegister : PushTokenRegister =  // PushTokenRegister | 

launch(Dispatchers.IO) {
    webService.accountPushTokenRegister(pushTokenRegister)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **pushTokenRegister** | [**PushTokenRegister**](PushTokenRegister.md)|  | |

### Return type

null (empty response body)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


Stop sending pushes to a device token

Idempotent: an unknown or already inactive token also answers 204.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AccountApi::class.java)
val pushToken : PushToken =  // PushToken | 

launch(Dispatchers.IO) {
    webService.accountPushTokenUnregister(pushToken)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **pushToken** | [**PushToken**](PushToken.md)|  | |

### Return type

null (empty response body)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


List the ratings written by the caller

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AccountApi::class.java)

launch(Dispatchers.IO) {
    val result : AccountRatingList = webService.accountRatingsList()
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**AccountRatingList**](AccountRatingList.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

