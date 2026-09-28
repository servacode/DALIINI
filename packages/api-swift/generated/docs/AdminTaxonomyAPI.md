# AdminTaxonomyAPI

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**adminCategoriesList**](AdminTaxonomyAPI.md#admincategorieslist) | **GET** /api/v1/admin/categories/ | List categories
[**adminCategoryCapabilitiesReplace**](AdminTaxonomyAPI.md#admincategorycapabilitiesreplace) | **PUT** /api/v1/admin/categories/{category_id}/capabilities/ | Set the capability flags of a category
[**adminCategoryCreate**](AdminTaxonomyAPI.md#admincategorycreate) | **POST** /api/v1/admin/categories/create/ | Create a category
[**adminCategoryGroupCreate**](AdminTaxonomyAPI.md#admincategorygroupcreate) | **POST** /api/v1/admin/category-groups/create/ | Create a category group
[**adminCategoryGroupUpdate**](AdminTaxonomyAPI.md#admincategorygroupupdate) | **PUT** /api/v1/admin/category-groups/{group_id}/ | Rename, reorder or deactivate a category group
[**adminCategoryGroupsList**](AdminTaxonomyAPI.md#admincategorygroupslist) | **GET** /api/v1/admin/category-groups/ | List category groups
[**adminCategoryProvinceReplace**](AdminTaxonomyAPI.md#admincategoryprovincereplace) | **PUT** /api/v1/admin/categories/{category_id}/provinces/ | Set the per-province switches of a category
[**adminCategoryUpdate**](AdminTaxonomyAPI.md#admincategoryupdate) | **PUT** /api/v1/admin/categories/{category_id}/ | Rename, move, reorder or deactivate a category


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

# **adminCategoryCreate**
```swift
    open class func adminCategoryCreate(adminCategoryCreateRequest: AdminCategoryCreateRequest, completion: @escaping (_ data: AdminId?, _ error: Error?) -> Void)
```

Create a category

`code` and `slug` are fixed at creation and cannot be changed afterwards. A new category is invisible everywhere until its per-province switches are turned on, whatever `active` says.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let adminCategoryCreateRequest = AdminCategoryCreateRequest(groupId: 123, code: "code_example", slug: "slug_example", nameAr: "nameAr_example", nameEn: "nameEn_example", iconKey: "iconKey_example", specialization: CategorySpecializationEnum(), active: false, sortOrder: 123) // AdminCategoryCreateRequest | 

// Create a category
AdminTaxonomyAPI.adminCategoryCreate(adminCategoryCreateRequest: adminCategoryCreateRequest) { (response, error) in
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
 **adminCategoryCreateRequest** | [**AdminCategoryCreateRequest**](AdminCategoryCreateRequest.md) |  | 

### Return type

[**AdminId**](AdminId.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminCategoryGroupCreate**
```swift
    open class func adminCategoryGroupCreate(adminCategoryGroupRequest: AdminCategoryGroupRequest? = nil, completion: @escaping (_ data: AdminId?, _ error: Error?) -> Void)
```

Create a category group

Cycle J begins here: a group has to exist before a category can join it.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let adminCategoryGroupRequest = AdminCategoryGroupRequest(code: "code_example", nameAr: "nameAr_example", nameEn: "nameEn_example", iconKey: "iconKey_example", active: false, sortOrder: 123) // AdminCategoryGroupRequest |  (optional)

// Create a category group
AdminTaxonomyAPI.adminCategoryGroupCreate(adminCategoryGroupRequest: adminCategoryGroupRequest) { (response, error) in
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
 **adminCategoryGroupRequest** | [**AdminCategoryGroupRequest**](AdminCategoryGroupRequest.md) |  | [optional] 

### Return type

[**AdminId**](AdminId.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminCategoryGroupUpdate**
```swift
    open class func adminCategoryGroupUpdate(groupId: UUID, adminCategoryGroupRequest: AdminCategoryGroupRequest? = nil, completion: @escaping (_ data: AdminCategoryGroup?, _ error: Error?) -> Void)
```

Rename, reorder or deactivate a category group

The group code is immutable; sending a different one is refused.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let groupId = 987 // UUID | 
let adminCategoryGroupRequest = AdminCategoryGroupRequest(code: "code_example", nameAr: "nameAr_example", nameEn: "nameEn_example", iconKey: "iconKey_example", active: false, sortOrder: 123) // AdminCategoryGroupRequest |  (optional)

// Rename, reorder or deactivate a category group
AdminTaxonomyAPI.adminCategoryGroupUpdate(groupId: groupId, adminCategoryGroupRequest: adminCategoryGroupRequest) { (response, error) in
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
 **groupId** | **UUID** |  | 
 **adminCategoryGroupRequest** | [**AdminCategoryGroupRequest**](AdminCategoryGroupRequest.md) |  | [optional] 

### Return type

[**AdminCategoryGroup**](AdminCategoryGroup.md)

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

# **adminCategoryUpdate**
```swift
    open class func adminCategoryUpdate(categoryId: UUID, adminCategoryUpdateRequest: AdminCategoryUpdateRequest? = nil, completion: @escaping (_ data: AdminCategory?, _ error: Error?) -> Void)
```

Rename, move, reorder or deactivate a category

`code` and `slug` are immutable and are not accepted. Changing the specialization re-validates the capability set, so a category that carries duty cannot be moved off PHARMACY while it does.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let categoryId = 987 // UUID | 
let adminCategoryUpdateRequest = AdminCategoryUpdateRequest(groupId: 123, nameAr: "nameAr_example", nameEn: "nameEn_example", iconKey: "iconKey_example", specialization: CategorySpecializationEnum(), active: false, sortOrder: 123) // AdminCategoryUpdateRequest |  (optional)

// Rename, move, reorder or deactivate a category
AdminTaxonomyAPI.adminCategoryUpdate(categoryId: categoryId, adminCategoryUpdateRequest: adminCategoryUpdateRequest) { (response, error) in
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
 **adminCategoryUpdateRequest** | [**AdminCategoryUpdateRequest**](AdminCategoryUpdateRequest.md) |  | [optional] 

### Return type

[**AdminCategory**](AdminCategory.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

