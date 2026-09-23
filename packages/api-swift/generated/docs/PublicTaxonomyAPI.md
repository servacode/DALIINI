# PublicTaxonomyAPI

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**publicLocationResolve**](PublicTaxonomyAPI.md#publiclocationresolve) | **GET** /api/v1/public/locations/resolve/ | Resolve a coordinate to a province, city and neighbourhood
[**publicProvinceCategoriesList**](PublicTaxonomyAPI.md#publicprovincecategorieslist) | **GET** /api/v1/public/provinces/{province_id}/categories/ | List categories publicly enabled for a province
[**publicProvinceCitiesList**](PublicTaxonomyAPI.md#publicprovincecitieslist) | **GET** /api/v1/public/provinces/{province_id}/cities/ | List active cities in a province
[**publicProvincesList**](PublicTaxonomyAPI.md#publicprovinceslist) | **GET** /api/v1/public/provinces/ | List active provinces


# **publicLocationResolve**
```swift
    open class func publicLocationResolve(latitude: Double, longitude: Double, completion: @escaping (_ data: PublicLocationResolve?, _ error: Error?) -> Void)
```

Resolve a coordinate to a province, city and neighbourhood

Point-in-polygon against the seeded city and neighbourhood boundaries, then the nearest active province centre within 200 km. No external geocoder is called and the coordinate is not stored.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let latitude = 987 // Double | 
let longitude = 987 // Double | 

// Resolve a coordinate to a province, city and neighbourhood
PublicTaxonomyAPI.publicLocationResolve(latitude: latitude, longitude: longitude) { (response, error) in
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
 **latitude** | **Double** |  | 
 **longitude** | **Double** |  | 

### Return type

[**PublicLocationResolve**](PublicLocationResolve.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **publicProvinceCategoriesList**
```swift
    open class func publicProvinceCategoriesList(provinceId: UUID, completion: @escaping (_ data: PublicCategoryList?, _ error: Error?) -> Void)
```

List categories publicly enabled for a province

A category is listed only when the province is active, the group and the category are active, and the per-province public switch is on. Clients drive their UI from the returned capability flags, never from the category name.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let provinceId = 987 // UUID | 

// List categories publicly enabled for a province
PublicTaxonomyAPI.publicProvinceCategoriesList(provinceId: provinceId) { (response, error) in
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
 **provinceId** | **UUID** |  | 

### Return type

[**PublicCategoryList**](PublicCategoryList.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **publicProvinceCitiesList**
```swift
    open class func publicProvinceCitiesList(provinceId: UUID, completion: @escaping (_ data: PublicCityList?, _ error: Error?) -> Void)
```

List active cities in a province

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let provinceId = 987 // UUID | 

// List active cities in a province
PublicTaxonomyAPI.publicProvinceCitiesList(provinceId: provinceId) { (response, error) in
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
 **provinceId** | **UUID** |  | 

### Return type

[**PublicCityList**](PublicCityList.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **publicProvincesList**
```swift
    open class func publicProvincesList(completion: @escaping (_ data: PublicProvinceList?, _ error: Error?) -> Void)
```

List active provinces

Every province is seeded, but only active ones are publicly visible. Ordered by sort order then Arabic name.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI


// List active provinces
PublicTaxonomyAPI.publicProvincesList() { (response, error) in
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
This endpoint does not need any parameter.

### Return type

[**PublicProvinceList**](PublicProvinceList.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

