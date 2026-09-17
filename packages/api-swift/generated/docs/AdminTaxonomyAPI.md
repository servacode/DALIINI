# AdminTaxonomyAPI

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**adminCategoriesList**](AdminTaxonomyAPI.md#admincategorieslist) | **GET** /api/v1/admin/categories/ | List categories
[**adminCategoryCapabilitiesReplace**](AdminTaxonomyAPI.md#admincategorycapabilitiesreplace) | **PUT** /api/v1/admin/categories/{category_id}/capabilities/ | Set the capability flags of a category
[**adminCategoryGroupsList**](AdminTaxonomyAPI.md#admincategorygroupslist) | **GET** /api/v1/admin/category-groups/ | List category groups
[**adminCategoryProvinceReplace**](AdminTaxonomyAPI.md#admincategoryprovincereplace) | **PUT** /api/v1/admin/categories/{category_id}/provinces/ | Set the per-province switches of a category


# **adminCategoriesList**
```swift
    open class func adminCategoriesList(completion: @escaping (_ data: AdminCategoryList?, _ error: Error?) -> Void)
```

List categories

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI


// List categories
AdminTaxonomyAPI.adminCategoriesList() { (response, error) in
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

[**AdminCategoryList**](AdminCategoryList.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminCategoryCapabilitiesReplace**
```swift
    open class func adminCategoryCapabilitiesReplace(categoryId: UUID, adminCapabilitiesRequest: AdminCapabilitiesRequest? = nil, completion: @escaping (_ data: AdminCapabilities?, _ error: Error?) -> Void)
```

Set the capability flags of a category

Duty can only be enabled for an approved specialization.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let categoryId = 987 // UUID | 
let adminCapabilitiesRequest = AdminCapabilitiesRequest(supportsHours: false, supportsPhotos: false, supportsRatings: false, supportsDuty: false, supportsSpecialtyFilter: false, supportsServiceFilter: false, supportsTemporaryClosure: false, supportsOwnerOnboarding: false) // AdminCapabilitiesRequest |  (optional)

// Set the capability flags of a category
AdminTaxonomyAPI.adminCategoryCapabilitiesReplace(categoryId: categoryId, adminCapabilitiesRequest: adminCapabilitiesRequest) { (response, error) in
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
 **categoryId** | **UUID** |  | 
 **adminCapabilitiesRequest** | [**AdminCapabilitiesRequest**](AdminCapabilitiesRequest.md) |  | [optional] 

### Return type

[**AdminCapabilities**](AdminCapabilities.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminCategoryGroupsList**
```swift
    open class func adminCategoryGroupsList(completion: @escaping (_ data: AdminCategoryGroupList?, _ error: Error?) -> Void)
```

List category groups

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI


// List category groups
AdminTaxonomyAPI.adminCategoryGroupsList() { (response, error) in
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

[**AdminCategoryGroupList**](AdminCategoryGroupList.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminCategoryProvinceReplace**
```swift
    open class func adminCategoryProvinceReplace(categoryId: UUID, adminCategoryProvinceRequest: AdminCategoryProvinceRequest, completion: @escaping (_ data: AdminId?, _ error: Error?) -> Void)
```

Set the per-province switches of a category

Public visibility and owner onboarding are independent switches.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let categoryId = 987 // UUID | 
let adminCategoryProvinceRequest = AdminCategoryProvinceRequest(provinceId: 123, publicEnabled: false, ownerRegistrationEnabled: false, sortOrder: 123) // AdminCategoryProvinceRequest | 

// Set the per-province switches of a category
AdminTaxonomyAPI.adminCategoryProvinceReplace(categoryId: categoryId, adminCategoryProvinceRequest: adminCategoryProvinceRequest) { (response, error) in
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
 **categoryId** | **UUID** |  | 
 **adminCategoryProvinceRequest** | [**AdminCategoryProvinceRequest**](AdminCategoryProvinceRequest.md) |  | 

### Return type

[**AdminId**](AdminId.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

