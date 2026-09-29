# AdminAdsApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
| ------------- | ------------- | ------------- |
| [**adminAdCreate**](AdminAdsApi.md#adminAdCreate) | **POST** api/v1/admin/ads/ | Create an advertisement |
| [**adminAdDelete**](AdminAdsApi.md#adminAdDelete) | **DELETE** api/v1/admin/ads/{advertisement_id}/ | Delete an advertisement |
| [**adminAdImageUpload**](AdminAdsApi.md#adminAdImageUpload) | **POST** api/v1/admin/ads/images/ | Upload an advertisement image |
| [**adminAdUpdate**](AdminAdsApi.md#adminAdUpdate) | **PUT** api/v1/admin/ads/{advertisement_id}/ | Edit an advertisement, its schedule or its activation |
| [**adminAdsList**](AdminAdsApi.md#adminAdsList) | **GET** api/v1/admin/ads/ | List advertisements |



Create an advertisement

Requires the manage permission, which is re-checked inside the handler. Action payloads are validated per action type.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminAdsApi::class.java)
val adminAdvertisementRequest : AdminAdvertisementRequest =  // AdminAdvertisementRequest | 

launch(Dispatchers.IO) {
    val result : AdminId = webService.adminAdCreate(adminAdvertisementRequest)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **adminAdvertisementRequest** | [**AdminAdvertisementRequest**](AdminAdvertisementRequest.md)|  | |

### Return type

[**AdminId**](AdminId.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


Delete an advertisement

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminAdsApi::class.java)
val advertisementId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 

launch(Dispatchers.IO) {
    webService.adminAdDelete(advertisementId)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **advertisementId** | **java.util.UUID**|  | |

### Return type

null (empty response body)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Upload an advertisement image

multipart/form-data with &#x60;file&#x60;. JPEG, PNG or WebP only, at most 2 MB, each side 100 to 4096 px. The image is re-encoded to JPEG (metadata stripped) and stored in public media under a random key. Pass the returned &#x60;imageKey&#x60; when creating or updating the advertisement.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminAdsApi::class.java)
val file : java.io.File = BINARY_DATA_HERE // java.io.File | JPEG, PNG or WebP, at most 2 MB, 100-4096 px a side.

launch(Dispatchers.IO) {
    val result : AdminAdImage = webService.adminAdImageUpload(file)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **file** | **java.io.File**| JPEG, PNG or WebP, at most 2 MB, 100-4096 px a side. | |

### Return type

[**AdminAdImage**](AdminAdImage.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json


Edit an advertisement, its schedule or its activation

Omitted fields keep their current value. Schedule, targeting and action payload are validated together, so an end before its start or a global advertisement carrying a target is refused.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminAdsApi::class.java)
val advertisementId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 
val adminAdvertisementUpdateRequest : AdminAdvertisementUpdateRequest =  // AdminAdvertisementUpdateRequest | 

launch(Dispatchers.IO) {
    val result : AdminId = webService.adminAdUpdate(advertisementId, adminAdvertisementUpdateRequest)
}
```

### Parameters
| **advertisementId** | **java.util.UUID**|  | |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **adminAdvertisementUpdateRequest** | [**AdminAdvertisementUpdateRequest**](AdminAdvertisementUpdateRequest.md)|  | [optional] |

### Return type

[**AdminId**](AdminId.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


List advertisements

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminAdsApi::class.java)

launch(Dispatchers.IO) {
    val result : AdminAdvertisementList = webService.adminAdsList()
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**AdminAdvertisementList**](AdminAdvertisementList.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

