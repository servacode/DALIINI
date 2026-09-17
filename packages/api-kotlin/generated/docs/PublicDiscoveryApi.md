# PublicDiscoveryApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
| ------------- | ------------- | ------------- |
| [**publicFacilitiesList**](PublicDiscoveryApi.md#publicFacilitiesList) | **GET** api/v1/public/facilities/ | List publicly visible facilities in a province and category |
| [**publicFacilityRetrieve**](PublicDiscoveryApi.md#publicFacilityRetrieve) | **GET** api/v1/public/facilities/{facility_id}/ | Retrieve one publicly visible facility |
| [**publicHomeRetrieve**](PublicDiscoveryApi.md#publicHomeRetrieve) | **GET** api/v1/public/home/ | Retrieve the home composition for a province |
| [**publicMapFacilitiesList**](PublicDiscoveryApi.md#publicMapFacilitiesList) | **GET** api/v1/public/map/facilities/ | List compact map markers inside a viewport |
| [**publicSearchList**](PublicDiscoveryApi.md#publicSearchList) | **GET** api/v1/public/search/ | Search facilities within a province |



List publicly visible facilities in a province and category

Ordered nearest-first when coordinates are supplied, otherwise by Arabic name. Availability is computed by the backend; openNow and dutyNow filter on that computed state rather than on a stored flag.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(PublicDiscoveryApi::class.java)
val categoryId : kotlin.String = categoryId_example // kotlin.String | Category to list. Required.
val provinceId : kotlin.String = provinceId_example // kotlin.String | Province to scope the query to.
val bbox : kotlin.String = bbox_example // kotlin.String | Viewport as west,south,east,north in WGS84 decimal degrees.
val cityId : kotlin.String = cityId_example // kotlin.String | Optional city filter.
val cursor : kotlin.String = cursor_example // kotlin.String | Opaque cursor returned in the previous page's next link.
val dutyNow : kotlin.String = dutyNow_example // kotlin.String | Pass true to keep only facilities currently on duty.
val latitude : kotlin.String = latitude_example // kotlin.String | Caller latitude in WGS84 decimal degrees. Must be sent with longitude.
val limit : kotlin.Int = 56 // kotlin.Int | Page size, maximum 100, default 30.
val longitude : kotlin.String = longitude_example // kotlin.String | Caller longitude in WGS84 decimal degrees. Must be sent with latitude.
val neighborhoodId : kotlin.String = neighborhoodId_example // kotlin.String | Optional neighbourhood filter.
val openNow : kotlin.String = openNow_example // kotlin.String | Pass true to keep only facilities currently open.
val search : kotlin.String = search_example // kotlin.String | Free-text term matched against facility text.
val serviceId : kotlin.String = serviceId_example // kotlin.String | Optional service-tag filter; only meaningful when the category declares serviceFilter.
val specialtyId : kotlin.String = specialtyId_example // kotlin.String | Optional specialty filter; only meaningful when the category declares specialtyFilter.

launch(Dispatchers.IO) {
    val result : FacilityCursorPage = webService.publicFacilitiesList(categoryId, provinceId, bbox, cityId, cursor, dutyNow, latitude, limit, longitude, neighborhoodId, openNow, search, serviceId, specialtyId)
}
```

### Parameters
| **categoryId** | **kotlin.String**| Category to list. Required. | |
| **provinceId** | **kotlin.String**| Province to scope the query to. | |
| **bbox** | **kotlin.String**| Viewport as west,south,east,north in WGS84 decimal degrees. | [optional] |
| **cityId** | **kotlin.String**| Optional city filter. | [optional] |
| **cursor** | **kotlin.String**| Opaque cursor returned in the previous page&#39;s next link. | [optional] |
| **dutyNow** | **kotlin.String**| Pass true to keep only facilities currently on duty. | [optional] |
| **latitude** | **kotlin.String**| Caller latitude in WGS84 decimal degrees. Must be sent with longitude. | [optional] |
| **limit** | **kotlin.Int**| Page size, maximum 100, default 30. | [optional] |
| **longitude** | **kotlin.String**| Caller longitude in WGS84 decimal degrees. Must be sent with latitude. | [optional] |
| **neighborhoodId** | **kotlin.String**| Optional neighbourhood filter. | [optional] |
| **openNow** | **kotlin.String**| Pass true to keep only facilities currently open. | [optional] |
| **search** | **kotlin.String**| Free-text term matched against facility text. | [optional] |
| **serviceId** | **kotlin.String**| Optional service-tag filter; only meaningful when the category declares serviceFilter. | [optional] |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **specialtyId** | **kotlin.String**| Optional specialty filter; only meaningful when the category declares specialtyFilter. | [optional] |

### Return type

[**FacilityCursorPage**](FacilityCursorPage.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Retrieve one publicly visible facility

Returns the public projection only. Verification evidence, reviewer notes, memberships, internal policy fields and raw storage keys are never included.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(PublicDiscoveryApi::class.java)
val facilityId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 

launch(Dispatchers.IO) {
    val result : PublicFacilityDetail = webService.publicFacilityRetrieve(facilityId)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **facilityId** | **java.util.UUID**|  | |

### Return type

[**PublicFacilityDetail**](PublicFacilityDetail.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Retrieve the home composition for a province

Bundles advertisements, the active category grid and three facility strips so the first screen needs one round trip. serverTime is authoritative.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(PublicDiscoveryApi::class.java)
val provinceId : kotlin.String = provinceId_example // kotlin.String | Province to scope the query to.
val bbox : kotlin.String = bbox_example // kotlin.String | Viewport as west,south,east,north in WGS84 decimal degrees.
val categoryId : kotlin.String = categoryId_example // kotlin.String | Optional category filter.
val cityId : kotlin.String = cityId_example // kotlin.String | Optional city filter.
val latitude : kotlin.String = latitude_example // kotlin.String | Caller latitude in WGS84 decimal degrees. Must be sent with longitude.
val longitude : kotlin.String = longitude_example // kotlin.String | Caller longitude in WGS84 decimal degrees. Must be sent with latitude.
val neighborhoodId : kotlin.String = neighborhoodId_example // kotlin.String | Optional neighbourhood filter.
val search : kotlin.String = search_example // kotlin.String | Free-text term matched against facility text.
val serviceId : kotlin.String = serviceId_example // kotlin.String | Optional service-tag filter; only meaningful when the category declares serviceFilter.
val specialtyId : kotlin.String = specialtyId_example // kotlin.String | Optional specialty filter; only meaningful when the category declares specialtyFilter.

launch(Dispatchers.IO) {
    val result : PublicHome = webService.publicHomeRetrieve(provinceId, bbox, categoryId, cityId, latitude, longitude, neighborhoodId, search, serviceId, specialtyId)
}
```

### Parameters
| **provinceId** | **kotlin.String**| Province to scope the query to. | |
| **bbox** | **kotlin.String**| Viewport as west,south,east,north in WGS84 decimal degrees. | [optional] |
| **categoryId** | **kotlin.String**| Optional category filter. | [optional] |
| **cityId** | **kotlin.String**| Optional city filter. | [optional] |
| **latitude** | **kotlin.String**| Caller latitude in WGS84 decimal degrees. Must be sent with longitude. | [optional] |
| **longitude** | **kotlin.String**| Caller longitude in WGS84 decimal degrees. Must be sent with latitude. | [optional] |
| **neighborhoodId** | **kotlin.String**| Optional neighbourhood filter. | [optional] |
| **search** | **kotlin.String**| Free-text term matched against facility text. | [optional] |
| **serviceId** | **kotlin.String**| Optional service-tag filter; only meaningful when the category declares serviceFilter. | [optional] |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **specialtyId** | **kotlin.String**| Optional specialty filter; only meaningful when the category declares specialtyFilter. | [optional] |

### Return type

[**PublicHome**](PublicHome.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


List compact map markers inside a viewport

Capped at 500 markers. Facilities without coordinates are omitted.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(PublicDiscoveryApi::class.java)
val provinceId : kotlin.String = provinceId_example // kotlin.String | Province to scope the query to.
val bbox : kotlin.String = bbox_example // kotlin.String | Viewport as west,south,east,north in WGS84 decimal degrees.
val categoryId : kotlin.String = categoryId_example // kotlin.String | Optional category filter.
val cityId : kotlin.String = cityId_example // kotlin.String | Optional city filter.
val latitude : kotlin.String = latitude_example // kotlin.String | Caller latitude in WGS84 decimal degrees. Must be sent with longitude.
val longitude : kotlin.String = longitude_example // kotlin.String | Caller longitude in WGS84 decimal degrees. Must be sent with latitude.
val neighborhoodId : kotlin.String = neighborhoodId_example // kotlin.String | Optional neighbourhood filter.
val search : kotlin.String = search_example // kotlin.String | Free-text term matched against facility text.
val serviceId : kotlin.String = serviceId_example // kotlin.String | Optional service-tag filter; only meaningful when the category declares serviceFilter.
val specialtyId : kotlin.String = specialtyId_example // kotlin.String | Optional specialty filter; only meaningful when the category declares specialtyFilter.

launch(Dispatchers.IO) {
    val result : MapMarkerList = webService.publicMapFacilitiesList(provinceId, bbox, categoryId, cityId, latitude, longitude, neighborhoodId, search, serviceId, specialtyId)
}
```

### Parameters
| **provinceId** | **kotlin.String**| Province to scope the query to. | |
| **bbox** | **kotlin.String**| Viewport as west,south,east,north in WGS84 decimal degrees. | [optional] |
| **categoryId** | **kotlin.String**| Optional category filter. | [optional] |
| **cityId** | **kotlin.String**| Optional city filter. | [optional] |
| **latitude** | **kotlin.String**| Caller latitude in WGS84 decimal degrees. Must be sent with longitude. | [optional] |
| **longitude** | **kotlin.String**| Caller longitude in WGS84 decimal degrees. Must be sent with latitude. | [optional] |
| **neighborhoodId** | **kotlin.String**| Optional neighbourhood filter. | [optional] |
| **search** | **kotlin.String**| Free-text term matched against facility text. | [optional] |
| **serviceId** | **kotlin.String**| Optional service-tag filter; only meaningful when the category declares serviceFilter. | [optional] |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **specialtyId** | **kotlin.String**| Optional specialty filter; only meaningful when the category declares specialtyFilter. | [optional] |

### Return type

[**MapMarkerList**](MapMarkerList.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Search facilities within a province

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(PublicDiscoveryApi::class.java)
val provinceId : kotlin.String = provinceId_example // kotlin.String | Province to scope the query to.
val q : kotlin.String = q_example // kotlin.String | Search term, at least two characters.
val bbox : kotlin.String = bbox_example // kotlin.String | Viewport as west,south,east,north in WGS84 decimal degrees.
val categoryId : kotlin.String = categoryId_example // kotlin.String | Optional category filter.
val cityId : kotlin.String = cityId_example // kotlin.String | Optional city filter.
val cursor : kotlin.String = cursor_example // kotlin.String | Opaque cursor returned in the previous page's next link.
val latitude : kotlin.String = latitude_example // kotlin.String | Caller latitude in WGS84 decimal degrees. Must be sent with longitude.
val limit : kotlin.Int = 56 // kotlin.Int | Page size, maximum 100, default 30.
val longitude : kotlin.String = longitude_example // kotlin.String | Caller longitude in WGS84 decimal degrees. Must be sent with latitude.
val neighborhoodId : kotlin.String = neighborhoodId_example // kotlin.String | Optional neighbourhood filter.
val search : kotlin.String = search_example // kotlin.String | Free-text term matched against facility text.
val serviceId : kotlin.String = serviceId_example // kotlin.String | Optional service-tag filter; only meaningful when the category declares serviceFilter.
val specialtyId : kotlin.String = specialtyId_example // kotlin.String | Optional specialty filter; only meaningful when the category declares specialtyFilter.

launch(Dispatchers.IO) {
    val result : FacilityCursorPage = webService.publicSearchList(provinceId, q, bbox, categoryId, cityId, cursor, latitude, limit, longitude, neighborhoodId, search, serviceId, specialtyId)
}
```

### Parameters
| **provinceId** | **kotlin.String**| Province to scope the query to. | |
| **q** | **kotlin.String**| Search term, at least two characters. | |
| **bbox** | **kotlin.String**| Viewport as west,south,east,north in WGS84 decimal degrees. | [optional] |
| **categoryId** | **kotlin.String**| Optional category filter. | [optional] |
| **cityId** | **kotlin.String**| Optional city filter. | [optional] |
| **cursor** | **kotlin.String**| Opaque cursor returned in the previous page&#39;s next link. | [optional] |
| **latitude** | **kotlin.String**| Caller latitude in WGS84 decimal degrees. Must be sent with longitude. | [optional] |
| **limit** | **kotlin.Int**| Page size, maximum 100, default 30. | [optional] |
| **longitude** | **kotlin.String**| Caller longitude in WGS84 decimal degrees. Must be sent with latitude. | [optional] |
| **neighborhoodId** | **kotlin.String**| Optional neighbourhood filter. | [optional] |
| **search** | **kotlin.String**| Free-text term matched against facility text. | [optional] |
| **serviceId** | **kotlin.String**| Optional service-tag filter; only meaningful when the category declares serviceFilter. | [optional] |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **specialtyId** | **kotlin.String**| Optional specialty filter; only meaningful when the category declares specialtyFilter. | [optional] |

### Return type

[**FacilityCursorPage**](FacilityCursorPage.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

