# AdminSettingsAPI

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**adminAppReleaseRetrieve**](AdminSettingsAPI.md#adminappreleaseretrieve) | **GET** /api/v1/admin/app-release/ | What a mobile build must be
[**adminAppReleaseUpdate**](AdminSettingsAPI.md#adminappreleaseupdate) | **PUT** /api/v1/admin/app-release/ | Set what a mobile build must be
[**adminSettingWrite**](AdminSettingsAPI.md#adminsettingwrite) | **PUT** /api/v1/admin/settings/ | Create or update a typed platform setting
[**adminSettingsList**](AdminSettingsAPI.md#adminsettingslist) | **GET** /api/v1/admin/settings/ | List typed platform settings


# **adminAppReleaseRetrieve**
```swift
    open class func adminAppReleaseRetrieve(platform: Platform_adminAppReleaseRetrieve? = nil, completion: @escaping (_ data: AdminAppRelease?, _ error: Error?) -> Void)
```

What a mobile build must be

Zeros mean nothing is enforced, which is what an unset platform reads as.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let platform = "platform_example" // String | Defaults to ANDROID. (optional)

// What a mobile build must be
AdminSettingsAPI.adminAppReleaseRetrieve(platform: platform) { (response, error) in
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
 **platform** | **String** | Defaults to ANDROID. | [optional] 

### Return type

[**AdminAppRelease**](AdminAppRelease.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminAppReleaseUpdate**
```swift
    open class func adminAppReleaseUpdate(adminAppReleaseRequest: AdminAppReleaseRequest, platform: Platform_adminAppReleaseUpdate? = nil, completion: @escaping (_ data: AdminAppRelease?, _ error: Error?) -> Void)
```

Set what a mobile build must be

Requires admin.settings.manage, re-checked inside the handler. A minimum above the latest is refused: nobody can install a build that does not exist. Audited.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let adminAppReleaseRequest = AdminAppReleaseRequest(minimumVersionCode: 123, latestVersionCode: 123, storeUrl: "storeUrl_example", noticeAr: "noticeAr_example") // AdminAppReleaseRequest | 
let platform = "platform_example" // String | Defaults to ANDROID. (optional)

// Set what a mobile build must be
AdminSettingsAPI.adminAppReleaseUpdate(adminAppReleaseRequest: adminAppReleaseRequest, platform: platform) { (response, error) in
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
 **adminAppReleaseRequest** | [**AdminAppReleaseRequest**](AdminAppReleaseRequest.md) |  | 
 **platform** | **String** | Defaults to ANDROID. | [optional] 

### Return type

[**AdminAppRelease**](AdminAppRelease.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminSettingWrite**
```swift
    open class func adminSettingWrite(adminSettingWriteRequest: AdminSettingWriteRequest, completion: @escaping (_ data: AdminSettingWritten?, _ error: Error?) -> Void)
```

Create or update a typed platform setting

Requires the manage permission, which is re-checked inside the handler.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let adminSettingWriteRequest = AdminSettingWriteRequest(key: "key_example", type: "type_example", value: 123) // AdminSettingWriteRequest | 

// Create or update a typed platform setting
AdminSettingsAPI.adminSettingWrite(adminSettingWriteRequest: adminSettingWriteRequest) { (response, error) in
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
 **adminSettingWriteRequest** | [**AdminSettingWriteRequest**](AdminSettingWriteRequest.md) |  | 

### Return type

[**AdminSettingWritten**](AdminSettingWritten.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminSettingsList**
```swift
    open class func adminSettingsList(completion: @escaping (_ data: AdminSettingList?, _ error: Error?) -> Void)
```

List typed platform settings

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI


// List typed platform settings
AdminSettingsAPI.adminSettingsList() { (response, error) in
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

[**AdminSettingList**](AdminSettingList.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

