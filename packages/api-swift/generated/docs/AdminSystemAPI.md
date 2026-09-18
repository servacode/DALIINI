# AdminSystemAPI

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**adminDashboardRetrieve**](AdminSystemAPI.md#admindashboardretrieve) | **GET** /api/v1/admin/dashboard/ | Operational counters for the review desk
[**adminMeRetrieve**](AdminSystemAPI.md#adminmeretrieve) | **GET** /api/v1/admin/me/ | The current operator and the permissions they hold
[**adminSystemStatusRetrieve**](AdminSystemAPI.md#adminsystemstatusretrieve) | **GET** /api/v1/admin/system/status/ | Runtime and configuration status


# **adminDashboardRetrieve**
```swift
    open class func adminDashboardRetrieve(completion: @escaping (_ data: AdminDashboard?, _ error: Error?) -> Void)
```

Operational counters for the review desk

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI


// Operational counters for the review desk
AdminSystemAPI.adminDashboardRetrieve() { (response, error) in
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

[**AdminDashboard**](AdminDashboard.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminMeRetrieve**
```swift
    open class func adminMeRetrieve(completion: @escaping (_ data: AdminMe?, _ error: Error?) -> Void)
```

The current operator and the permissions they hold

Drives navigation visibility and action gating in the Admin. A UI gate is not authorization: every endpoint re-checks, and a permission revoked mid-session surfaces as a 403 on the next call.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI


// The current operator and the permissions they hold
AdminSystemAPI.adminMeRetrieve() { (response, error) in
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

[**AdminMe**](AdminMe.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminSystemStatusRetrieve**
```swift
    open class func adminSystemStatusRetrieve(completion: @escaping (_ data: AdminSystemStatus?, _ error: Error?) -> Void)
```

Runtime and configuration status

Reports only whether each dependency is configured. No secret, connection string or credential is returned.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI


// Runtime and configuration status
AdminSystemAPI.adminSystemStatusRetrieve() { (response, error) in
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

[**AdminSystemStatus**](AdminSystemStatus.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

