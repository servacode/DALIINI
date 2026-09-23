# PublicTaxonomyApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
| ------------- | ------------- | ------------- |
| [**publicLocationResolve**](PublicTaxonomyApi.md#publicLocationResolve) | **GET** api/v1/public/locations/resolve/ | Resolve a coordinate to a province, city and neighbourhood |
| [**publicProvinceCategoriesList**](PublicTaxonomyApi.md#publicProvinceCategoriesList) | **GET** api/v1/public/provinces/{province_id}/categories/ | List categories publicly enabled for a province |
| [**publicProvinceCitiesList**](PublicTaxonomyApi.md#publicProvinceCitiesList) | **GET** api/v1/public/provinces/{province_id}/cities/ | List active cities in a province |
| [**publicProvincesList**](PublicTaxonomyApi.md#publicProvincesList) | **GET** api/v1/public/provinces/ | List active provinces |



Resolve a coordinate to a province, city and neighbourhood

Point-in-polygon against the seeded city and neighbourhood boundaries, then the nearest active province centre within 200 km. No external geocoder is called and the coordinate is not stored.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
val webService = apiClient.createWebservice(PublicTaxonomyApi::class.java)
val latitude : kotlin.Double = 1.2 // kotlin.Double | 
val longitude : kotlin.Double = 1.2 // kotlin.Double | 

launch(Dispatchers.IO) {
    val result : PublicLocationResolve = webService.publicLocationResolve(latitude, longitude)
}
```

### Parameters
| **latitude** | **kotlin.Double**|  | |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **longitude** | **kotlin.Double**|  | |

### Return type

[**PublicLocationResolve**](PublicLocationResolve.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


List categories publicly enabled for a province

A category is listed only when the province is active, the group and the category are active, and the per-province public switch is on. Clients drive their UI from the returned capability flags, never from the category name.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
val webService = apiClient.createWebservice(PublicTaxonomyApi::class.java)
val provinceId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 

launch(Dispatchers.IO) {
    val result : PublicCategoryList = webService.publicProvinceCategoriesList(provinceId)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **provinceId** | **java.util.UUID**|  | |

### Return type

[**PublicCategoryList**](PublicCategoryList.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


List active cities in a province

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
val webService = apiClient.createWebservice(PublicTaxonomyApi::class.java)
val provinceId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 

launch(Dispatchers.IO) {
    val result : PublicCityList = webService.publicProvinceCitiesList(provinceId)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **provinceId** | **java.util.UUID**|  | |

### Return type

[**PublicCityList**](PublicCityList.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


List active provinces

Every province is seeded, but only active ones are publicly visible. Ordered by sort order then Arabic name.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
val webService = apiClient.createWebservice(PublicTaxonomyApi::class.java)

launch(Dispatchers.IO) {
    val result : PublicProvinceList = webService.publicProvincesList()
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**PublicProvinceList**](PublicProvinceList.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

