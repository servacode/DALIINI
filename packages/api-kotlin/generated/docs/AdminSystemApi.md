# AdminSystemApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
| ------------- | ------------- | ------------- |
| [**adminAlertsList**](AdminSystemApi.md#adminAlertsList) | **GET** api/v1/admin/alerts/ | Smart alerts: problems worth acting on now |
| [**adminDashboardRetrieve**](AdminSystemApi.md#adminDashboardRetrieve) | **GET** api/v1/admin/dashboard/ | Operational counters for the review desk |
| [**adminMeRetrieve**](AdminSystemApi.md#adminMeRetrieve) | **GET** api/v1/admin/me/ | The current operator and the permissions they hold |
| [**adminSearchRetrieve**](AdminSystemApi.md#adminSearchRetrieve) | **GET** api/v1/admin/search/ | Search facilities, users and applications at once |
| [**adminSystemStatusRetrieve**](AdminSystemApi.md#adminSystemStatusRetrieve) | **GET** api/v1/admin/system/status/ | Runtime and configuration status |
| [**adminTasksRetrieve**](AdminSystemApi.md#adminTasksRetrieve) | **GET** api/v1/admin/tasks/ | The operator&#39;s queue: what is waiting, oldest first |



Smart alerts: problems worth acting on now

DUTY_GAP: per province offering a duty category, the Damascus days of the next 14 with no duty shift of any ACTIVE pharmacy (critical when the first gap is today or tomorrow). STALE_FACILITY: ACTIVE facilities with no change, owner confirmation or approval for 90 days. REPORTED_FACILITY: 3 or more open reports (critical from 5). ZERO_RESULT_SEARCH: searches without results in the last 7 days, grouped by province and category because search text is never recorded. REVIEW_OVERDUE: submitted applications past the SLA (critical past twice it). MAINTENANCE_ON.

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
    val result : AdminAlertList = webService.adminAlertsList()
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**AdminAlertList**](AdminAlertList.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


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


The current operator and the permissions they hold

Drives navigation visibility and action gating in the Admin. A UI gate is not authorization: every endpoint re-checks, and a permission revoked mid-session surfaces as a 403 on the next call.

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
    val result : AdminMe = webService.adminMeRetrieve()
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**AdminMe**](AdminMe.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Search facilities, users and applications at once

Up to 5 hits per group. FACILITY (admin.facilities.read): Arabic or English name, or phone digits. USER (admin.users.read, or admin.facilities.read with the phone masked to its last 4 digits): name or phone digits. APPLICATION (admin.reviews.read): facility name. A group the caller may not read is left out, not returned empty.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminSystemApi::class.java)
val q : kotlin.String = q_example // kotlin.String | At least 2 chars.

launch(Dispatchers.IO) {
    val result : AdminSearchResult = webService.adminSearchRetrieve(q)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **q** | **kotlin.String**| At least 2 chars. | |

### Return type

[**AdminSearchResult**](AdminSearchResult.md)

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


The operator&#39;s queue: what is waiting, oldest first

Submitted applications split into INITIAL and REVERIFICATION, open problem reports grouped by facility (facilities with 2 or more open reports first) and facilities waiting in REVERIFICATION_REQUIRED. Each bucket has its count, how many are past the SLA (platform setting &#x60;review.slaHours&#x60;, default 48) and up to 10 oldest items with their age in hours and an &#x60;overdue&#x60; flag.

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
    val result : AdminTasks = webService.adminTasksRetrieve()
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**AdminTasks**](AdminTasks.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

