# AdminSettingsApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
| ------------- | ------------- | ------------- |
| [**adminSettingWrite**](AdminSettingsApi.md#adminSettingWrite) | **PUT** api/v1/admin/settings/ | Create or update a typed platform setting |
| [**adminSettingsList**](AdminSettingsApi.md#adminSettingsList) | **GET** api/v1/admin/settings/ | List typed platform settings |



Create or update a typed platform setting

Requires the manage permission, which is re-checked inside the handler.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminSettingsApi::class.java)
val adminSettingWriteRequest : AdminSettingWriteRequest =  // AdminSettingWriteRequest | 

launch(Dispatchers.IO) {
    val result : AdminSettingWritten = webService.adminSettingWrite(adminSettingWriteRequest)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **adminSettingWriteRequest** | [**AdminSettingWriteRequest**](AdminSettingWriteRequest.md)|  | |

### Return type

[**AdminSettingWritten**](AdminSettingWritten.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


List typed platform settings

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminSettingsApi::class.java)

launch(Dispatchers.IO) {
    val result : AdminSettingList = webService.adminSettingsList()
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**AdminSettingList**](AdminSettingList.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

