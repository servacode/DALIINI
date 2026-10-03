# AdminAuditApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
| ------------- | ------------- | ------------- |
| [**adminAuditList**](AdminAuditApi.md#adminAuditList) | **GET** api/v1/admin/audit/ | Search the audit trail |



Search the audit trail

Newest first, in cursor pages. Snapshots and metadata are stored redacted. Every filter is optional and combines with the rest.

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
val cursor : kotlin.String = cursor_example // kotlin.String | Opaque token returned as `nextCursor` by the previous page.
val from : kotlin.String = from_example // kotlin.String | ISO date or datetime; keeps entries created at or after it.
val limit : kotlin.Int = 56 // kotlin.Int | Page size, maximum 200, default 50.
val requestId : kotlin.String = requestId_example // kotlin.String | Exact request correlation id, as returned in an error body.
val resource : kotlin.String = resource_example // kotlin.String | Substring matched against the target type, or an exact target id.
val to : kotlin.String = to_example // kotlin.String | ISO date or datetime; a bare date includes that whole day.

launch(Dispatchers.IO) {
    val result : AdminAuditList = webService.adminAuditList(action, actor, cursor, from, limit, requestId, resource, to)
}
```

### Parameters
| **action** | **kotlin.String**| Substring matched against the action code, case-insensitive. | [optional] |
| **actor** | **kotlin.String**| Actor user id. | [optional] |
| **cursor** | **kotlin.String**| Opaque token returned as &#x60;nextCursor&#x60; by the previous page. | [optional] |
| **from** | **kotlin.String**| ISO date or datetime; keeps entries created at or after it. | [optional] |
| **limit** | **kotlin.Int**| Page size, maximum 200, default 50. | [optional] |
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

