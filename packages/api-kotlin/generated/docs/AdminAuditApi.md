# AdminAuditApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
| ------------- | ------------- | ------------- |
| [**adminAuditList**](AdminAuditApi.md#adminAuditList) | **GET** api/v1/admin/audit/ | Search the audit trail |



Search the audit trail

Capped at 250 rows. Snapshots and metadata are stored redacted.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminAuditApi::class.java)

launch(Dispatchers.IO) {
    val result : AdminAuditList = webService.adminAuditList()
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**AdminAuditList**](AdminAuditList.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

