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
val requestId : kotlin.String = requestId_example // kotlin.String | Exact request correlation id, as returned in an error body.
val resource : kotlin.String = resource_example // kotlin.String | Substring matched against the target type, or an exact target id.

launch(Dispatchers.IO) {
    val result : AdminAuditList = webService.adminAuditList(action, actor, requestId, resource)
}
```

### Parameters
| **action** | **kotlin.String**| Substring matched against the action code, case-insensitive. | [optional] |
| **actor** | **kotlin.String**| Actor user id. | [optional] |
| **requestId** | **kotlin.String**| Exact request correlation id, as returned in an error body. | [optional] |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **resource** | **kotlin.String**| Substring matched against the target type, or an exact target id. | [optional] |

### Return type

[**AdminAuditList**](AdminAuditList.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

