# AdminTaxonomyApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
| ------------- | ------------- | ------------- |
| [**adminCategoriesList**](AdminTaxonomyApi.md#adminCategoriesList) | **GET** api/v1/admin/categories/ | List categories |
| [**adminCategoryCapabilitiesReplace**](AdminTaxonomyApi.md#adminCategoryCapabilitiesReplace) | **PUT** api/v1/admin/categories/{category_id}/capabilities/ | Set the capability flags of a category |
| [**adminCategoryCreate**](AdminTaxonomyApi.md#adminCategoryCreate) | **POST** api/v1/admin/categories/create/ | Create a category |
| [**adminCategoryGroupCreate**](AdminTaxonomyApi.md#adminCategoryGroupCreate) | **POST** api/v1/admin/category-groups/create/ | Create a category group |
| [**adminCategoryGroupUpdate**](AdminTaxonomyApi.md#adminCategoryGroupUpdate) | **PUT** api/v1/admin/category-groups/{group_id}/ | Rename, reorder or deactivate a category group |
| [**adminCategoryGroupsList**](AdminTaxonomyApi.md#adminCategoryGroupsList) | **GET** api/v1/admin/category-groups/ | List category groups |
| [**adminCategoryProvinceReplace**](AdminTaxonomyApi.md#adminCategoryProvinceReplace) | **PUT** api/v1/admin/categories/{category_id}/provinces/ | Set the per-province switches of a category |
| [**adminCategoryUpdate**](AdminTaxonomyApi.md#adminCategoryUpdate) | **PUT** api/v1/admin/categories/{category_id}/ | Rename, move, reorder or deactivate a category |



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


Create a category

&#x60;code&#x60; and &#x60;slug&#x60; are fixed at creation and cannot be changed afterwards. A new category is invisible everywhere until its per-province switches are turned on, whatever &#x60;active&#x60; says.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminTaxonomyApi::class.java)
val adminCategoryCreateRequest : AdminCategoryCreateRequest =  // AdminCategoryCreateRequest | 

launch(Dispatchers.IO) {
    val result : AdminId = webService.adminCategoryCreate(adminCategoryCreateRequest)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **adminCategoryCreateRequest** | [**AdminCategoryCreateRequest**](AdminCategoryCreateRequest.md)|  | |

### Return type

[**AdminId**](AdminId.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


Create a category group

Cycle J begins here: a group has to exist before a category can join it.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminTaxonomyApi::class.java)
val adminCategoryGroupRequest : AdminCategoryGroupRequest =  // AdminCategoryGroupRequest | 

launch(Dispatchers.IO) {
    val result : AdminId = webService.adminCategoryGroupCreate(adminCategoryGroupRequest)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **adminCategoryGroupRequest** | [**AdminCategoryGroupRequest**](AdminCategoryGroupRequest.md)|  | [optional] |

### Return type

[**AdminId**](AdminId.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


Rename, reorder or deactivate a category group

The group code is immutable; sending a different one is refused.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminTaxonomyApi::class.java)
val groupId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 
val adminCategoryGroupRequest : AdminCategoryGroupRequest =  // AdminCategoryGroupRequest | 

launch(Dispatchers.IO) {
    val result : AdminCategoryGroup = webService.adminCategoryGroupUpdate(groupId, adminCategoryGroupRequest)
}
```

### Parameters
| **groupId** | **java.util.UUID**|  | |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **adminCategoryGroupRequest** | [**AdminCategoryGroupRequest**](AdminCategoryGroupRequest.md)|  | [optional] |

### Return type

[**AdminCategoryGroup**](AdminCategoryGroup.md)

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


Rename, move, reorder or deactivate a category

&#x60;code&#x60; and &#x60;slug&#x60; are immutable and are not accepted. Changing the specialization re-validates the capability set, so a category that carries duty cannot be moved off PHARMACY while it does.

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
val adminCategoryUpdateRequest : AdminCategoryUpdateRequest =  // AdminCategoryUpdateRequest | 

launch(Dispatchers.IO) {
    val result : AdminCategory = webService.adminCategoryUpdate(categoryId, adminCategoryUpdateRequest)
}
```

### Parameters
| **categoryId** | **java.util.UUID**|  | |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **adminCategoryUpdateRequest** | [**AdminCategoryUpdateRequest**](AdminCategoryUpdateRequest.md)|  | [optional] |

### Return type

[**AdminCategory**](AdminCategory.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

