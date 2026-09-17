# AuthApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
| ------------- | ------------- | ------------- |
| [**authLogin**](AuthApi.md#authLogin) | **POST** api/v1/auth/login/ | Exchange phone and password for session credentials |
| [**authLogout**](AuthApi.md#authLogout) | **POST** api/v1/auth/logout/ | Revoke one session |
| [**authLogoutAll**](AuthApi.md#authLogoutAll) | **POST** api/v1/auth/logout-all/ | Revoke every session belonging to the caller |
| [**authRecoveryReset**](AuthApi.md#authRecoveryReset) | **POST** api/v1/auth/recovery/reset/ | Set a new password using a verified recovery challenge |
| [**authRecoveryStart**](AuthApi.md#authRecoveryStart) | **POST** api/v1/auth/recovery/start/ | Start password recovery by requesting an OTP challenge |
| [**authRecoveryVerify**](AuthApi.md#authRecoveryVerify) | **POST** api/v1/auth/recovery/verify/ | Verify the recovery OTP code |
| [**authRefresh**](AuthApi.md#authRefresh) | **POST** api/v1/auth/refresh/ | Rotate the refresh secret and issue a new access token |
| [**authRegisterComplete**](AuthApi.md#authRegisterComplete) | **POST** api/v1/auth/register/complete/ | Set the password and open the first session |
| [**authRegisterStart**](AuthApi.md#authRegisterStart) | **POST** api/v1/auth/register/start/ | Start registration by requesting an OTP challenge |
| [**authRegisterVerify**](AuthApi.md#authRegisterVerify) | **POST** api/v1/auth/register/verify/ | Verify the registration OTP code |
| [**authSessionRevoke**](AuthApi.md#authSessionRevoke) | **DELETE** api/v1/auth/sessions/{session_id}/ | Revoke a specific session of the caller |
| [**authSessionsList**](AuthApi.md#authSessionsList) | **GET** api/v1/auth/sessions/ | List the sessions and devices of the caller |



Exchange phone and password for session credentials

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
val webService = apiClient.createWebservice(AuthApi::class.java)
val login : Login =  // Login | 

launch(Dispatchers.IO) {
    val result : SessionCredentials = webService.authLogin(login)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **login** | [**Login**](Login.md)|  | |

### Return type

[**SessionCredentials**](SessionCredentials.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


Revoke one session

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AuthApi::class.java)
val logoutRequest : LogoutRequest =  // LogoutRequest | 

launch(Dispatchers.IO) {
    webService.authLogout(logoutRequest)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **logoutRequest** | [**LogoutRequest**](LogoutRequest.md)|  | |

### Return type

null (empty response body)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


Revoke every session belonging to the caller

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AuthApi::class.java)

launch(Dispatchers.IO) {
    webService.authLogoutAll()
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

null (empty response body)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Set a new password using a verified recovery challenge

A successful reset revokes every existing session for that user.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
val webService = apiClient.createWebservice(AuthApi::class.java)
val recoveryReset : RecoveryReset =  // RecoveryReset | 

launch(Dispatchers.IO) {
    webService.authRecoveryReset(recoveryReset)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **recoveryReset** | [**RecoveryReset**](RecoveryReset.md)|  | |

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


Start password recovery by requesting an OTP challenge

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
val webService = apiClient.createWebservice(AuthApi::class.java)
val recoveryStart : RecoveryStart =  // RecoveryStart | 

launch(Dispatchers.IO) {
    val result : ChallengeAccepted = webService.authRecoveryStart(recoveryStart)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **recoveryStart** | [**RecoveryStart**](RecoveryStart.md)|  | |

### Return type

[**ChallengeAccepted**](ChallengeAccepted.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


Verify the recovery OTP code

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
val webService = apiClient.createWebservice(AuthApi::class.java)
val challengeVerify : ChallengeVerify =  // ChallengeVerify | 

launch(Dispatchers.IO) {
    val result : ChallengeVerified = webService.authRecoveryVerify(challengeVerify)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **challengeVerify** | [**ChallengeVerify**](ChallengeVerify.md)|  | |

### Return type

[**ChallengeVerified**](ChallengeVerified.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


Rotate the refresh secret and issue a new access token

The supplied secret is rotated on every successful call. Replaying a secret outside the short concurrency grace window is treated as compromise and revokes every session belonging to the user.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
val webService = apiClient.createWebservice(AuthApi::class.java)
val refresh : Refresh =  // Refresh | 

launch(Dispatchers.IO) {
    val result : SessionCredentials = webService.authRefresh(refresh)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **refresh** | [**Refresh**](Refresh.md)|  | |

### Return type

[**SessionCredentials**](SessionCredentials.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


Set the password and open the first session

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
val webService = apiClient.createWebservice(AuthApi::class.java)
val registerComplete : RegisterComplete =  // RegisterComplete | 

launch(Dispatchers.IO) {
    val result : SessionCredentials = webService.authRegisterComplete(registerComplete)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **registerComplete** | [**RegisterComplete**](RegisterComplete.md)|  | |

### Return type

[**SessionCredentials**](SessionCredentials.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


Start registration by requesting an OTP challenge

Accepts 09XXXXXXXX, +9639XXXXXXXX or 009639XXXXXXXX and normalises to the canonical form. The OTP code is delivered by the configured provider and is never returned in the response.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
val webService = apiClient.createWebservice(AuthApi::class.java)
val registerStart : RegisterStart =  // RegisterStart | 

launch(Dispatchers.IO) {
    val result : ChallengeAccepted = webService.authRegisterStart(registerStart)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **registerStart** | [**RegisterStart**](RegisterStart.md)|  | |

### Return type

[**ChallengeAccepted**](ChallengeAccepted.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


Verify the registration OTP code

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
val webService = apiClient.createWebservice(AuthApi::class.java)
val challengeVerify : ChallengeVerify =  // ChallengeVerify | 

launch(Dispatchers.IO) {
    val result : ChallengeVerified = webService.authRegisterVerify(challengeVerify)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **challengeVerify** | [**ChallengeVerify**](ChallengeVerify.md)|  | |

### Return type

[**ChallengeVerified**](ChallengeVerified.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


Revoke a specific session of the caller

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AuthApi::class.java)
val sessionId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 

launch(Dispatchers.IO) {
    webService.authSessionRevoke(sessionId)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **sessionId** | **java.util.UUID**|  | |

### Return type

null (empty response body)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


List the sessions and devices of the caller

Session secrets are never returned, only metadata and revocation state.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AuthApi::class.java)

launch(Dispatchers.IO) {
    val result : UserSessionList = webService.authSessionsList()
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**UserSessionList**](UserSessionList.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

