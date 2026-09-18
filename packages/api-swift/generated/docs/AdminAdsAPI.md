# AdminAdsAPI

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**adminAdCreate**](AdminAdsAPI.md#adminadcreate) | **POST** /api/v1/admin/ads/ | Create an advertisement
[**adminAdDelete**](AdminAdsAPI.md#adminaddelete) | **DELETE** /api/v1/admin/ads/{advertisement_id}/ | Delete an advertisement
[**adminAdUpdate**](AdminAdsAPI.md#adminadupdate) | **PUT** /api/v1/admin/ads/{advertisement_id}/ | Edit an advertisement, its schedule or its activation
[**adminAdsList**](AdminAdsAPI.md#adminadslist) | **GET** /api/v1/admin/ads/ | List advertisements


# **adminAdCreate**
```swift
    open class func adminAdCreate(adminAdvertisementRequest: AdminAdvertisementRequest, completion: @escaping (_ data: AdminId?, _ error: Error?) -> Void)
```

Create an advertisement

Requires the manage permission, which is re-checked inside the handler. Action payloads are validated per action type.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let adminAdvertisementRequest = AdminAdvertisementRequest(imageKey: "imageKey_example", titleAr: "titleAr_example", titleEn: "titleEn_example", subtitleAr: "subtitleAr_example", subtitleEn: "subtitleEn_example", actionType: AdvertisementActionTypeEnum(), actionPayload: "TODO", targetScope: AdvertisementTargetScopeEnum(), provinceId: 123, categoryId: 123, startsAt: Date(), endsAt: Date(), enabled: false, sortOrder: 123, slideDurationMs: 123) // AdminAdvertisementRequest | 

// Create an advertisement
AdminAdsAPI.adminAdCreate(adminAdvertisementRequest: adminAdvertisementRequest) { (response, error) in
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
 **adminAdvertisementRequest** | [**AdminAdvertisementRequest**](AdminAdvertisementRequest.md) |  | 

### Return type

[**AdminId**](AdminId.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminAdDelete**
```swift
    open class func adminAdDelete(advertisementId: UUID, completion: @escaping (_ data: Void?, _ error: Error?) -> Void)
```

Delete an advertisement

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let advertisementId = 987 // UUID | 

// Delete an advertisement
AdminAdsAPI.adminAdDelete(advertisementId: advertisementId) { (response, error) in
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
 **advertisementId** | **UUID** |  | 

### Return type

Void (empty response body)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminAdUpdate**
```swift
    open class func adminAdUpdate(advertisementId: UUID, adminAdvertisementUpdateRequest: AdminAdvertisementUpdateRequest? = nil, completion: @escaping (_ data: AdminId?, _ error: Error?) -> Void)
```

Edit an advertisement, its schedule or its activation

Omitted fields keep their current value. Schedule, targeting and action payload are validated together, so an end before its start or a global advertisement carrying a target is refused.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let advertisementId = 987 // UUID | 
let adminAdvertisementUpdateRequest = AdminAdvertisementUpdateRequest(imageKey: "imageKey_example", titleAr: "titleAr_example", titleEn: "titleEn_example", subtitleAr: "subtitleAr_example", subtitleEn: "subtitleEn_example", actionType: AdvertisementActionTypeEnum(), actionPayload: "TODO", targetScope: AdvertisementTargetScopeEnum(), provinceId: 123, categoryId: 123, startsAt: Date(), endsAt: Date(), enabled: false, sortOrder: 123, slideDurationMs: 123) // AdminAdvertisementUpdateRequest |  (optional)

// Edit an advertisement, its schedule or its activation
AdminAdsAPI.adminAdUpdate(advertisementId: advertisementId, adminAdvertisementUpdateRequest: adminAdvertisementUpdateRequest) { (response, error) in
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
 **advertisementId** | **UUID** |  | 
 **adminAdvertisementUpdateRequest** | [**AdminAdvertisementUpdateRequest**](AdminAdvertisementUpdateRequest.md) |  | [optional] 

### Return type

[**AdminId**](AdminId.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminAdsList**
```swift
    open class func adminAdsList(completion: @escaping (_ data: AdminAdvertisementList?, _ error: Error?) -> Void)
```

List advertisements

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI


// List advertisements
AdminAdsAPI.adminAdsList() { (response, error) in
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

[**AdminAdvertisementList**](AdminAdvertisementList.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

