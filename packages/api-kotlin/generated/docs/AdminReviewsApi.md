# AdminReviewsApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
| ------------- | ------------- | ------------- |
| [**adminEvidenceContentRetrieve**](AdminReviewsApi.md#adminEvidenceContentRetrieve) | **GET** api/v1/admin/evidence/{evidence_id}/content/ | Stream one piece of private verification evidence |
| [**adminReviewApprove**](AdminReviewsApi.md#adminReviewApprove) | **POST** api/v1/admin/applications/{application_id}/approve/ | Approve an application |
| [**adminReviewReject**](AdminReviewsApi.md#adminReviewReject) | **POST** api/v1/admin/applications/{application_id}/reject/ | Reject an application |
| [**adminReviewRetrieve**](AdminReviewsApi.md#adminReviewRetrieve) | **GET** api/v1/admin/applications/{application_id}/ | Retrieve one application with its review context |
| [**adminReviewsList**](AdminReviewsApi.md#adminReviewsList) | **GET** api/v1/admin/applications/ | List facility applications awaiting or past review |



Stream one piece of private verification evidence

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminReviewsApi::class.java)
val evidenceId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 

launch(Dispatchers.IO) {
    val result : java.io.File = webService.adminEvidenceContentRetrieve(evidenceId)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **evidenceId** | **java.util.UUID**|  | |

### Return type

[**java.io.File**](java.io.File.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Approve an application

Runs in one transaction: the application and the facility lifecycle are locked, the current requirements are re-checked, the change is audited and the realtime event is emitted only after commit.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminReviewsApi::class.java)
val applicationId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 
val adminDecisionRequest : AdminDecisionRequest =  // AdminDecisionRequest | 

launch(Dispatchers.IO) {
    val result : AdminApplication = webService.adminReviewApprove(applicationId, adminDecisionRequest)
}
```

### Parameters
| **applicationId** | **java.util.UUID**|  | |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **adminDecisionRequest** | [**AdminDecisionRequest**](AdminDecisionRequest.md)|  | [optional] |

### Return type

[**AdminApplication**](AdminApplication.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


Reject an application

A reason is recorded in the audit trail; nothing is silently deleted.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminReviewsApi::class.java)
val applicationId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 
val adminDecisionRequest : AdminDecisionRequest =  // AdminDecisionRequest | 

launch(Dispatchers.IO) {
    val result : AdminApplication = webService.adminReviewReject(applicationId, adminDecisionRequest)
}
```

### Parameters
| **applicationId** | **java.util.UUID**|  | |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **adminDecisionRequest** | [**AdminDecisionRequest**](AdminDecisionRequest.md)|  | [optional] |

### Return type

[**AdminApplication**](AdminApplication.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


Retrieve one application with its review context

Evidence is referenced by identifier only; content is fetched separately through the audited evidence endpoint.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminReviewsApi::class.java)
val applicationId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 

launch(Dispatchers.IO) {
    val result : AdminApplicationDetail = webService.adminReviewRetrieve(applicationId)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **applicationId** | **java.util.UUID**|  | |

### Return type

[**AdminApplicationDetail**](AdminApplicationDetail.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


List facility applications awaiting or past review

Capped at 200 rows. Every filter is optional and combines with the rest.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminReviewsApi::class.java)
val category : kotlin.String = category_example // kotlin.String | Category id of the facility the application belongs to.
val kind : kotlin.String = kind_example // kotlin.String | Application kind, for example REGISTRATION or REVERIFICATION.
val province : kotlin.String = province_example // kotlin.String | Province id of the facility the application belongs to.
val status : kotlin.String = status_example // kotlin.String | Application status, for example SUBMITTED or APPROVED.

launch(Dispatchers.IO) {
    val result : AdminApplicationList = webService.adminReviewsList(category, kind, province, status)
}
```

### Parameters
| **category** | **kotlin.String**| Category id of the facility the application belongs to. | [optional] |
| **kind** | **kotlin.String**| Application kind, for example REGISTRATION or REVERIFICATION. | [optional] |
| **province** | **kotlin.String**| Province id of the facility the application belongs to. | [optional] |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **status** | **kotlin.String**| Application status, for example SUBMITTED or APPROVED. | [optional] |

### Return type

[**AdminApplicationList**](AdminApplicationList.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

