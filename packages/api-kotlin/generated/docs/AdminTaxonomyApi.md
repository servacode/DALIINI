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
| [**adminCategoryServiceTagCreate**](AdminTaxonomyApi.md#adminCategoryServiceTagCreate) | **POST** api/v1/admin/categories/{category_id}/service-tags/ | Add a service to a category |
| [**adminCategoryServiceTagsList**](AdminTaxonomyApi.md#adminCategoryServiceTagsList) | **GET** api/v1/admin/categories/{category_id}/service-tags/ | List the services of a category |
| [**adminCategorySpecialtiesList**](AdminTaxonomyApi.md#adminCategorySpecialtiesList) | **GET** api/v1/admin/categories/{category_id}/specialties/ | List the specialties a category offers, in both scopes |
| [**adminCategorySpecialtyCreate**](AdminTaxonomyApi.md#adminCategorySpecialtyCreate) | **POST** api/v1/admin/categories/{category_id}/specialties/ | Add a specialty to a category or to its specialization |
| [**adminCategoryUpdate**](AdminTaxonomyApi.md#adminCategoryUpdate) | **PUT** api/v1/admin/categories/{category_id}/ | Rename, move, reorder or deactivate a category |
| [**adminServiceTagDelete**](AdminTaxonomyApi.md#adminServiceTagDelete) | **DELETE** api/v1/admin/service-tags/{service_tag_id}/ | Delete a service no facility lists |
| [**adminServiceTagUpdate**](AdminTaxonomyApi.md#adminServiceTagUpdate) | **PUT** api/v1/admin/service-tags/{service_tag_id}/ | Rename, reorder, retire or bring back a service |
| [**adminSpecialtyDelete**](AdminTaxonomyApi.md#adminSpecialtyDelete) | **DELETE** api/v1/admin/specialties/{specialty_id}/ | Delete a specialty no facility lists |
| [**adminSpecialtyUpdate**](AdminTaxonomyApi.md#adminSpecialtyUpdate) | **PUT** api/v1/admin/specialties/{specialty_id}/ | Rename, reorder, retire or bring back a specialty |



List categories

Each category with its capability flags, its province switches and how many facilities it holds: everything its card and settings window show, in one query count whatever the number of categories.

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
    val result : AdminCategoryCardList = webService.adminCategoriesList()
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**AdminCategoryCardList**](AdminCategoryCardList.md)

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


Add a service to a category

Requires admin.taxonomy.manage, re-checked inside the handler. A name already used in the category is refused, retired services included.

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
val adminServiceTagCreateRequest : AdminServiceTagCreateRequest =  // AdminServiceTagCreateRequest | 

launch(Dispatchers.IO) {
    val result : AdminServiceTag = webService.adminCategoryServiceTagCreate(categoryId, adminServiceTagCreateRequest)
}
```

### Parameters
| **categoryId** | **java.util.UUID**|  | |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **adminServiceTagCreateRequest** | [**AdminServiceTagCreateRequest**](AdminServiceTagCreateRequest.md)|  | |

### Return type

[**AdminServiceTag**](AdminServiceTag.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


List the services of a category

Retired ones included, in the order the public sees them.

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

launch(Dispatchers.IO) {
    val result : AdminServiceTagList = webService.adminCategoryServiceTagsList(categoryId)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **categoryId** | **java.util.UUID**|  | |

### Return type

[**AdminServiceTagList**](AdminServiceTagList.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


List the specialties a category offers, in both scopes

The category&#39;s own specialties and those its specialization shares, retired ones included, in the order the public sees them.

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

launch(Dispatchers.IO) {
    val result : AdminSpecialtyList = webService.adminCategorySpecialtiesList(categoryId)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **categoryId** | **java.util.UUID**|  | |

### Return type

[**AdminSpecialtyList**](AdminSpecialtyList.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Add a specialty to a category or to its specialization

Requires admin.taxonomy.manage, re-checked inside the handler. &#x60;scope&#x60; CATEGORY scopes it to this category; SPECIALIZATION shares it with every category of this category&#39;s specialization and is refused for a GENERIC category. A name already used in the same scope is refused, retired items included.

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
val adminSpecialtyCreateRequest : AdminSpecialtyCreateRequest =  // AdminSpecialtyCreateRequest | 

launch(Dispatchers.IO) {
    val result : AdminSpecialty = webService.adminCategorySpecialtyCreate(categoryId, adminSpecialtyCreateRequest)
}
```

### Parameters
| **categoryId** | **java.util.UUID**|  | |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **adminSpecialtyCreateRequest** | [**AdminSpecialtyCreateRequest**](AdminSpecialtyCreateRequest.md)|  | |

### Return type

[**AdminSpecialty**](AdminSpecialty.md)

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


Delete a service no facility lists

One that a facility lists answers 409 &#x60;SERVICE_TAG_IN_USE&#x60;; retire it with &#x60;active &#x3D; false&#x60; instead.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminTaxonomyApi::class.java)
val serviceTagId : kotlin.Int = 56 // kotlin.Int | 

launch(Dispatchers.IO) {
    webService.adminServiceTagDelete(serviceTagId)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **serviceTagId** | **kotlin.Int**|  | |

### Return type

null (empty response body)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Rename, reorder, retire or bring back a service

Omitted fields keep their value. The category is fixed: &#x60;categoryId&#x60; is refused.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminTaxonomyApi::class.java)
val serviceTagId : kotlin.Int = 56 // kotlin.Int | 
val adminTagUpdateRequest : AdminTagUpdateRequest =  // AdminTagUpdateRequest | 

launch(Dispatchers.IO) {
    val result : AdminServiceTag = webService.adminServiceTagUpdate(serviceTagId, adminTagUpdateRequest)
}
```

### Parameters
| **serviceTagId** | **kotlin.Int**|  | |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **adminTagUpdateRequest** | [**AdminTagUpdateRequest**](AdminTagUpdateRequest.md)|  | [optional] |

### Return type

[**AdminServiceTag**](AdminServiceTag.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


Delete a specialty no facility lists

One that a facility lists answers 409 &#x60;SPECIALTY_IN_USE&#x60;; retire it with &#x60;active &#x3D; false&#x60; instead.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminTaxonomyApi::class.java)
val specialtyId : kotlin.Int = 56 // kotlin.Int | 

launch(Dispatchers.IO) {
    webService.adminSpecialtyDelete(specialtyId)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **specialtyId** | **kotlin.Int**|  | |

### Return type

null (empty response body)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Rename, reorder, retire or bring back a specialty

Omitted fields keep their value. The scope is fixed: &#x60;scope&#x60;, &#x60;categoryId&#x60; and &#x60;specialization&#x60; are refused. A specialization&#39;s specialty changes for every category of that specialization.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminTaxonomyApi::class.java)
val specialtyId : kotlin.Int = 56 // kotlin.Int | 
val adminTagUpdateRequest : AdminTagUpdateRequest =  // AdminTagUpdateRequest | 

launch(Dispatchers.IO) {
    val result : AdminSpecialty = webService.adminSpecialtyUpdate(specialtyId, adminTagUpdateRequest)
}
```

### Parameters
| **specialtyId** | **kotlin.Int**|  | |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **adminTagUpdateRequest** | [**AdminTagUpdateRequest**](AdminTagUpdateRequest.md)|  | [optional] |

### Return type

[**AdminSpecialty**](AdminSpecialty.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

