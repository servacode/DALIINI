# PublicTaxonomyAPI

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**publicProvinceCategoriesList**](PublicTaxonomyAPI.md#publicprovincecategorieslist) | **GET** /api/v1/public/provinces/{province_id}/categories/ | List categories publicly enabled for a province
[**publicProvinceCitiesList**](PublicTaxonomyAPI.md#publicprovincecitieslist) | **GET** /api/v1/public/provinces/{province_id}/cities/ | List active cities in a province
[**publicProvincesList**](PublicTaxonomyAPI.md#publicprovinceslist) | **GET** /api/v1/public/provinces/ | List active provinces


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

