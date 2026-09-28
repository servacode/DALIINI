# AdminProvincesApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
| ------------- | ------------- | ------------- |
| [**adminProvinceCitiesList**](AdminProvincesApi.md#adminProvinceCitiesList) | **GET** api/v1/admin/provinces/{province_id}/cities/ | List every city of a province, active or not |
| [**adminProvinceCityUpdate**](AdminProvincesApi.md#adminProvinceCityUpdate) | **PUT** api/v1/admin/provinces/{province_id}/cities/{city_id}/ | Activate or deactivate a city |
| [**adminProvinceReadinessRetrieve**](AdminProvincesApi.md#adminProvinceReadinessRetrieve) | **GET** api/v1/admin/provinces/{province_id}/readiness/ | Launch checklist for a province |
| [**adminProvinceUpdate**](AdminProvincesApi.md#adminProvinceUpdate) | **PUT** api/v1/admin/provinces/{province_id}/ | Activate a province or change its order |
| [**adminProvincesList**](AdminProvincesApi.md#adminProvincesList) | **GET** api/v1/admin/provinces/ | List every province |



List every city of a province, active or not

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminProvincesApi::class.java)
val provinceId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 

launch(Dispatchers.IO) {
    val result : AdminCityAdminList = webService.adminProvinceCitiesList(provinceId)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **provinceId** | **java.util.UUID**|  | |

### Return type

[**AdminCityAdminList**](AdminCityAdminList.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Activate or deactivate a city

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminProvincesApi::class.java)
val cityId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 
val provinceId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 
val adminCityUpdateRequest : AdminCityUpdateRequest =  // AdminCityUpdateRequest | 

launch(Dispatchers.IO) {
    val result : AdminCityAdmin = webService.adminProvinceCityUpdate(cityId, provinceId, adminCityUpdateRequest)
}
```

### Parameters
| **cityId** | **java.util.UUID**|  | |
| **provinceId** | **java.util.UUID**|  | |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **adminCityUpdateRequest** | [**AdminCityUpdateRequest**](AdminCityUpdateRequest.md)|  | |

### Return type

[**AdminCityAdmin**](AdminCityAdmin.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


Launch checklist for a province

PROVINCE_ACTIVE; CATEGORY_PUBLIC (at least one active category publicly enabled); MIN_ACTIVE_FACILITIES (platform setting &#x60;readiness.minActiveFacilities&#x60;, default 5); DUTY_COVERAGE (no DUTY_GAP in the next 14 days, or not applicable when the province offers no duty category); EMERGENCY_NUMBERS (an active national or provincial number).

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminProvincesApi::class.java)
val provinceId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 

launch(Dispatchers.IO) {
    val result : AdminProvinceReadiness = webService.adminProvinceReadinessRetrieve(provinceId)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **provinceId** | **java.util.UUID**|  | |

### Return type

[**AdminProvinceReadiness**](AdminProvinceReadiness.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Activate a province or change its order

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminProvincesApi::class.java)
val provinceId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 
val adminProvinceUpdateRequest : AdminProvinceUpdateRequest =  // AdminProvinceUpdateRequest | 

launch(Dispatchers.IO) {
    val result : AdminProvinceUpdated = webService.adminProvinceUpdate(provinceId, adminProvinceUpdateRequest)
}
```

### Parameters
| **provinceId** | **java.util.UUID**|  | |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **adminProvinceUpdateRequest** | [**AdminProvinceUpdateRequest**](AdminProvinceUpdateRequest.md)|  | [optional] |

### Return type

[**AdminProvinceUpdated**](AdminProvinceUpdated.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


List every province

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminProvincesApi::class.java)

launch(Dispatchers.IO) {
    val result : AdminProvinceList = webService.adminProvincesList()
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**AdminProvinceList**](AdminProvinceList.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

