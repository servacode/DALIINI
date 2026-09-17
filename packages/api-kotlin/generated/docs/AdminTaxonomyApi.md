# AdminTaxonomyApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
| ------------- | ------------- | ------------- |
| [**adminCategoriesList**](AdminTaxonomyApi.md#adminCategoriesList) | **GET** api/v1/admin/categories/ | List categories |
| [**adminCategoryCapabilitiesReplace**](AdminTaxonomyApi.md#adminCategoryCapabilitiesReplace) | **PUT** api/v1/admin/categories/{category_id}/capabilities/ | Set the capability flags of a category |
| [**adminCategoryGroupsList**](AdminTaxonomyApi.md#adminCategoryGroupsList) | **GET** api/v1/admin/category-groups/ | List category groups |
| [**adminCategoryProvinceReplace**](AdminTaxonomyApi.md#adminCategoryProvinceReplace) | **PUT** api/v1/admin/categories/{category_id}/provinces/ | Set the per-province switches of a category |



List categories

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminTaxonomyApi::class.java)

launch(Dispatchers.IO) {
    val result : AdminCategoryList = webService.adminCategoriesList()
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**AdminCategoryList**](AdminCategoryList.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Set the capability flags of a category

Duty can only be enabled for an approved specialization.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminTaxonomyApi::class.java)
val categoryId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 
val adminCapabilitiesRequest : AdminCapabilitiesRequest =  // AdminCapabilitiesRequest | 

launch(Dispatchers.IO) {
    val result : AdminCapabilities = webService.adminCategoryCapabilitiesReplace(categoryId, adminCapabilitiesRequest)
}
```

### Parameters
| **categoryId** | **java.util.UUID**|  | |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **adminCapabilitiesRequest** | [**AdminCapabilitiesRequest**](AdminCapabilitiesRequest.md)|  | [optional] |

### Return type

[**AdminCapabilities**](AdminCapabilities.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


List category groups

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminTaxonomyApi::class.java)

launch(Dispatchers.IO) {
    val result : AdminCategoryGroupList = webService.adminCategoryGroupsList()
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**AdminCategoryGroupList**](AdminCategoryGroupList.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Set the per-province switches of a category

Public visibility and owner onboarding are independent switches.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminTaxonomyApi::class.java)
val categoryId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 
val adminCategoryProvinceRequest : AdminCategoryProvinceRequest =  // AdminCategoryProvinceRequest | 

launch(Dispatchers.IO) {
    val result : AdminId = webService.adminCategoryProvinceReplace(categoryId, adminCategoryProvinceRequest)
}
```

### Parameters
| **categoryId** | **java.util.UUID**|  | |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **adminCategoryProvinceRequest** | [**AdminCategoryProvinceRequest**](AdminCategoryProvinceRequest.md)|  | |

### Return type

[**AdminId**](AdminId.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

