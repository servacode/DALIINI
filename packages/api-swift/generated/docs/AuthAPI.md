# AuthAPI

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**authLogin**](AuthAPI.md#authlogin) | **POST** /api/v1/auth/login/ | Exchange phone and password for session credentials
[**authLogout**](AuthAPI.md#authlogout) | **POST** /api/v1/auth/logout/ | Revoke one session
[**authLogoutAll**](AuthAPI.md#authlogoutall) | **POST** /api/v1/auth/logout-all/ | Revoke every session belonging to the caller
[**authRecoveryReset**](AuthAPI.md#authrecoveryreset) | **POST** /api/v1/auth/recovery/reset/ | Set a new password using a verified recovery challenge
[**authRecoveryStart**](AuthAPI.md#authrecoverystart) | **POST** /api/v1/auth/recovery/start/ | Start password recovery by requesting an OTP challenge
[**authRecoveryVerify**](AuthAPI.md#authrecoveryverify) | **POST** /api/v1/auth/recovery/verify/ | Verify the recovery OTP code
[**authRefresh**](AuthAPI.md#authrefresh) | **POST** /api/v1/auth/refresh/ | Rotate the refresh secret and issue a new access token
[**authRegisterComplete**](AuthAPI.md#authregistercomplete) | **POST** /api/v1/auth/register/complete/ | Set the password and open the first session
[**authRegisterStart**](AuthAPI.md#authregisterstart) | **POST** /api/v1/auth/register/start/ | Start registration by requesting an OTP challenge
[**authRegisterVerify**](AuthAPI.md#authregisterverify) | **POST** /api/v1/auth/register/verify/ | Verify the registration OTP code
[**authSessionRevoke**](AuthAPI.md#authsessionrevoke) | **DELETE** /api/v1/auth/sessions/{session_id}/ | Revoke a specific session of the caller
[**authSessionsList**](AuthAPI.md#authsessionslist) | **GET** /api/v1/auth/sessions/ | List the sessions and devices of the caller


# **authLogin**
```swift
    open class func authLogin(login: Login, completion: @escaping (_ data: SessionCredentials?, _ error: Error?) -> Void)
```

Exchange phone and password for session credentials

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let login = Login(phone: "phone_example", password: "password_example", platform: "platform_example", deviceName: "deviceName_example") // Login | 

// Exchange phone and password for session credentials
AuthAPI.authLogin(login: login) { (response, error) in
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
 **login** | [**Login**](Login.md) |  | 

### Return type

[**SessionCredentials**](SessionCredentials.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **authLogout**
```swift
    open class func authLogout(logoutRequest: LogoutRequest, completion: @escaping (_ data: Void?, _ error: Error?) -> Void)
```

Revoke one session

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let logoutRequest = LogoutRequest(sessionId: 123) // LogoutRequest | 

// Revoke one session
AuthAPI.authLogout(logoutRequest: logoutRequest) { (response, error) in
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
 **logoutRequest** | [**LogoutRequest**](LogoutRequest.md) |  | 

### Return type

Void (empty response body)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **authLogoutAll**
```swift
    open class func authLogoutAll(completion: @escaping (_ data: Void?, _ error: Error?) -> Void)
```

Revoke every session belonging to the caller

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI


// Revoke every session belonging to the caller
AuthAPI.authLogoutAll() { (response, error) in
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

Void (empty response body)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **authRecoveryReset**
```swift
    open class func authRecoveryReset(recoveryReset: RecoveryReset, completion: @escaping (_ data: Void?, _ error: Error?) -> Void)
```

Set a new password using a verified recovery challenge

A successful reset revokes every existing session for that user.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let recoveryReset = RecoveryReset(challengeId: 123, password: "password_example") // RecoveryReset | 

// Set a new password using a verified recovery challenge
AuthAPI.authRecoveryReset(recoveryReset: recoveryReset) { (response, error) in
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
 **recoveryReset** | [**RecoveryReset**](RecoveryReset.md) |  | 

### Return type

Void (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **authRecoveryStart**
```swift
    open class func authRecoveryStart(recoveryStart: RecoveryStart, completion: @escaping (_ data: ChallengeAccepted?, _ error: Error?) -> Void)
```

Start password recovery by requesting an OTP challenge

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let recoveryStart = RecoveryStart(phone: "phone_example") // RecoveryStart | 

// Start password recovery by requesting an OTP challenge
AuthAPI.authRecoveryStart(recoveryStart: recoveryStart) { (response, error) in
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
 **recoveryStart** | [**RecoveryStart**](RecoveryStart.md) |  | 

### Return type

[**ChallengeAccepted**](ChallengeAccepted.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **authRecoveryVerify**
```swift
    open class func authRecoveryVerify(challengeVerify: ChallengeVerify, completion: @escaping (_ data: ChallengeVerified?, _ error: Error?) -> Void)
```

Verify the recovery OTP code

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let challengeVerify = ChallengeVerify(challengeId: 123, code: "code_example") // ChallengeVerify | 

// Verify the recovery OTP code
AuthAPI.authRecoveryVerify(challengeVerify: challengeVerify) { (response, error) in
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
 **challengeVerify** | [**ChallengeVerify**](ChallengeVerify.md) |  | 

### Return type

[**ChallengeVerified**](ChallengeVerified.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **authRefresh**
```swift
    open class func authRefresh(refresh: Refresh, completion: @escaping (_ data: SessionCredentials?, _ error: Error?) -> Void)
```

Rotate the refresh secret and issue a new access token

The supplied secret is rotated on every successful call. Replaying a secret outside the short concurrency grace window is treated as compromise and revokes every session belonging to the user.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let refresh = Refresh(refreshToken: "refreshToken_example") // Refresh | 

// Rotate the refresh secret and issue a new access token
AuthAPI.authRefresh(refresh: refresh) { (response, error) in
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
 **refresh** | [**Refresh**](Refresh.md) |  | 

### Return type

[**SessionCredentials**](SessionCredentials.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **authRegisterComplete**
```swift
    open class func authRegisterComplete(registerComplete: RegisterComplete, completion: @escaping (_ data: SessionCredentials?, _ error: Error?) -> Void)
```

Set the password and open the first session

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let registerComplete = RegisterComplete(challengeId: 123, password: "password_example", platform: "platform_example", deviceName: "deviceName_example") // RegisterComplete | 

// Set the password and open the first session
AuthAPI.authRegisterComplete(registerComplete: registerComplete) { (response, error) in
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
 **registerComplete** | [**RegisterComplete**](RegisterComplete.md) |  | 

### Return type

[**SessionCredentials**](SessionCredentials.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **authRegisterStart**
```swift
    open class func authRegisterStart(registerStart: RegisterStart, completion: @escaping (_ data: ChallengeAccepted?, _ error: Error?) -> Void)
```

Start registration by requesting an OTP challenge

Accepts 09XXXXXXXX, +9639XXXXXXXX or 009639XXXXXXXX and normalises to the canonical form. The OTP code is delivered by the configured provider and is never returned in the response.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let registerStart = RegisterStart(displayName: "displayName_example", phone: "phone_example", provinceId: 123) // RegisterStart | 

// Start registration by requesting an OTP challenge
AuthAPI.authRegisterStart(registerStart: registerStart) { (response, error) in
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
 **registerStart** | [**RegisterStart**](RegisterStart.md) |  | 

### Return type

[**ChallengeAccepted**](ChallengeAccepted.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **authRegisterVerify**
```swift
    open class func authRegisterVerify(challengeVerify: ChallengeVerify, completion: @escaping (_ data: ChallengeVerified?, _ error: Error?) -> Void)
```

Verify the registration OTP code

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let challengeVerify = ChallengeVerify(challengeId: 123, code: "code_example") // ChallengeVerify | 

// Verify the registration OTP code
AuthAPI.authRegisterVerify(challengeVerify: challengeVerify) { (response, error) in
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
 **challengeVerify** | [**ChallengeVerify**](ChallengeVerify.md) |  | 

### Return type

[**ChallengeVerified**](ChallengeVerified.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **authSessionRevoke**
```swift
    open class func authSessionRevoke(sessionId: UUID, completion: @escaping (_ data: Void?, _ error: Error?) -> Void)
```

Revoke a specific session of the caller

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let sessionId = 987 // UUID | 

// Revoke a specific session of the caller
AuthAPI.authSessionRevoke(sessionId: sessionId) { (response, error) in
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
 **sessionId** | **UUID** |  | 

### Return type

Void (empty response body)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **authSessionsList**
```swift
    open class func authSessionsList(completion: @escaping (_ data: UserSessionList?, _ error: Error?) -> Void)
```

List the sessions and devices of the caller

Session secrets are never returned, only metadata and revocation state.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI


// List the sessions and devices of the caller
AuthAPI.authSessionsList() { (response, error) in
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

[**UserSessionList**](UserSessionList.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

