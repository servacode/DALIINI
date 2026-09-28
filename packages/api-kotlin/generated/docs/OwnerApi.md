# OwnerApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
| ------------- | ------------- | ------------- |
| [**ownerConfigRetrieve**](OwnerApi.md#ownerConfigRetrieve) | **GET** api/v1/owner/config/ | List categories open for owner onboarding in a province |
| [**ownerFacilitiesList**](OwnerApi.md#ownerFacilitiesList) | **GET** api/v1/owner/facilities/ | List the facilities the caller belongs to |
| [**ownerFacilityCreate**](OwnerApi.md#ownerFacilityCreate) | **POST** api/v1/owner/facilities/ | Create a facility draft |
| [**ownerFacilityHoursConfirm**](OwnerApi.md#ownerFacilityHoursConfirm) | **POST** api/v1/owner/facilities/{facility_id}/confirm-hours/ | Confirm that the facility&#39;s opening hours are still right |
| [**ownerFacilityInsightsRetrieve**](OwnerApi.md#ownerFacilityInsightsRetrieve) | **GET** api/v1/owner/facilities/{facility_id}/insights/ | Engagement with a facility over the last 30 days |
| [**ownerFacilityLocationReplace**](OwnerApi.md#ownerFacilityLocationReplace) | **PUT** api/v1/owner/facilities/{facility_id}/location/ | Set the map point of a facility |
| [**ownerFacilityMemberDelete**](OwnerApi.md#ownerFacilityMemberDelete) | **DELETE** api/v1/owner/facilities/{facility_id}/members/{user_id}/ | Remove a member from a facility |
| [**ownerFacilityMemberUpsert**](OwnerApi.md#ownerFacilityMemberUpsert) | **POST** api/v1/owner/facilities/{facility_id}/members/ | Add a member or change a member role |
| [**ownerFacilityMembersList**](OwnerApi.md#ownerFacilityMembersList) | **GET** api/v1/owner/facilities/{facility_id}/members/ | List the members of a facility |
| [**ownerFacilityRetrieve**](OwnerApi.md#ownerFacilityRetrieve) | **GET** api/v1/owner/facilities/{facility_id}/ | Retrieve one facility the caller belongs to |
| [**ownerFacilitySubmit**](OwnerApi.md#ownerFacilitySubmit) | **POST** api/v1/owner/facilities/{facility_id}/submit/ | Submit a facility for review |
| [**ownerFacilityUpdate**](OwnerApi.md#ownerFacilityUpdate) | **PATCH** api/v1/owner/facilities/{facility_id}/ | Update the core fields of a facility |



List categories open for owner onboarding in a province

Returns only categories whose per-province owner switch is on and whose capability set allows onboarding, together with the safe descriptors of the verification requirements the owner will have to satisfy.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(OwnerApi::class.java)
val provinceId : kotlin.String = provinceId_example // kotlin.String | Province to inspect.

launch(Dispatchers.IO) {
    val result : OwnerConfig = webService.ownerConfigRetrieve(provinceId)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **provinceId** | **kotlin.String**| Province to inspect. | |

### Return type

[**OwnerConfig**](OwnerConfig.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


List the facilities the caller belongs to

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(OwnerApi::class.java)

launch(Dispatchers.IO) {
    val result : OwnerFacilitySummaryList = webService.ownerFacilitiesList()
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**OwnerFacilitySummaryList**](OwnerFacilitySummaryList.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Create a facility draft

The caller becomes the owner of the new draft. Creation re-checks the current province and category onboarding policy rather than any cached value.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(OwnerApi::class.java)
val facilityCreate : FacilityCreate =  // FacilityCreate | 

launch(Dispatchers.IO) {
    val result : OwnerFacilityDetail = webService.ownerFacilityCreate(facilityCreate)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **facilityCreate** | [**FacilityCreate**](FacilityCreate.md)|  | |

### Return type

[**OwnerFacilityDetail**](OwnerFacilityDetail.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


Confirm that the facility&#39;s opening hours are still right

Any owner or manager may confirm. Sets &#x60;hoursConfirmedAt&#x60;, which also moves the public &#x60;infoConfirmedAt&#x60;; &#x60;lastVerifiedAt&#x60; keeps meaning an operator approval. Replacing the hours confirms them too. 409 HOURS_NOT_SUPPORTED when the category has no opening hours.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(OwnerApi::class.java)
val facilityId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 

launch(Dispatchers.IO) {
    val result : OwnerHoursConfirmed = webService.ownerFacilityHoursConfirm(facilityId)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **facilityId** | **java.util.UUID**|  | |

### Return type

[**OwnerHoursConfirmed**](OwnerHoursConfirmed.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Engagement with a facility over the last 30 days

Counts of product analytics events that reference this facility.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(OwnerApi::class.java)
val facilityId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 

launch(Dispatchers.IO) {
    val result : OwnerFacilityInsights = webService.ownerFacilityInsightsRetrieve(facilityId)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **facilityId** | **java.util.UUID**|  | |

### Return type

[**OwnerFacilityInsights**](OwnerFacilityInsights.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Set the map point of a facility

WGS84 decimal degrees. PostGIS remains the source of truth for geo.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(OwnerApi::class.java)
val facilityId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 
val facilityLocation : FacilityLocation =  // FacilityLocation | 

launch(Dispatchers.IO) {
    val result : OwnerFacilityDetail = webService.ownerFacilityLocationReplace(facilityId, facilityLocation)
}
```

### Parameters
| **facilityId** | **java.util.UUID**|  | |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **facilityLocation** | [**FacilityLocation**](FacilityLocation.md)|  | |

### Return type

[**OwnerFacilityDetail**](OwnerFacilityDetail.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


Remove a member from a facility

Only an owner may call this, and the last owner cannot be removed.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(OwnerApi::class.java)
val facilityId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 
val userId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 

launch(Dispatchers.IO) {
    webService.ownerFacilityMemberDelete(facilityId, userId)
}
```

### Parameters
| **facilityId** | **java.util.UUID**|  | |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **userId** | **java.util.UUID**|  | |

### Return type

null (empty response body)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Add a member or change a member role

Only an owner may call this, and the last owner cannot be demoted.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(OwnerApi::class.java)
val facilityId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 
val facilityMember : FacilityMember =  // FacilityMember | 

launch(Dispatchers.IO) {
    val result : OwnerMemberUpserted = webService.ownerFacilityMemberUpsert(facilityId, facilityMember)
}
```

### Parameters
| **facilityId** | **java.util.UUID**|  | |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **facilityMember** | [**FacilityMember**](FacilityMember.md)|  | |

### Return type

[**OwnerMemberUpserted**](OwnerMemberUpserted.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


List the members of a facility

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(OwnerApi::class.java)
val facilityId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 

launch(Dispatchers.IO) {
    val result : OwnerMemberList = webService.ownerFacilityMembersList(facilityId)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **facilityId** | **java.util.UUID**|  | |

### Return type

[**OwnerMemberList**](OwnerMemberList.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Retrieve one facility the caller belongs to

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(OwnerApi::class.java)
val facilityId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 

launch(Dispatchers.IO) {
    val result : OwnerFacilityDetail = webService.ownerFacilityRetrieve(facilityId)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **facilityId** | **java.util.UUID**|  | |

### Return type

[**OwnerFacilityDetail**](OwnerFacilityDetail.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Submit a facility for review

Submission re-validates the current onboarding policy and the completeness of the current evidence requirements. Only one submitted application of a given kind can exist per facility at a time.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(OwnerApi::class.java)
val facilityId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 

launch(Dispatchers.IO) {
    val result : OwnerSubmitResult = webService.ownerFacilitySubmit(facilityId)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **facilityId** | **java.util.UUID**|  | |

### Return type

[**OwnerSubmitResult**](OwnerSubmitResult.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Update the core fields of a facility

Editing a sensitive field on an active facility moves it into REVERIFICATION_REQUIRED, so the change is reviewed before it becomes public.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(OwnerApi::class.java)
val facilityId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 
val patchedFacilityPatch : PatchedFacilityPatch =  // PatchedFacilityPatch | 

launch(Dispatchers.IO) {
    val result : OwnerFacilityDetail = webService.ownerFacilityUpdate(facilityId, patchedFacilityPatch)
}
```

### Parameters
| **facilityId** | **java.util.UUID**|  | |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **patchedFacilityPatch** | [**PatchedFacilityPatch**](PatchedFacilityPatch.md)|  | [optional] |

### Return type

[**OwnerFacilityDetail**](OwnerFacilityDetail.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

