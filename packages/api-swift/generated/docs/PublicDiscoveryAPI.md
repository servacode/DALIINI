# PublicDiscoveryAPI

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**publicFacilitiesList**](PublicDiscoveryAPI.md#publicfacilitieslist) | **GET** /api/v1/public/facilities/ | List publicly visible facilities in a province, optionally in one category
[**publicFacilityRetrieve**](PublicDiscoveryAPI.md#publicfacilityretrieve) | **GET** /api/v1/public/facilities/{facility_id}/ | Retrieve one publicly visible facility
[**publicHomeRetrieve**](PublicDiscoveryAPI.md#publichomeretrieve) | **GET** /api/v1/public/home/ | Retrieve the home composition for a province
[**publicMapFacilitiesList**](PublicDiscoveryAPI.md#publicmapfacilitieslist) | **GET** /api/v1/public/map/facilities/ | List compact map markers inside a viewport
[**publicSearchList**](PublicDiscoveryAPI.md#publicsearchlist) | **GET** /api/v1/public/search/ | Search facilities within a province


# **publicFacilitiesList**
```swift
    open class func publicFacilitiesList(provinceId: String, bbox: String? = nil, categoryId: String? = nil, cityId: String? = nil, cursor: String? = nil, dutyNow: String? = nil, latitude: String? = nil, limit: Int? = nil, longitude: String? = nil, neighborhoodId: String? = nil, openNow: String? = nil, search: String? = nil, serviceId: String? = nil, specialtyId: String? = nil, completion: @escaping (_ data: FacilityCursorPage?, _ error: Error?) -> Void)
```

List publicly visible facilities in a province, optionally in one category

Ordered nearest-first when coordinates are supplied, otherwise by Arabic name. Availability is computed by the backend; openNow and dutyNow filter on that computed state rather than on a stored flag.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let provinceId = "provinceId_example" // String | Province to scope the query to.
let bbox = "bbox_example" // String | Viewport as west,south,east,north in WGS84 decimal degrees. (optional)
let categoryId = "categoryId_example" // String | Optional category to list. Absent means the whole province. (optional)
let cityId = "cityId_example" // String | Optional city filter. (optional)
let cursor = "cursor_example" // String | Opaque token returned as `nextCursor` by the previous page. (optional)
let dutyNow = "dutyNow_example" // String | Pass true to keep only facilities currently on duty. (optional)
let latitude = "latitude_example" // String | Caller latitude in WGS84 decimal degrees. Must be sent with longitude. (optional)
let limit = 987 // Int | Page size, maximum 100, default 30. (optional)
let longitude = "longitude_example" // String | Caller longitude in WGS84 decimal degrees. Must be sent with latitude. (optional)
let neighborhoodId = "neighborhoodId_example" // String | Optional neighbourhood filter. (optional)
let openNow = "openNow_example" // String | Pass true to keep only facilities currently open. (optional)
let search = "search_example" // String | Free-text term matched against facility text. (optional)
let serviceId = "serviceId_example" // String | Optional service-tag filter; only meaningful when the category declares serviceFilter. (optional)
let specialtyId = "specialtyId_example" // String | Optional specialty filter; only meaningful when the category declares specialtyFilter. (optional)

// List publicly visible facilities in a province, optionally in one category
PublicDiscoveryAPI.publicFacilitiesList(provinceId: provinceId, bbox: bbox, categoryId: categoryId, cityId: cityId, cursor: cursor, dutyNow: dutyNow, latitude: latitude, limit: limit, longitude: longitude, neighborhoodId: neighborhoodId, openNow: openNow, search: search, serviceId: serviceId, specialtyId: specialtyId) { (response, error) in
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
 **dutyNow** | **String** | Pass true to keep only facilities currently on duty. | [optional] 
 **latitude** | **String** | Caller latitude in WGS84 decimal degrees. Must be sent with longitude. | [optional] 
 **limit** | **Int** | Page size, maximum 100, default 30. | [optional] 
 **longitude** | **String** | Caller longitude in WGS84 decimal degrees. Must be sent with latitude. | [optional] 
 **neighborhoodId** | **String** | Optional neighbourhood filter. | [optional] 
 **openNow** | **String** | Pass true to keep only facilities currently open. | [optional] 
 **search** | **String** | Free-text term matched against facility text. | [optional] 
 **serviceId** | **String** | Optional service-tag filter; only meaningful when the category declares serviceFilter. | [optional] 
 **specialtyId** | **String** | Optional specialty filter; only meaningful when the category declares specialtyFilter. | [optional] 

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
    open class func publicHomeRetrieve(provinceId: String, bbox: String? = nil, categoryId: String? = nil, cityId: String? = nil, latitude: String? = nil, longitude: String? = nil, neighborhoodId: String? = nil, search: String? = nil, serviceId: String? = nil, specialtyId: String? = nil, completion: @escaping (_ data: PublicHome?, _ error: Error?) -> Void)
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
let serviceId = "serviceId_example" // String | Optional service-tag filter; only meaningful when the category declares serviceFilter. (optional)
let specialtyId = "specialtyId_example" // String | Optional specialty filter; only meaningful when the category declares specialtyFilter. (optional)

// Retrieve the home composition for a province
PublicDiscoveryAPI.publicHomeRetrieve(provinceId: provinceId, bbox: bbox, categoryId: categoryId, cityId: cityId, latitude: latitude, longitude: longitude, neighborhoodId: neighborhoodId, search: search, serviceId: serviceId, specialtyId: specialtyId) { (response, error) in
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
 **serviceId** | **String** | Optional service-tag filter; only meaningful when the category declares serviceFilter. | [optional] 
 **specialtyId** | **String** | Optional specialty filter; only meaningful when the category declares specialtyFilter. | [optional] 

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
    open class func publicMapFacilitiesList(provinceId: String, bbox: String? = nil, categoryId: String? = nil, cityId: String? = nil, latitude: String? = nil, longitude: String? = nil, neighborhoodId: String? = nil, search: String? = nil, serviceId: String? = nil, specialtyId: String? = nil, completion: @escaping (_ data: MapMarkerList?, _ error: Error?) -> Void)
```

List compact map markers inside a viewport

Capped at 500 markers. Facilities without coordinates are omitted.

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
let serviceId = "serviceId_example" // String | Optional service-tag filter; only meaningful when the category declares serviceFilter. (optional)
let specialtyId = "specialtyId_example" // String | Optional specialty filter; only meaningful when the category declares specialtyFilter. (optional)

// List compact map markers inside a viewport
PublicDiscoveryAPI.publicMapFacilitiesList(provinceId: provinceId, bbox: bbox, categoryId: categoryId, cityId: cityId, latitude: latitude, longitude: longitude, neighborhoodId: neighborhoodId, search: search, serviceId: serviceId, specialtyId: specialtyId) { (response, error) in
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
 **serviceId** | **String** | Optional service-tag filter; only meaningful when the category declares serviceFilter. | [optional] 
 **specialtyId** | **String** | Optional specialty filter; only meaningful when the category declares specialtyFilter. | [optional] 

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
    open class func publicSearchList(provinceId: String, q: String, bbox: String? = nil, categoryId: String? = nil, cityId: String? = nil, cursor: String? = nil, latitude: String? = nil, limit: Int? = nil, longitude: String? = nil, neighborhoodId: String? = nil, search: String? = nil, serviceId: String? = nil, specialtyId: String? = nil, completion: @escaping (_ data: FacilityCursorPage?, _ error: Error?) -> Void)
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
let serviceId = "serviceId_example" // String | Optional service-tag filter; only meaningful when the category declares serviceFilter. (optional)
let specialtyId = "specialtyId_example" // String | Optional specialty filter; only meaningful when the category declares specialtyFilter. (optional)

// Search facilities within a province
PublicDiscoveryAPI.publicSearchList(provinceId: provinceId, q: q, bbox: bbox, categoryId: categoryId, cityId: cityId, cursor: cursor, latitude: latitude, limit: limit, longitude: longitude, neighborhoodId: neighborhoodId, search: search, serviceId: serviceId, specialtyId: specialtyId) { (response, error) in
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
 **serviceId** | **String** | Optional service-tag filter; only meaningful when the category declares serviceFilter. | [optional] 
 **specialtyId** | **String** | Optional specialty filter; only meaningful when the category declares specialtyFilter. | [optional] 

### Return type

[**FacilityCursorPage**](FacilityCursorPage.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

