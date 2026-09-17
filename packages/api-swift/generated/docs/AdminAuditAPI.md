# AdminAuditAPI

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**adminAuditList**](AdminAuditAPI.md#adminauditlist) | **GET** /api/v1/admin/audit/ | Search the audit trail


# **adminAuditList**
```swift
    open class func adminAuditList(completion: @escaping (_ data: AdminAuditList?, _ error: Error?) -> Void)
```

Search the audit trail

Capped at 250 rows. Snapshots and metadata are stored redacted.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI


// Search the audit trail
AdminAuditAPI.adminAuditList() { (response, error) in
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

[**AdminAuditList**](AdminAuditList.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

