# PublicDiscoveryAPI

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**publicDutyByDateList**](PublicDiscoveryAPI.md#publicdutybydatelist) | **GET** /api/v1/public/duty/ | Pharmacies on duty on a given day (or up to 7 days)
[**publicFacilitiesList**](PublicDiscoveryAPI.md#publicfacilitieslist) | **GET** /api/v1/public/facilities/ | List publicly visible facilities in a province, optionally in one category
[**publicFacilityRetrieve**](PublicDiscoveryAPI.md#publicfacilityretrieve) | **GET** /api/v1/public/facilities/{facility_id}/ | Retrieve one publicly visible facility
[**publicHomeRetrieve**](PublicDiscoveryAPI.md#publichomeretrieve) | **GET** /api/v1/public/home/ | Retrieve the home composition for a province
[**publicMapFacilitiesList**](PublicDiscoveryAPI.md#publicmapfacilitieslist) | **GET** /api/v1/public/map/facilities/ | List compact map markers inside a viewport
[**publicSearchList**](PublicDiscoveryAPI.md#publicsearchlist) | **GET** /api/v1/public/search/ | Search facilities within a province


# **publicDutyByDateList**
```swift
    open class func publicDutyByDateList(provinceId: String, categoryId: String? = nil, cityId: String? = nil, date: String? = nil, days: Int? = nil, completion: @escaping (_ data: PublicDutyRoster?, _ error: Error?) -> Void)
```

Pharmacies on duty on a given day (or up to 7 days)

Days are Damascus calendar days starting at `date` (default today). A pharmacy is listed on every day one of its duty shifts overlaps. Same visibility as the public duty-now listing. Cacheable for one minute.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let provinceId = "provinceId_example" // String | 
let categoryId = "categoryId_example" // String |  (optional)
let cityId = "cityId_example" // String |  (optional)
let date = "date_example" // String | YYYY-MM-DD (optional)
let days = 987 // Int | 1 to 7. (optional)

// Pharmacies on duty on a given day (or up to 7 days)
PublicDiscoveryAPI.publicDutyByDateList(provinceId: provinceId, categoryId: categoryId, cityId: cityId, date: date, days: days) { (response, error) in
    guard error == nil else {
        print(error)
        return
    }

    if (response) {
        dump(response)
    }
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **provinceId** | **String** |  | 
 **categoryId** | **String** |  | [optional] 
 **cityId** | **String** |  | [optional] 
 **date** | **String** | YYYY-MM-DD | [optional] 
 **days** | **Int** | 1 to 7. | [optional] 

### Return type

[**PublicDutyRoster**](PublicDutyRoster.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **publicFacilitiesList**
```swift
    open class func publicFacilitiesList(provinceId: String, bbox: String? = nil, categoryId: String? = nil, cityId: String? = nil, cursor: String? = nil, dutyNow: String? = nil, dutyToday: String? = nil, latitude: String? = nil, limit: Int? = nil, longitude: String? = nil, neighborhoodId: String? = nil, openNow: String? = nil, search: String? = nil, serviceId: Int? = nil, serviceTagId: Int? = nil, sort: String? = nil, specialtyId: Int? = nil, completion: @escaping (_ data: FacilityCursorPage?, _ error: Error?) -> Void)
```

List publicly visible facilities in a province, optionally in one category

Ordered nearest-first when coordinates are supplied, otherwise by Arabic name. Availability is computed by the backend. The filters combine: openNow and dutyToday together mean facilities that are both, which is a different question from either alone.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let provinceId = "provinceId_example" // String | Province to scope the query to.
let bbox = "bbox_example" // String | Viewport as west,south,east,north in WGS84 decimal degrees. (optional)
let categoryId = "categoryId_example" // String | Optional category to list. Absent means the whole province. (optional)
let cityId = "cityId_example" // String | Optional city filter. (optional)
let cursor = "cursor_example" // String | Opaque token returned as `nextCursor` by the previous page. (optional)
let dutyNow = "dutyNow_example" // String | Pass true to keep only facilities whose duty shift is running. (optional)
let dutyToday = "dutyToday_example" // String | Pass true to keep only facilities on today's duty roster. (optional)
let latitude = "latitude_example" // String | Caller latitude in WGS84 decimal degrees. Must be sent with longitude. (optional)
let limit = 987 // Int | Page size, maximum 100, default 30. (optional)
let longitude = "longitude_example" // String | Caller longitude in WGS84 decimal degrees. Must be sent with latitude. (optional)
let neighborhoodId = "neighborhoodId_example" // String | Optional neighbourhood filter. (optional)
let openNow = "openNow_example" // String | Pass true to keep only facilities open at this moment. (optional)
let search = "search_example" // String | Free-text term matched against facility text. (optional)
let serviceId = 987 // Int | The earlier name of serviceTagId, still accepted; it behaves the same way. (optional)
let serviceTagId = 987 // Int | Optional service filter, an id from publicCategoryTagsRetrieve. Only facilities whose category declares serviceFilter can match. Anything but a positive whole number is refused with 400. (optional)
let sort = "sort_example" // String | nearest orders by distance and needs coordinates; name orders by Arabic name. Omitted keeps the historical behaviour: nearest whenever coordinates are supplied, name otherwise. Distances are returned whenever coordinates are supplied, whichever ordering is asked for. (optional)
let specialtyId = 987 // Int | Optional specialty filter, an id from publicCategoryTagsRetrieve. Only facilities whose category declares specialtyFilter can match. Anything but a positive whole number is refused with 400. (optional)

// List publicly visible facilities in a province, optionally in one category
PublicDiscoveryAPI.publicFacilitiesList(provinceId: provinceId, bbox: bbox, categoryId: categoryId, cityId: cityId, cursor: cursor, dutyNow: dutyNow, dutyToday: dutyToday, latitude: latitude, limit: limit, longitude: longitude, neighborhoodId: neighborhoodId, openNow: openNow, search: search, serviceId: serviceId, serviceTagId: serviceTagId, sort: sort, specialtyId: specialtyId) { (response, error) in
    guard error == nil else {
        print(error)
        return
    }

    if (response) {
        dump(response)
    }
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **provinceId** | **String** | Province to scope the query to. | 
 **bbox** | **String** | Viewport as west,south,east,north in WGS84 decimal degrees. | [optional] 
 **categoryId** | **String** | Optional category to list. Absent means the whole province. | [optional] 
 **cityId** | **String** | Optional city filter. | [optional] 
 **cursor** | **String** | Opaque token returned as &#x60;nextCursor&#x60; by the previous page. | [optional] 
 **dutyNow** | **String** | Pass true to keep only facilities whose duty shift is running. | [optional] 
 **dutyToday** | **String** | Pass true to keep only facilities on today&#39;s duty roster. | [optional] 
 **latitude** | **String** | Caller latitude in WGS84 decimal degrees. Must be sent with longitude. | [optional] 
 **limit** | **Int** | Page size, maximum 100, default 30. | [optional] 
 **longitude** | **String** | Caller longitude in WGS84 decimal degrees. Must be sent with latitude. | [optional] 
 **neighborhoodId** | **String** | Optional neighbourhood filter. | [optional] 
 **openNow** | **String** | Pass true to keep only facilities open at this moment. | [optional] 
 **search** | **String** | Free-text term matched against facility text. | [optional] 
 **serviceId** | **Int** | The earlier name of serviceTagId, still accepted; it behaves the same way. | [optional] 
 **serviceTagId** | **Int** | Optional service filter, an id from publicCategoryTagsRetrieve. Only facilities whose category declares serviceFilter can match. Anything but a positive whole number is refused with 400. | [optional] 
 **sort** | **String** | nearest orders by distance and needs coordinates; name orders by Arabic name. Omitted keeps the historical behaviour: nearest whenever coordinates are supplied, name otherwise. Distances are returned whenever coordinates are supplied, whichever ordering is asked for. | [optional] 
 **specialtyId** | **Int** | Optional specialty filter, an id from publicCategoryTagsRetrieve. Only facilities whose category declares specialtyFilter can match. Anything but a positive whole number is refused with 400. | [optional] 

### Return type

[**FacilityCursorPage**](FacilityCursorPage.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **publicFacilityRetrieve**
```swift
    open class func publicFacilityRetrieve(facilityId: UUID, completion: @escaping (_ data: PublicFacilityDetail?, _ error: Error?) -> Void)
```

Retrieve one publicly visible facility

Returns the public projection only. Verification evidence, reviewer notes, memberships, internal policy fields and raw storage keys are never included.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let facilityId = 987 // UUID | 

// Retrieve one publicly visible facility
PublicDiscoveryAPI.publicFacilityRetrieve(facilityId: facilityId) { (response, error) in
    guard error == nil else {
        print(error)
        return
    }

    if (response) {
        dump(response)
    }
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **facilityId** | **UUID** |  | 

### Return type

[**PublicFacilityDetail**](PublicFacilityDetail.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **publicHomeRetrieve**
```swift
    open class func publicHomeRetrieve(provinceId: String, bbox: String? = nil, categoryId: String? = nil, cityId: String? = nil, latitude: String? = nil, longitude: String? = nil, neighborhoodId: String? = nil, search: String? = nil, serviceId: Int? = nil, serviceTagId: Int? = nil, specialtyId: Int? = nil, completion: @escaping (_ data: PublicHome?, _ error: Error?) -> Void)
```

Retrieve the home composition for a province

Bundles advertisements, the active category grid and three facility strips so the first screen needs one round trip. serverTime is authoritative.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let provinceId = "provinceId_example" // String | Province to scope the query to.
let bbox = "bbox_example" // String | Viewport as west,south,east,north in WGS84 decimal degrees. (optional)
let categoryId = "categoryId_example" // String | Optional category filter. (optional)
let cityId = "cityId_example" // String | Optional city filter. (optional)
let latitude = "latitude_example" // String | Caller latitude in WGS84 decimal degrees. Must be sent with longitude. (optional)
let longitude = "longitude_example" // String | Caller longitude in WGS84 decimal degrees. Must be sent with latitude. (optional)
let neighborhoodId = "neighborhoodId_example" // String | Optional neighbourhood filter. (optional)
let search = "search_example" // String | Free-text term matched against facility text. (optional)
let serviceId = 987 // Int | The earlier name of serviceTagId, still accepted; it behaves the same way. (optional)
let serviceTagId = 987 // Int | Optional service filter, an id from publicCategoryTagsRetrieve. Only facilities whose category declares serviceFilter can match. Anything but a positive whole number is refused with 400. (optional)
let specialtyId = 987 // Int | Optional specialty filter, an id from publicCategoryTagsRetrieve. Only facilities whose category declares specialtyFilter can match. Anything but a positive whole number is refused with 400. (optional)

// Retrieve the home composition for a province
PublicDiscoveryAPI.publicHomeRetrieve(provinceId: provinceId, bbox: bbox, categoryId: categoryId, cityId: cityId, latitude: latitude, longitude: longitude, neighborhoodId: neighborhoodId, search: search, serviceId: serviceId, serviceTagId: serviceTagId, specialtyId: specialtyId) { (response, error) in
    guard error == nil else {
        print(error)
        return
    }

    if (response) {
        dump(response)
    }
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **provinceId** | **String** | Province to scope the query to. | 
 **bbox** | **String** | Viewport as west,south,east,north in WGS84 decimal degrees. | [optional] 
 **categoryId** | **String** | Optional category filter. | [optional] 
 **cityId** | **String** | Optional city filter. | [optional] 
 **latitude** | **String** | Caller latitude in WGS84 decimal degrees. Must be sent with longitude. | [optional] 
 **longitude** | **String** | Caller longitude in WGS84 decimal degrees. Must be sent with latitude. | [optional] 
 **neighborhoodId** | **String** | Optional neighbourhood filter. | [optional] 
 **search** | **String** | Free-text term matched against facility text. | [optional] 
 **serviceId** | **Int** | The earlier name of serviceTagId, still accepted; it behaves the same way. | [optional] 
 **serviceTagId** | **Int** | Optional service filter, an id from publicCategoryTagsRetrieve. Only facilities whose category declares serviceFilter can match. Anything but a positive whole number is refused with 400. | [optional] 
 **specialtyId** | **Int** | Optional specialty filter, an id from publicCategoryTagsRetrieve. Only facilities whose category declares specialtyFilter can match. Anything but a positive whole number is refused with 400. | [optional] 

### Return type

[**PublicHome**](PublicHome.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **publicMapFacilitiesList**
```swift
    open class func publicMapFacilitiesList(provinceId: String, bbox: String? = nil, categoryId: String? = nil, cityId: String? = nil, dutyNow: String? = nil, dutyToday: String? = nil, latitude: String? = nil, longitude: String? = nil, neighborhoodId: String? = nil, openNow: String? = nil, search: String? = nil, serviceId: Int? = nil, serviceTagId: Int? = nil, specialtyId: Int? = nil, completion: @escaping (_ data: MapMarkerList?, _ error: Error?) -> Void)
```

List compact map markers inside a viewport

Capped at 500 markers. Facilities without coordinates are omitted. The filters behave exactly as they do on the list endpoint and combine the same way, so a map and a list asked the same question answer the same.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let provinceId = "provinceId_example" // String | Province to scope the query to.
let bbox = "bbox_example" // String | Viewport as west,south,east,north in WGS84 decimal degrees. (optional)
let categoryId = "categoryId_example" // String | Optional category filter. (optional)
let cityId = "cityId_example" // String | Optional city filter. (optional)
let dutyNow = "dutyNow_example" // String | Pass true to keep only facilities whose duty shift is running. (optional)
let dutyToday = "dutyToday_example" // String | Pass true to keep only facilities on today's duty roster. (optional)
let latitude = "latitude_example" // String | Caller latitude in WGS84 decimal degrees. Must be sent with longitude. (optional)
let longitude = "longitude_example" // String | Caller longitude in WGS84 decimal degrees. Must be sent with latitude. (optional)
let neighborhoodId = "neighborhoodId_example" // String | Optional neighbourhood filter. (optional)
let openNow = "openNow_example" // String | Pass true to keep only facilities open at this moment. (optional)
let search = "search_example" // String | Free-text term matched against facility text. (optional)
let serviceId = 987 // Int | The earlier name of serviceTagId, still accepted; it behaves the same way. (optional)
let serviceTagId = 987 // Int | Optional service filter, an id from publicCategoryTagsRetrieve. Only facilities whose category declares serviceFilter can match. Anything but a positive whole number is refused with 400. (optional)
let specialtyId = 987 // Int | Optional specialty filter, an id from publicCategoryTagsRetrieve. Only facilities whose category declares specialtyFilter can match. Anything but a positive whole number is refused with 400. (optional)

// List compact map markers inside a viewport
PublicDiscoveryAPI.publicMapFacilitiesList(provinceId: provinceId, bbox: bbox, categoryId: categoryId, cityId: cityId, dutyNow: dutyNow, dutyToday: dutyToday, latitude: latitude, longitude: longitude, neighborhoodId: neighborhoodId, openNow: openNow, search: search, serviceId: serviceId, serviceTagId: serviceTagId, specialtyId: specialtyId) { (response, error) in
    guard error == nil else {
        print(error)
        return
    }

    if (response) {
        dump(response)
    }
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **provinceId** | **String** | Province to scope the query to. | 
 **bbox** | **String** | Viewport as west,south,east,north in WGS84 decimal degrees. | [optional] 
 **categoryId** | **String** | Optional category filter. | [optional] 
 **cityId** | **String** | Optional city filter. | [optional] 
 **dutyNow** | **String** | Pass true to keep only facilities whose duty shift is running. | [optional] 
 **dutyToday** | **String** | Pass true to keep only facilities on today&#39;s duty roster. | [optional] 
 **latitude** | **String** | Caller latitude in WGS84 decimal degrees. Must be sent with longitude. | [optional] 
 **longitude** | **String** | Caller longitude in WGS84 decimal degrees. Must be sent with latitude. | [optional] 
 **neighborhoodId** | **String** | Optional neighbourhood filter. | [optional] 
 **openNow** | **String** | Pass true to keep only facilities open at this moment. | [optional] 
 **search** | **String** | Free-text term matched against facility text. | [optional] 
 **serviceId** | **Int** | The earlier name of serviceTagId, still accepted; it behaves the same way. | [optional] 
 **serviceTagId** | **Int** | Optional service filter, an id from publicCategoryTagsRetrieve. Only facilities whose category declares serviceFilter can match. Anything but a positive whole number is refused with 400. | [optional] 
 **specialtyId** | **Int** | Optional specialty filter, an id from publicCategoryTagsRetrieve. Only facilities whose category declares specialtyFilter can match. Anything but a positive whole number is refused with 400. | [optional] 

### Return type

[**MapMarkerList**](MapMarkerList.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **publicSearchList**
```swift
    open class func publicSearchList(provinceId: String, q: String, bbox: String? = nil, categoryId: String? = nil, cityId: String? = nil, cursor: String? = nil, latitude: String? = nil, limit: Int? = nil, longitude: String? = nil, neighborhoodId: String? = nil, search: String? = nil, serviceId: Int? = nil, serviceTagId: Int? = nil, specialtyId: Int? = nil, completion: @escaping (_ data: FacilityCursorPage?, _ error: Error?) -> Void)
```

Search facilities within a province

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let provinceId = "provinceId_example" // String | Province to scope the query to.
let q = "q_example" // String | Search term, at least two characters.
let bbox = "bbox_example" // String | Viewport as west,south,east,north in WGS84 decimal degrees. (optional)
let categoryId = "categoryId_example" // String | Optional category filter. (optional)
let cityId = "cityId_example" // String | Optional city filter. (optional)
let cursor = "cursor_example" // String | Opaque token returned as `nextCursor` by the previous page. (optional)
let latitude = "latitude_example" // String | Caller latitude in WGS84 decimal degrees. Must be sent with longitude. (optional)
let limit = 987 // Int | Page size, maximum 100, default 30. (optional)
let longitude = "longitude_example" // String | Caller longitude in WGS84 decimal degrees. Must be sent with latitude. (optional)
let neighborhoodId = "neighborhoodId_example" // String | Optional neighbourhood filter. (optional)
let search = "search_example" // String | Free-text term matched against facility text. (optional)
let serviceId = 987 // Int | The earlier name of serviceTagId, still accepted; it behaves the same way. (optional)
let serviceTagId = 987 // Int | Optional service filter, an id from publicCategoryTagsRetrieve. Only facilities whose category declares serviceFilter can match. Anything but a positive whole number is refused with 400. (optional)
let specialtyId = 987 // Int | Optional specialty filter, an id from publicCategoryTagsRetrieve. Only facilities whose category declares specialtyFilter can match. Anything but a positive whole number is refused with 400. (optional)

// Search facilities within a province
PublicDiscoveryAPI.publicSearchList(provinceId: provinceId, q: q, bbox: bbox, categoryId: categoryId, cityId: cityId, cursor: cursor, latitude: latitude, limit: limit, longitude: longitude, neighborhoodId: neighborhoodId, search: search, serviceId: serviceId, serviceTagId: serviceTagId, specialtyId: specialtyId) { (response, error) in
    guard error == nil else {
        print(error)
        return
    }

    if (response) {
        dump(response)
    }
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **provinceId** | **String** | Province to scope the query to. | 
 **q** | **String** | Search term, at least two characters. | 
 **bbox** | **String** | Viewport as west,south,east,north in WGS84 decimal degrees. | [optional] 
 **categoryId** | **String** | Optional category filter. | [optional] 
 **cityId** | **String** | Optional city filter. | [optional] 
 **cursor** | **String** | Opaque token returned as &#x60;nextCursor&#x60; by the previous page. | [optional] 
 **latitude** | **String** | Caller latitude in WGS84 decimal degrees. Must be sent with longitude. | [optional] 
 **limit** | **Int** | Page size, maximum 100, default 30. | [optional] 
 **longitude** | **String** | Caller longitude in WGS84 decimal degrees. Must be sent with latitude. | [optional] 
 **neighborhoodId** | **String** | Optional neighbourhood filter. | [optional] 
 **search** | **String** | Free-text term matched against facility text. | [optional] 
 **serviceId** | **Int** | The earlier name of serviceTagId, still accepted; it behaves the same way. | [optional] 
 **serviceTagId** | **Int** | Optional service filter, an id from publicCategoryTagsRetrieve. Only facilities whose category declares serviceFilter can match. Anything but a positive whole number is refused with 400. | [optional] 
 **specialtyId** | **Int** | Optional specialty filter, an id from publicCategoryTagsRetrieve. Only facilities whose category declares specialtyFilter can match. Anything but a positive whole number is refused with 400. | [optional] 

### Return type

[**FacilityCursorPage**](FacilityCursorPage.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

