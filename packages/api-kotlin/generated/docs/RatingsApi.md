# RatingsApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
| ------------- | ------------- | ------------- |
| [**facilityRatingDelete**](RatingsApi.md#facilityRatingDelete) | **DELETE** api/v1/facilities/{facility_id}/rating/ | Remove the caller&#39;s rating for a facility |
| [**facilityRatingUpsert**](RatingsApi.md#facilityRatingUpsert) | **PUT** api/v1/facilities/{facility_id}/rating/ | Create or replace the caller&#39;s rating for a facility |



Remove the caller&#39;s rating for a facility

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(RatingsApi::class.java)
val facilityId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 

launch(Dispatchers.IO) {
    webService.facilityRatingDelete(facilityId)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **facilityId** | **java.util.UUID**|  | |

### Return type

null (empty response body)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Create or replace the caller&#39;s rating for a facility

One rating per user per facility, so repeating the call replaces the previous value. Only categories that declare the ratings capability accept this.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(RatingsApi::class.java)
val facilityId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 
val ratingWrite : RatingWrite =  // RatingWrite | 

launch(Dispatchers.IO) {
    val result : FacilityRating = webService.facilityRatingUpsert(facilityId, ratingWrite)
}
```

### Parameters
| **facilityId** | **java.util.UUID**|  | |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **ratingWrite** | [**RatingWrite**](RatingWrite.md)|  | |

### Return type

[**FacilityRating**](FacilityRating.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

