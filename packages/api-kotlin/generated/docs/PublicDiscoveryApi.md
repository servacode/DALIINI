# PublicDiscoveryApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
| ------------- | ------------- | ------------- |
| [**publicDutyByDateList**](PublicDiscoveryApi.md#publicDutyByDateList) | **GET** api/v1/public/duty/ | Pharmacies on duty on a given day (or up to 7 days) |
| [**publicFacilitiesList**](PublicDiscoveryApi.md#publicFacilitiesList) | **GET** api/v1/public/facilities/ | List publicly visible facilities in a province, optionally in one category |
| [**publicFacilityRetrieve**](PublicDiscoveryApi.md#publicFacilityRetrieve) | **GET** api/v1/public/facilities/{facility_id}/ | Retrieve one publicly visible facility |
| [**publicHomeRetrieve**](PublicDiscoveryApi.md#publicHomeRetrieve) | **GET** api/v1/public/home/ | Retrieve the home composition for a province |
| [**publicMapFacilitiesList**](PublicDiscoveryApi.md#publicMapFacilitiesList) | **GET** api/v1/public/map/facilities/ | List compact map markers inside a viewport |
| [**publicSearchList**](PublicDiscoveryApi.md#publicSearchList) | **GET** api/v1/public/search/ | Search facilities within a province |



Pharmacies on duty on a given day (or up to 7 days)

Days are Damascus calendar days starting at &#x60;date&#x60; (default today). A pharmacy is listed on every day one of its duty shifts overlaps. Same visibility as the public duty-now listing. Cacheable for one minute.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
val webService = apiClient.createWebservice(PublicDiscoveryApi::class.java)
val provinceId : kotlin.String = provinceId_example // kotlin.String | 
val categoryId : kotlin.String = categoryId_example // kotlin.String | 
val cityId : kotlin.String = cityId_example // kotlin.String | 
val date : kotlin.String = date_example // kotlin.String | YYYY-MM-DD
val days : kotlin.Int = 56 // kotlin.Int | 1 to 7.

launch(Dispatchers.IO) {
    val result : PublicDutyRoster = webService.publicDutyByDateList(provinceId, categoryId, cityId, date, days)
}
```

### Parameters
| **provinceId** | **kotlin.String**|  | |
| **categoryId** | **kotlin.String**|  | [optional] |
| **cityId** | **kotlin.String**|  | [optional] |
| **date** | **kotlin.String**| YYYY-MM-DD | [optional] |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **days** | **kotlin.Int**| 1 to 7. | [optional] |

### Return type

[**PublicDutyRoster**](PublicDutyRoster.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


List publicly visible facilities in a province, optionally in one category

Ordered nearest-first when coordinates are supplied, otherwise by Arabic name. Availability is computed by the backend. The filters combine: openNow and dutyToday together mean facilities that are both, which is a different question from either alone.

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
val categoryId : kotlin.String = categoryId_example // kotlin.String | Optional category to list. Absent means the whole province.
val cityId : kotlin.String = cityId_example // kotlin.String | Optional city filter.
val cursor : kotlin.String = cursor_example // kotlin.String | Opaque token returned as `nextCursor` by the previous page.
val dutyNow : kotlin.String = dutyNow_example // kotlin.String | Pass true to keep only facilities whose duty shift is running.
val dutyToday : kotlin.String = dutyToday_example // kotlin.String | Pass true to keep only facilities on today's duty roster.
val latitude : kotlin.String = latitude_example // kotlin.String | Caller latitude in WGS84 decimal degrees. Must be sent with longitude.
val limit : kotlin.Int = 56 // kotlin.Int | Page size, maximum 100, default 30.
val longitude : kotlin.String = longitude_example // kotlin.String | Caller longitude in WGS84 decimal degrees. Must be sent with latitude.
val neighborhoodId : kotlin.String = neighborhoodId_example // kotlin.String | Optional neighbourhood filter.
val openNow : kotlin.String = openNow_example // kotlin.String | Pass true to keep only facilities open at this moment.
val search : kotlin.String = search_example // kotlin.String | Free-text term matched against facility text.
val serviceId : kotlin.Int = 56 // kotlin.Int | The earlier name of serviceTagId, still accepted; it behaves the same way.
val serviceTagId : kotlin.Int = 56 // kotlin.Int | Optional service filter, an id from publicCategoryTagsRetrieve. Only facilities whose category declares serviceFilter can match. Anything but a positive whole number is refused with 400.
val sort : kotlin.String = sort_example // kotlin.String | nearest orders by distance and needs coordinates; name orders by Arabic name. Omitted keeps the historical behaviour: nearest whenever coordinates are supplied, name otherwise. Distances are returned whenever coordinates are supplied, whichever ordering is asked for.
val specialtyId : kotlin.Int = 56 // kotlin.Int | Optional specialty filter, an id from publicCategoryTagsRetrieve. Only facilities whose category declares specialtyFilter can match. Anything but a positive whole number is refused with 400.

launch(Dispatchers.IO) {
    val result : FacilityCursorPage = webService.publicFacilitiesList(provinceId, bbox, categoryId, cityId, cursor, dutyNow, dutyToday, latitude, limit, longitude, neighborhoodId, openNow, search, serviceId, serviceTagId, sort, specialtyId)
}
```

### Parameters
| **provinceId** | **kotlin.String**| Province to scope the query to. | |
| **bbox** | **kotlin.String**| Viewport as west,south,east,north in WGS84 decimal degrees. | [optional] |
| **categoryId** | **kotlin.String**| Optional category to list. Absent means the whole province. | [optional] |
| **cityId** | **kotlin.String**| Optional city filter. | [optional] |
| **cursor** | **kotlin.String**| Opaque token returned as &#x60;nextCursor&#x60; by the previous page. | [optional] |
| **dutyNow** | **kotlin.String**| Pass true to keep only facilities whose duty shift is running. | [optional] |
| **dutyToday** | **kotlin.String**| Pass true to keep only facilities on today&#39;s duty roster. | [optional] |
| **latitude** | **kotlin.String**| Caller latitude in WGS84 decimal degrees. Must be sent with longitude. | [optional] |
| **limit** | **kotlin.Int**| Page size, maximum 100, default 30. | [optional] |
| **longitude** | **kotlin.String**| Caller longitude in WGS84 decimal degrees. Must be sent with latitude. | [optional] |
| **neighborhoodId** | **kotlin.String**| Optional neighbourhood filter. | [optional] |
| **openNow** | **kotlin.String**| Pass true to keep only facilities open at this moment. | [optional] |
| **search** | **kotlin.String**| Free-text term matched against facility text. | [optional] |
| **serviceId** | **kotlin.Int**| The earlier name of serviceTagId, still accepted; it behaves the same way. | [optional] |
| **serviceTagId** | **kotlin.Int**| Optional service filter, an id from publicCategoryTagsRetrieve. Only facilities whose category declares serviceFilter can match. Anything but a positive whole number is refused with 400. | [optional] |
| **sort** | **kotlin.String**| nearest orders by distance and needs coordinates; name orders by Arabic name. Omitted keeps the historical behaviour: nearest whenever coordinates are supplied, name otherwise. Distances are returned whenever coordinates are supplied, whichever ordering is asked for. | [optional] |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **specialtyId** | **kotlin.Int**| Optional specialty filter, an id from publicCategoryTagsRetrieve. Only facilities whose category declares specialtyFilter can match. Anything but a positive whole number is refused with 400. | [optional] |

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
val serviceId : kotlin.Int = 56 // kotlin.Int | The earlier name of serviceTagId, still accepted; it behaves the same way.
val serviceTagId : kotlin.Int = 56 // kotlin.Int | Optional service filter, an id from publicCategoryTagsRetrieve. Only facilities whose category declares serviceFilter can match. Anything but a positive whole number is refused with 400.
val specialtyId : kotlin.Int = 56 // kotlin.Int | Optional specialty filter, an id from publicCategoryTagsRetrieve. Only facilities whose category declares specialtyFilter can match. Anything but a positive whole number is refused with 400.

launch(Dispatchers.IO) {
    val result : PublicHome = webService.publicHomeRetrieve(provinceId, bbox, categoryId, cityId, latitude, longitude, neighborhoodId, search, serviceId, serviceTagId, specialtyId)
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
| **serviceId** | **kotlin.Int**| The earlier name of serviceTagId, still accepted; it behaves the same way. | [optional] |
| **serviceTagId** | **kotlin.Int**| Optional service filter, an id from publicCategoryTagsRetrieve. Only facilities whose category declares serviceFilter can match. Anything but a positive whole number is refused with 400. | [optional] |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **specialtyId** | **kotlin.Int**| Optional specialty filter, an id from publicCategoryTagsRetrieve. Only facilities whose category declares specialtyFilter can match. Anything but a positive whole number is refused with 400. | [optional] |

### Return type

[**PublicHome**](PublicHome.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


List compact map markers inside a viewport

Capped at 500 markers. Facilities without coordinates are omitted. The filters behave exactly as they do on the list endpoint and combine the same way, so a map and a list asked the same question answer the same.

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
val dutyNow : kotlin.String = dutyNow_example // kotlin.String | Pass true to keep only facilities whose duty shift is running.
val dutyToday : kotlin.String = dutyToday_example // kotlin.String | Pass true to keep only facilities on today's duty roster.
val latitude : kotlin.String = latitude_example // kotlin.String | Caller latitude in WGS84 decimal degrees. Must be sent with longitude.
val longitude : kotlin.String = longitude_example // kotlin.String | Caller longitude in WGS84 decimal degrees. Must be sent with latitude.
val neighborhoodId : kotlin.String = neighborhoodId_example // kotlin.String | Optional neighbourhood filter.
val openNow : kotlin.String = openNow_example // kotlin.String | Pass true to keep only facilities open at this moment.
val search : kotlin.String = search_example // kotlin.String | Free-text term matched against facility text.
val serviceId : kotlin.Int = 56 // kotlin.Int | The earlier name of serviceTagId, still accepted; it behaves the same way.
val serviceTagId : kotlin.Int = 56 // kotlin.Int | Optional service filter, an id from publicCategoryTagsRetrieve. Only facilities whose category declares serviceFilter can match. Anything but a positive whole number is refused with 400.
val specialtyId : kotlin.Int = 56 // kotlin.Int | Optional specialty filter, an id from publicCategoryTagsRetrieve. Only facilities whose category declares specialtyFilter can match. Anything but a positive whole number is refused with 400.

launch(Dispatchers.IO) {
    val result : MapMarkerList = webService.publicMapFacilitiesList(provinceId, bbox, categoryId, cityId, dutyNow, dutyToday, latitude, longitude, neighborhoodId, openNow, search, serviceId, serviceTagId, specialtyId)
}
```

### Parameters
| **provinceId** | **kotlin.String**| Province to scope the query to. | |
| **bbox** | **kotlin.String**| Viewport as west,south,east,north in WGS84 decimal degrees. | [optional] |
| **categoryId** | **kotlin.String**| Optional category filter. | [optional] |
| **cityId** | **kotlin.String**| Optional city filter. | [optional] |
| **dutyNow** | **kotlin.String**| Pass true to keep only facilities whose duty shift is running. | [optional] |
| **dutyToday** | **kotlin.String**| Pass true to keep only facilities on today&#39;s duty roster. | [optional] |
| **latitude** | **kotlin.String**| Caller latitude in WGS84 decimal degrees. Must be sent with longitude. | [optional] |
| **longitude** | **kotlin.String**| Caller longitude in WGS84 decimal degrees. Must be sent with latitude. | [optional] |
| **neighborhoodId** | **kotlin.String**| Optional neighbourhood filter. | [optional] |
| **openNow** | **kotlin.String**| Pass true to keep only facilities open at this moment. | [optional] |
| **search** | **kotlin.String**| Free-text term matched against facility text. | [optional] |
| **serviceId** | **kotlin.Int**| The earlier name of serviceTagId, still accepted; it behaves the same way. | [optional] |
| **serviceTagId** | **kotlin.Int**| Optional service filter, an id from publicCategoryTagsRetrieve. Only facilities whose category declares serviceFilter can match. Anything but a positive whole number is refused with 400. | [optional] |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **specialtyId** | **kotlin.Int**| Optional specialty filter, an id from publicCategoryTagsRetrieve. Only facilities whose category declares specialtyFilter can match. Anything but a positive whole number is refused with 400. | [optional] |

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
val cursor : kotlin.String = cursor_example // kotlin.String | Opaque token returned as `nextCursor` by the previous page.
val latitude : kotlin.String = latitude_example // kotlin.String | Caller latitude in WGS84 decimal degrees. Must be sent with longitude.
val limit : kotlin.Int = 56 // kotlin.Int | Page size, maximum 100, default 30.
val longitude : kotlin.String = longitude_example // kotlin.String | Caller longitude in WGS84 decimal degrees. Must be sent with latitude.
val neighborhoodId : kotlin.String = neighborhoodId_example // kotlin.String | Optional neighbourhood filter.
val search : kotlin.String = search_example // kotlin.String | Free-text term matched against facility text.
val serviceId : kotlin.Int = 56 // kotlin.Int | The earlier name of serviceTagId, still accepted; it behaves the same way.
val serviceTagId : kotlin.Int = 56 // kotlin.Int | Optional service filter, an id from publicCategoryTagsRetrieve. Only facilities whose category declares serviceFilter can match. Anything but a positive whole number is refused with 400.
val specialtyId : kotlin.Int = 56 // kotlin.Int | Optional specialty filter, an id from publicCategoryTagsRetrieve. Only facilities whose category declares specialtyFilter can match. Anything but a positive whole number is refused with 400.

launch(Dispatchers.IO) {
    val result : FacilityCursorPage = webService.publicSearchList(provinceId, q, bbox, categoryId, cityId, cursor, latitude, limit, longitude, neighborhoodId, search, serviceId, serviceTagId, specialtyId)
}
```

### Parameters
| **provinceId** | **kotlin.String**| Province to scope the query to. | |
| **q** | **kotlin.String**| Search term, at least two characters. | |
| **bbox** | **kotlin.String**| Viewport as west,south,east,north in WGS84 decimal degrees. | [optional] |
| **categoryId** | **kotlin.String**| Optional category filter. | [optional] |
| **cityId** | **kotlin.String**| Optional city filter. | [optional] |
| **cursor** | **kotlin.String**| Opaque token returned as &#x60;nextCursor&#x60; by the previous page. | [optional] |
| **latitude** | **kotlin.String**| Caller latitude in WGS84 decimal degrees. Must be sent with longitude. | [optional] |
| **limit** | **kotlin.Int**| Page size, maximum 100, default 30. | [optional] |
| **longitude** | **kotlin.String**| Caller longitude in WGS84 decimal degrees. Must be sent with latitude. | [optional] |
| **neighborhoodId** | **kotlin.String**| Optional neighbourhood filter. | [optional] |
| **search** | **kotlin.String**| Free-text term matched against facility text. | [optional] |
| **serviceId** | **kotlin.Int**| The earlier name of serviceTagId, still accepted; it behaves the same way. | [optional] |
| **serviceTagId** | **kotlin.Int**| Optional service filter, an id from publicCategoryTagsRetrieve. Only facilities whose category declares serviceFilter can match. Anything but a positive whole number is refused with 400. | [optional] |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **specialtyId** | **kotlin.Int**| Optional specialty filter, an id from publicCategoryTagsRetrieve. Only facilities whose category declares specialtyFilter can match. Anything but a positive whole number is refused with 400. | [optional] |

### Return type

[**FacilityCursorPage**](FacilityCursorPage.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

