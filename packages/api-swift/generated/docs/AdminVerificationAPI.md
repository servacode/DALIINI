# AdminVerificationAPI

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**adminVerificationRequirementCreate**](AdminVerificationAPI.md#adminverificationrequirementcreate) | **POST** /api/v1/admin/verification-requirements/ | Create a verification requirement
[**adminVerificationRequirementsList**](AdminVerificationAPI.md#adminverificationrequirementslist) | **GET** /api/v1/admin/verification-requirements/ | List verification requirements


# **adminVerificationRequirementCreate**
```swift
    open class func adminVerificationRequirementCreate(adminVerificationRequirementRequest: AdminVerificationRequirementRequest, completion: @escaping (_ data: AdminId?, _ error: Error?) -> Void)
```

Create a verification requirement

Requires the manage permission, which is re-checked inside the handler.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let adminVerificationRequirementRequest = AdminVerificationRequirementRequest(categoryId: 123, labelAr: "labelAr_example", labelEn: "labelEn_example", instructionsAr: "instructionsAr_example", instructionsEn: "instructionsEn_example", _required: false, active: false, minFiles: 123, maxFiles: 123, sortOrder: 123) // AdminVerificationRequirementRequest | 

// Create a verification requirement
AdminVerificationAPI.adminVerificationRequirementCreate(adminVerificationRequirementRequest: adminVerificationRequirementRequest) { (response, error) in
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
 **adminVerificationRequirementRequest** | [**AdminVerificationRequirementRequest**](AdminVerificationRequirementRequest.md) |  | 

### Return type

[**AdminId**](AdminId.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminVerificationRequirementsList**
```swift
    open class func adminVerificationRequirementsList(completion: @escaping (_ data: AdminVerificationRequirementList?, _ error: Error?) -> Void)
```

List verification requirements

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI


// List verification requirements
AdminVerificationAPI.adminVerificationRequirementsList() { (response, error) in
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

[**AdminVerificationRequirementList**](AdminVerificationRequirementList.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

