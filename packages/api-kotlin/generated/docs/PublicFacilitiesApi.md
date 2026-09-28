# PublicFacilitiesApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
| ------------- | ------------- | ------------- |
| [**publicFacilityReportCreate**](PublicFacilitiesApi.md#publicFacilityReportCreate) | **POST** api/v1/facilities/{facility_id}/reports/ | Report a problem with a facility&#39;s listing |



Report a problem with a facility&#39;s listing

Anonymous callers are allowed; a signed-in caller is recorded as the reporter. Strictly throttled per account or IP. Only publicly visible facilities accept reports.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(PublicFacilitiesApi::class.java)
val facilityId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 
val facilityReportRequest : FacilityReportRequest =  // FacilityReportRequest | 

launch(Dispatchers.IO) {
    val result : FacilityReportCreated = webService.publicFacilityReportCreate(facilityId, facilityReportRequest)
}
```

### Parameters
| **facilityId** | **java.util.UUID**|  | |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **facilityReportRequest** | [**FacilityReportRequest**](FacilityReportRequest.md)|  | |

### Return type

[**FacilityReportCreated**](FacilityReportCreated.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

