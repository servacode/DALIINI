# AdminAuditApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
| ------------- | ------------- | ------------- |
| [**adminAuditList**](AdminAuditApi.md#adminAuditList) | **GET** api/v1/admin/audit/ | Search the audit trail |



Search the audit trail

Capped at 250 rows. Snapshots and metadata are stored redacted. Every filter is optional and combines with the rest.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminAuditApi::class.java)
val action : kotlin.String = action_example // kotlin.String | Substring matched against the action code, case-insensitive.
val actor : kotlin.String = actor_example // kotlin.String | Actor user id.
val from : kotlin.String = from_example // kotlin.String | ISO date or datetime; keeps entries created at or after it.
val requestId : kotlin.String = requestId_example // kotlin.String | Exact request correlation id, as returned in an error body.
val resource : kotlin.String = resource_example // kotlin.String | Substring matched against the target type, or an exact target id.
val to : kotlin.String = to_example // kotlin.String | ISO date or datetime; a bare date includes that whole day.

launch(Dispatchers.IO) {
    val result : AdminAuditList = webService.adminAuditList(action, actor, from, requestId, resource, to)
}
```

### Parameters
| **action** | **kotlin.String**| Substring matched against the action code, case-insensitive. | [optional] |
| **actor** | **kotlin.String**| Actor user id. | [optional] |
| **from** | **kotlin.String**| ISO date or datetime; keeps entries created at or after it. | [optional] |
| **requestId** | **kotlin.String**| Exact request correlation id, as returned in an error body. | [optional] |
| **resource** | **kotlin.String**| Substring matched against the target type, or an exact target id. | [optional] |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **to** | **kotlin.String**| ISO date or datetime; a bare date includes that whole day. | [optional] |

### Return type

[**AdminAuditList**](AdminAuditList.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

