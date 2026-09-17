# AdminSystemApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
| ------------- | ------------- | ------------- |
| [**adminDashboardRetrieve**](AdminSystemApi.md#adminDashboardRetrieve) | **GET** api/v1/admin/dashboard/ | Operational counters for the review desk |
| [**adminSystemStatusRetrieve**](AdminSystemApi.md#adminSystemStatusRetrieve) | **GET** api/v1/admin/system/status/ | Runtime and configuration status |



Operational counters for the review desk

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminSystemApi::class.java)

launch(Dispatchers.IO) {
    val result : AdminDashboard = webService.adminDashboardRetrieve()
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**AdminDashboard**](AdminDashboard.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Runtime and configuration status

Reports only whether each dependency is configured. No secret, connection string or credential is returned.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminSystemApi::class.java)

launch(Dispatchers.IO) {
    val result : AdminSystemStatus = webService.adminSystemStatusRetrieve()
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**AdminSystemStatus**](AdminSystemStatus.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

