# AdminSettingsApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
| ------------- | ------------- | ------------- |
| [**adminAppReleaseRetrieve**](AdminSettingsApi.md#adminAppReleaseRetrieve) | **GET** api/v1/admin/app-release/ | What a mobile build must be |
| [**adminAppReleaseUpdate**](AdminSettingsApi.md#adminAppReleaseUpdate) | **PUT** api/v1/admin/app-release/ | Set what a mobile build must be |
| [**adminSettingWrite**](AdminSettingsApi.md#adminSettingWrite) | **PUT** api/v1/admin/settings/ | Create or update a typed platform setting |
| [**adminSettingsList**](AdminSettingsApi.md#adminSettingsList) | **GET** api/v1/admin/settings/ | List typed platform settings |



What a mobile build must be

Zeros mean nothing is enforced, which is what an unset platform reads as.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminSettingsApi::class.java)
val platform : kotlin.String = platform_example // kotlin.String | Defaults to ANDROID.

launch(Dispatchers.IO) {
    val result : AdminAppRelease = webService.adminAppReleaseRetrieve(platform)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **platform** | **kotlin.String**| Defaults to ANDROID. | [optional] [enum: ANDROID, IOS] |

### Return type

[**AdminAppRelease**](AdminAppRelease.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Set what a mobile build must be

Requires admin.settings.manage, re-checked inside the handler. A minimum above the latest is refused: nobody can install a build that does not exist. Audited.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminSettingsApi::class.java)
val adminAppReleaseRequest : AdminAppReleaseRequest =  // AdminAppReleaseRequest | 
val platform : kotlin.String = platform_example // kotlin.String | Defaults to ANDROID.

launch(Dispatchers.IO) {
    val result : AdminAppRelease = webService.adminAppReleaseUpdate(adminAppReleaseRequest, platform)
}
```

### Parameters
| **adminAppReleaseRequest** | [**AdminAppReleaseRequest**](AdminAppReleaseRequest.md)|  | |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **platform** | **kotlin.String**| Defaults to ANDROID. | [optional] [enum: ANDROID, IOS] |

### Return type

[**AdminAppRelease**](AdminAppRelease.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


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

