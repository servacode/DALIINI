# AccountApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
| ------------- | ------------- | ------------- |
| [**accountDeletionRequestCreate**](AccountApi.md#accountDeletionRequestCreate) | **POST** api/v1/account/deletion-request/ | Request deletion of the account of the caller |
| [**accountFavoriteAdd**](AccountApi.md#accountFavoriteAdd) | **POST** api/v1/account/favorites/ | Save a facility |
| [**accountFavoriteRemove**](AccountApi.md#accountFavoriteRemove) | **DELETE** api/v1/account/favorites/{facility_id}/ | Remove a facility the caller had saved |
| [**accountFavoritesList**](AccountApi.md#accountFavoritesList) | **GET** api/v1/account/favorites/ | List the facilities the caller has saved |
| [**accountInvitationAccept**](AccountApi.md#accountInvitationAccept) | **POST** api/v1/account/invitations/{invitation_id}/accept/ | Join the facility an invitation is for |
| [**accountInvitationDecline**](AccountApi.md#accountInvitationDecline) | **POST** api/v1/account/invitations/{invitation_id}/decline/ | Decline an invitation |
| [**accountInvitationsList**](AccountApi.md#accountInvitationsList) | **GET** api/v1/account/invitations/ | Invitations waiting for this account&#39;s phone number |
| [**accountMfaConfirm**](AccountApi.md#accountMfaConfirm) | **POST** api/v1/account/mfa/confirm/ | Confirm the authenticator with its first code |
| [**accountMfaDisable**](AccountApi.md#accountMfaDisable) | **POST** api/v1/account/mfa/disable/ | Switch the authenticator off |
| [**accountMfaRetrieve**](AccountApi.md#accountMfaRetrieve) | **GET** api/v1/account/mfa/ | The second sign-in step, for this account and session |
| [**accountMfaSetup**](AccountApi.md#accountMfaSetup) | **POST** api/v1/account/mfa/setup/ | Start setting up an authenticator app |
| [**accountMfaVerify**](AccountApi.md#accountMfaVerify) | **POST** api/v1/account/mfa/verify/ | Pass the second step for this session |
| [**accountNotificationMarkRead**](AccountApi.md#accountNotificationMarkRead) | **POST** api/v1/account/notifications/{notification_id}/read/ | Mark one notification as read |
| [**accountNotificationPreferencesRetrieve**](AccountApi.md#accountNotificationPreferencesRetrieve) | **GET** api/v1/account/notification-preferences/ | Which kinds of notice are pushed to this account&#39;s devices |
| [**accountNotificationPreferencesUpdate**](AccountApi.md#accountNotificationPreferencesUpdate) | **PATCH** api/v1/account/notification-preferences/ | Change which kinds of notice are pushed |
| [**accountNotificationsList**](AccountApi.md#accountNotificationsList) | **GET** api/v1/account/notifications/ | List the caller&#39;s notifications, newest first |
| [**accountNotificationsMarkAllRead**](AccountApi.md#accountNotificationsMarkAllRead) | **POST** api/v1/account/notifications/read-all/ | Mark every unread notification as read |
| [**accountNotificationsUnreadCount**](AccountApi.md#accountNotificationsUnreadCount) | **GET** api/v1/account/notifications/unread-count/ | How many of the caller&#39;s notifications are unread |
| [**accountPasswordChange**](AccountApi.md#accountPasswordChange) | **POST** api/v1/account/password/ | Change the caller&#39;s password |
| [**accountPhoneChangeConfirm**](AccountApi.md#accountPhoneChangeConfirm) | **POST** api/v1/account/phone/confirm/ | Confirm the code and move the account to the new number |
| [**accountPhoneChangeStart**](AccountApi.md#accountPhoneChangeStart) | **POST** api/v1/account/phone/start/ | Start moving the account to another phone number |
| [**accountProfileImageDelete**](AccountApi.md#accountProfileImageDelete) | **DELETE** api/v1/account/profile/image/ | Remove the profile picture of the caller |
| [**accountProfileImageUpdate**](AccountApi.md#accountProfileImageUpdate) | **PUT** api/v1/account/profile/image/ | Upload or replace the profile picture of the caller |
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
val cursor : kotlin.String = cursor_example // kotlin.String | Opaque token returned as `nextCursor` by the previous page.
val limit : kotlin.Int = 56 // kotlin.Int | Page size, maximum 100, default 30.

launch(Dispatchers.IO) {
    val result : FavoriteList = webService.accountFavoritesList(cursor, limit)
}
```

### Parameters
| **cursor** | **kotlin.String**| Opaque token returned as &#x60;nextCursor&#x60; by the previous page. | [optional] |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **limit** | **kotlin.Int**| Page size, maximum 100, default 30. | [optional] |

### Return type

[**FavoriteList**](FavoriteList.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Join the facility an invitation is for

Only the account whose phone number was invited can accept; any other caller gets 404. An invitation to own raises a manager to owner and never lowers anyone. 409 INVITATION_EXPIRED or INVITATION_CLOSED when it can no longer be accepted.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AccountApi::class.java)
val invitationId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 

launch(Dispatchers.IO) {
    val result : Accepted = webService.accountInvitationAccept(invitationId)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **invitationId** | **java.util.UUID**|  | |

### Return type

[**Accepted**](Accepted.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Decline an invitation

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AccountApi::class.java)
val invitationId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 

launch(Dispatchers.IO) {
    webService.accountInvitationDecline(invitationId)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **invitationId** | **java.util.UUID**|  | |

### Return type

null (empty response body)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Invitations waiting for this account&#39;s phone number

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
    val result : ReceivedInvitationList = webService.accountInvitationsList()
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**ReceivedInvitationList**](ReceivedInvitationList.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Confirm the authenticator with its first code

Enables it, marks this session as having passed the second step, and returns ten recovery codes, shown this once.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AccountApi::class.java)
val mfaCode : MfaCode =  // MfaCode | 

launch(Dispatchers.IO) {
    val result : MfaRecoveryCodes = webService.accountMfaConfirm(mfaCode)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **mfaCode** | [**MfaCode**](MfaCode.md)|  | |

### Return type

[**MfaRecoveryCodes**](MfaRecoveryCodes.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


Switch the authenticator off

Needs a current code from the app. Refused with 409 MFA_REQUIRED_BY_POLICY where every operator must have one.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AccountApi::class.java)
val mfaCode : MfaCode =  // MfaCode | 

launch(Dispatchers.IO) {
    val result : MfaStatus = webService.accountMfaDisable(mfaCode)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **mfaCode** | [**MfaCode**](MfaCode.md)|  | |

### Return type

[**MfaStatus**](MfaStatus.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


The second sign-in step, for this account and session

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
    val result : MfaStatus = webService.accountMfaRetrieve()
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**MfaStatus**](MfaStatus.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Start setting up an authenticator app

Operators only. Returns a new secret and its QR code; nothing is enabled until a code from the app confirms it. Starting again replaces an unconfirmed secret.

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
    val result : MfaSetup = webService.accountMfaSetup()
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**MfaSetup**](MfaSetup.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Pass the second step for this session

A code from the app, or one of the recovery codes (each works once).

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AccountApi::class.java)
val mfaCode : MfaCode =  // MfaCode | 

launch(Dispatchers.IO) {
    val result : MfaStatus = webService.accountMfaVerify(mfaCode)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **mfaCode** | [**MfaCode**](MfaCode.md)|  | |

### Return type

[**MfaStatus**](MfaStatus.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
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


Which kinds of notice are pushed to this account&#39;s devices

All are on until the account turns one off. Only the push is governed: every message still reaches the inbox. A staff change to an owner&#39;s own duty shift, and any kind outside these three, is always pushed.

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
    val result : NotificationPreferences = webService.accountNotificationPreferencesRetrieve()
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**NotificationPreferences**](NotificationPreferences.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Change which kinds of notice are pushed

Only the fields sent change.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AccountApi::class.java)
val patchedNotificationPreferences : PatchedNotificationPreferences =  // PatchedNotificationPreferences | 

launch(Dispatchers.IO) {
    val result : NotificationPreferences = webService.accountNotificationPreferencesUpdate(patchedNotificationPreferences)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **patchedNotificationPreferences** | [**PatchedNotificationPreferences**](PatchedNotificationPreferences.md)|  | [optional] |

### Return type

[**NotificationPreferences**](NotificationPreferences.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
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
val cursor : kotlin.String = cursor_example // kotlin.String | Opaque token returned as `nextCursor` by the previous page.
val limit : kotlin.Int = 56 // kotlin.Int | Page size, maximum 100, default 30.

launch(Dispatchers.IO) {
    val result : NotificationPage = webService.accountNotificationsList(cursor, limit)
}
```

### Parameters
| **cursor** | **kotlin.String**| Opaque token returned as &#x60;nextCursor&#x60; by the previous page. | [optional] |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **limit** | **kotlin.Int**| Page size, maximum 100, default 30. | [optional] |

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


Confirm the code and move the account to the new number

Every session ends, this one included: the phone is how this account signs in, so a session issued to the old identity does not outlive it.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AccountApi::class.java)
val challengeVerify : ChallengeVerify =  // ChallengeVerify | 

launch(Dispatchers.IO) {
    val result : Profile = webService.accountPhoneChangeConfirm(challengeVerify)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **challengeVerify** | [**ChallengeVerify**](ChallengeVerify.md)|  | |

### Return type

[**Profile**](Profile.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


Start moving the account to another phone number

The code is sent to the new number, which is what proves the caller can receive on it. The account is not changed until the code is confirmed.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AccountApi::class.java)
val phoneChangeStart : PhoneChangeStart =  // PhoneChangeStart | 

launch(Dispatchers.IO) {
    val result : ChallengeAccepted = webService.accountPhoneChangeStart(phoneChangeStart)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **phoneChangeStart** | [**PhoneChangeStart**](PhoneChangeStart.md)|  | |

### Return type

[**ChallengeAccepted**](ChallengeAccepted.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


Remove the profile picture of the caller

The picture on the account: one at a time, replaced or removed.

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
    val result : Profile = webService.accountProfileImageDelete()
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


Upload or replace the profile picture of the caller

Sent as multipart/form-data. The server decodes the file, enforces byte and pixel limits, re-encodes to JPEG and strips metadata — a photograph carries where it was taken. The declared extension and MIME type are not trusted.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AccountApi::class.java)
val file : java.io.File = BINARY_DATA_HERE // java.io.File | 

launch(Dispatchers.IO) {
    val result : Profile = webService.accountProfileImageUpdate(file)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **file** | **java.io.File**|  | |

### Return type

[**Profile**](Profile.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: multipart/form-data
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

