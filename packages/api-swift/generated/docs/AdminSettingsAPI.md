# AdminSettingsAPI

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**adminSettingWrite**](AdminSettingsAPI.md#adminsettingwrite) | **PUT** /api/v1/admin/settings/ | Create or update a typed platform setting
[**adminSettingsList**](AdminSettingsAPI.md#adminsettingslist) | **GET** /api/v1/admin/settings/ | List typed platform settings


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

