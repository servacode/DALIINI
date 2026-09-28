# AdminProvincesAPI

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**adminProvinceCitiesList**](AdminProvincesAPI.md#adminprovincecitieslist) | **GET** /api/v1/admin/provinces/{province_id}/cities/ | List every city of a province, active or not
[**adminProvinceCityUpdate**](AdminProvincesAPI.md#adminprovincecityupdate) | **PUT** /api/v1/admin/provinces/{province_id}/cities/{city_id}/ | Activate or deactivate a city
[**adminProvinceUpdate**](AdminProvincesAPI.md#adminprovinceupdate) | **PUT** /api/v1/admin/provinces/{province_id}/ | Activate a province or change its order
[**adminProvincesList**](AdminProvincesAPI.md#adminprovinceslist) | **GET** /api/v1/admin/provinces/ | List every province


# **adminProvinceCitiesList**
```swift
    open class func adminProvinceCitiesList(provinceId: UUID, completion: @escaping (_ data: AdminCityAdminList?, _ error: Error?) -> Void)
```

List every city of a province, active or not

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let provinceId = 987 // UUID | 

// List every city of a province, active or not
AdminProvincesAPI.adminProvinceCitiesList(provinceId: provinceId) { (response, error) in
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

[**AdminCityAdminList**](AdminCityAdminList.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminProvinceCityUpdate**
```swift
    open class func adminProvinceCityUpdate(cityId: UUID, provinceId: UUID, adminCityUpdateRequest: AdminCityUpdateRequest, completion: @escaping (_ data: AdminCityAdmin?, _ error: Error?) -> Void)
```

Activate or deactivate a city

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let cityId = 987 // UUID | 
let provinceId = 987 // UUID | 
let adminCityUpdateRequest = AdminCityUpdateRequest(active: false) // AdminCityUpdateRequest | 

// Activate or deactivate a city
AdminProvincesAPI.adminProvinceCityUpdate(cityId: cityId, provinceId: provinceId, adminCityUpdateRequest: adminCityUpdateRequest) { (response, error) in
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
 **cityId** | **UUID** |  | 
 **provinceId** | **UUID** |  | 
 **adminCityUpdateRequest** | [**AdminCityUpdateRequest**](AdminCityUpdateRequest.md) |  | 

### Return type

[**AdminCityAdmin**](AdminCityAdmin.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminProvinceUpdate**
```swift
    open class func adminProvinceUpdate(provinceId: UUID, adminProvinceUpdateRequest: AdminProvinceUpdateRequest? = nil, completion: @escaping (_ data: AdminProvinceUpdated?, _ error: Error?) -> Void)
```

Activate a province or change its order

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let provinceId = 987 // UUID | 
let adminProvinceUpdateRequest = AdminProvinceUpdateRequest(active: false, sortOrder: 123) // AdminProvinceUpdateRequest |  (optional)

// Activate a province or change its order
AdminProvincesAPI.adminProvinceUpdate(provinceId: provinceId, adminProvinceUpdateRequest: adminProvinceUpdateRequest) { (response, error) in
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
 **adminProvinceUpdateRequest** | [**AdminProvinceUpdateRequest**](AdminProvinceUpdateRequest.md) |  | [optional] 

### Return type

[**AdminProvinceUpdated**](AdminProvinceUpdated.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminProvincesList**
```swift
    open class func adminProvincesList(completion: @escaping (_ data: AdminProvinceList?, _ error: Error?) -> Void)
```

List every province

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI


// List every province
AdminProvincesAPI.adminProvincesList() { (response, error) in
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

[**AdminProvinceList**](AdminProvinceList.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

