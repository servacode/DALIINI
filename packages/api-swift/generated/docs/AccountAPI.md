# AccountAPI

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**accountDeletionRequestCreate**](AccountAPI.md#accountdeletionrequestcreate) | **POST** /api/v1/account/deletion-request/ | Request deletion of the account of the caller
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

