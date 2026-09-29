# AdminAuditAPI

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**adminAuditList**](AdminAuditAPI.md#adminauditlist) | **GET** /api/v1/admin/audit/ | Search the audit trail


# **adminAuditList**
```swift
    open class func adminAuditList(action: String? = nil, actor: String? = nil, from: String? = nil, requestId: String? = nil, resource: String? = nil, to: String? = nil, completion: @escaping (_ data: AdminAuditList?, _ error: Error?) -> Void)
```

Search the audit trail

Capped at 250 rows. Snapshots and metadata are stored redacted. Every filter is optional and combines with the rest.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let action = "action_example" // String | Substring matched against the action code, case-insensitive. (optional)
let actor = "actor_example" // String | Actor user id. (optional)
let from = "from_example" // String | ISO date or datetime; keeps entries created at or after it. (optional)
let requestId = "requestId_example" // String | Exact request correlation id, as returned in an error body. (optional)
let resource = "resource_example" // String | Substring matched against the target type, or an exact target id. (optional)
let to = "to_example" // String | ISO date or datetime; a bare date includes that whole day. (optional)

// Search the audit trail
AdminAuditAPI.adminAuditList(action: action, actor: actor, from: from, requestId: requestId, resource: resource, to: to) { (response, error) in
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
 **action** | **String** | Substring matched against the action code, case-insensitive. | [optional] 
 **actor** | **String** | Actor user id. | [optional] 
 **from** | **String** | ISO date or datetime; keeps entries created at or after it. | [optional] 
 **requestId** | **String** | Exact request correlation id, as returned in an error body. | [optional] 
 **resource** | **String** | Substring matched against the target type, or an exact target id. | [optional] 
 **to** | **String** | ISO date or datetime; a bare date includes that whole day. | [optional] 

### Return type

[**AdminAuditList**](AdminAuditList.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

