# AdminFacilitiesApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
| ------------- | ------------- | ------------- |
| [**adminFacilitiesList**](AdminFacilitiesApi.md#adminFacilitiesList) | **GET** api/v1/admin/facilities/ | List facilities for operations |
| [**adminFacilitiesMap**](AdminFacilitiesApi.md#adminFacilitiesMap) | **GET** api/v1/admin/facilities/map/ | Located facilities as map points, with the same filters as the list |
| [**adminFacilityClose**](AdminFacilitiesApi.md#adminFacilityClose) | **POST** api/v1/admin/facilities/{facility_id}/close/ | Close a facility |
| [**adminFacilityCreate**](AdminFacilitiesApi.md#adminFacilityCreate) | **POST** api/v1/admin/facilities/ | Add a facility to the directory |
| [**adminFacilityHoursList**](AdminFacilitiesApi.md#adminFacilityHoursList) | **GET** api/v1/admin/facilities/{facility_id}/hours/ | A facility&#39;s weekly opening hours |
| [**adminFacilityHoursReplace**](AdminFacilitiesApi.md#adminFacilityHoursReplace) | **PUT** api/v1/admin/facilities/{facility_id}/hours/ | Replace a facility&#39;s weekly opening hours |
| [**adminFacilityImageCreate**](AdminFacilitiesApi.md#adminFacilityImageCreate) | **POST** api/v1/admin/facilities/{facility_id}/images/ | Add a public photo to a facility |
| [**adminFacilityImageDelete**](AdminFacilitiesApi.md#adminFacilityImageDelete) | **DELETE** api/v1/admin/facilities/{facility_id}/images/{image_id}/ | Remove a public photo from a facility |
| [**adminFacilityImagesList**](AdminFacilitiesApi.md#adminFacilityImagesList) | **GET** api/v1/admin/facilities/{facility_id}/images/ | A facility&#39;s public photos |
| [**adminFacilityOwnerTransfer**](AdminFacilitiesApi.md#adminFacilityOwnerTransfer) | **POST** api/v1/admin/facilities/{facility_id}/owner/ | Move a facility to another owner |
| [**adminFacilityReactivate**](AdminFacilitiesApi.md#adminFacilityReactivate) | **POST** api/v1/admin/facilities/{facility_id}/reactivate/ | Reactivate a suspended facility |
| [**adminFacilityRetrieve**](AdminFacilitiesApi.md#adminFacilityRetrieve) | **GET** api/v1/admin/facilities/{facility_id}/ | Retrieve one facility |
| [**adminFacilitySuspend**](AdminFacilitiesApi.md#adminFacilitySuspend) | **POST** api/v1/admin/facilities/{facility_id}/suspend/ | Suspend a facility |
| [**adminFacilityTimelineRetrieve**](AdminFacilitiesApi.md#adminFacilityTimelineRetrieve) | **GET** api/v1/admin/facilities/{facility_id}/timeline/ | Everything that happened to a facility, newest first |
| [**adminFacilityUpdate**](AdminFacilitiesApi.md#adminFacilityUpdate) | **PATCH** api/v1/admin/facilities/{facility_id}/ | Correct a facility&#39;s details |



List facilities for operations

In cursor pages. Every filter is optional and combines with the rest. Each row carries &#x60;qualityScore&#x60; (0-100) and &#x60;qualityIssues&#x60;, computed in the same query.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminFacilitiesApi::class.java)
val category : kotlin.String = category_example // kotlin.String | Category id.
val city : kotlin.String = city_example // kotlin.String | City id.
val cursor : kotlin.String = cursor_example // kotlin.String | Opaque token returned as `nextCursor` by the previous page.
val id : kotlin.String = id_example // kotlin.String | One facility by id: what a link written before the console had cards resolves to.
val issue : kotlin.String = issue_example // kotlin.String | Keep facilities that have this quality issue.
val limit : kotlin.Int = 56 // kotlin.Int | Page size, maximum 200, default 50.
val ordering : kotlin.String = ordering_example // kotlin.String | Sort order; the default is `-updatedAt` (most recently changed).
val province : kotlin.String = province_example // kotlin.String | Province id.
val q : kotlin.String = q_example // kotlin.String | Free text matched against the Arabic and English facility names.
val status : kotlin.String = status_example // kotlin.String | Facility status, for example ACTIVE or SUSPENDED.

launch(Dispatchers.IO) {
    val result : AdminFacilityList = webService.adminFacilitiesList(category, city, cursor, id, issue, limit, ordering, province, q, status)
}
```

### Parameters
| **category** | **kotlin.String**| Category id. | [optional] |
| **city** | **kotlin.String**| City id. | [optional] |
| **cursor** | **kotlin.String**| Opaque token returned as &#x60;nextCursor&#x60; by the previous page. | [optional] |
| **id** | **kotlin.String**| One facility by id: what a link written before the console had cards resolves to. | [optional] |
| **issue** | **kotlin.String**| Keep facilities that have this quality issue. | [optional] [enum: NOT_VERIFIED_RECENTLY, NO_HOURS, NO_LOCATION, NO_PHONE, NO_PHOTOS, OPEN_REPORTS, STALE] |
| **limit** | **kotlin.Int**| Page size, maximum 200, default 50. | [optional] |
| **ordering** | **kotlin.String**| Sort order; the default is &#x60;-updatedAt&#x60; (most recently changed). | [optional] [enum: -qualityScore, -updatedAt, qualityScore, updatedAt] |
| **province** | **kotlin.String**| Province id. | [optional] |
| **q** | **kotlin.String**| Free text matched against the Arabic and English facility names. | [optional] |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **status** | **kotlin.String**| Facility status, for example ACTIVE or SUSPENDED. | [optional] |

### Return type

[**AdminFacilityList**](AdminFacilityList.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Located facilities as map points, with the same filters as the list

Every located facility the filters select, as points (DECISION-075).  The same filters as the list, so \&quot;the map of what I am looking at\&quot; is one click. Only what a pin needs travels: the name, the state and the coordinates. A facility without a location is counted rather than dropped silently, so the operator can go and fix it.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminFacilitiesApi::class.java)
val category : kotlin.String = category_example // kotlin.String | Category id.
val city : kotlin.String = city_example // kotlin.String | City id.
val issue : kotlin.String = issue_example // kotlin.String | One of NO_PHOTOS, NO_HOURS, NO_LOCATION, NO_PHONE, STALE, OPEN_REPORTS, NOT_VERIFIED_RECENTLY.
val province : kotlin.String = province_example // kotlin.String | Province id.
val q : kotlin.String = q_example // kotlin.String | Free text matched against the facility names.
val status : kotlin.String = status_example // kotlin.String | Facility status.

launch(Dispatchers.IO) {
    val result : AdminFacilityMap = webService.adminFacilitiesMap(category, city, issue, province, q, status)
}
```

### Parameters
| **category** | **kotlin.String**| Category id. | [optional] |
| **city** | **kotlin.String**| City id. | [optional] |
| **issue** | **kotlin.String**| One of NO_PHOTOS, NO_HOURS, NO_LOCATION, NO_PHONE, STALE, OPEN_REPORTS, NOT_VERIFIED_RECENTLY. | [optional] |
| **province** | **kotlin.String**| Province id. | [optional] |
| **q** | **kotlin.String**| Free text matched against the facility names. | [optional] |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **status** | **kotlin.String**| Facility status. | [optional] |

### Return type

[**AdminFacilityMap**](AdminFacilityMap.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Close a facility

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminFacilitiesApi::class.java)
val facilityId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 
val adminDecisionRequest : AdminDecisionRequest =  // AdminDecisionRequest | 

launch(Dispatchers.IO) {
    val result : AdminFacility = webService.adminFacilityClose(facilityId, adminDecisionRequest)
}
```

### Parameters
| **facilityId** | **java.util.UUID**|  | |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **adminDecisionRequest** | [**AdminDecisionRequest**](AdminDecisionRequest.md)|  | [optional] |

### Return type

[**AdminFacility**](AdminFacility.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


Add a facility to the directory

Listed by the directory itself, with no owner; an owner can claim it later. ACTIVE (the default) publishes it at once and counts as verified. The same validation as an owner&#39;s edit applies. Requires &#x60;admin.facilities.edit&#x60;; audited.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminFacilitiesApi::class.java)
val adminFacilityCreate : AdminFacilityCreate =  // AdminFacilityCreate | 

launch(Dispatchers.IO) {
    val result : AdminFacilityDetail = webService.adminFacilityCreate(adminFacilityCreate)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **adminFacilityCreate** | [**AdminFacilityCreate**](AdminFacilityCreate.md)|  | |

### Return type

[**AdminFacilityDetail**](AdminFacilityDetail.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


A facility&#39;s weekly opening hours

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminFacilitiesApi::class.java)
val facilityId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 

launch(Dispatchers.IO) {
    val result : BusinessHoursList = webService.adminFacilityHoursList(facilityId)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **facilityId** | **java.util.UUID**|  | |

### Return type

[**BusinessHoursList**](BusinessHoursList.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Replace a facility&#39;s weekly opening hours

The whole week in one call, as the owner&#39;s own route: overnight spans are allowed and same-day overlaps refused. Requires &#x60;admin.facilities.edit&#x60;.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminFacilitiesApi::class.java)
val facilityId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 
val businessHourInput : kotlin.collections.List<BusinessHourInput> =  // kotlin.collections.List<BusinessHourInput> | 

launch(Dispatchers.IO) {
    val result : BusinessHoursList = webService.adminFacilityHoursReplace(facilityId, businessHourInput)
}
```

### Parameters
| **facilityId** | **java.util.UUID**|  | |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **businessHourInput** | [**kotlin.collections.List&lt;BusinessHourInput&gt;**](BusinessHourInput.md)|  | |

### Return type

[**BusinessHoursList**](BusinessHoursList.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


Add a public photo to a facility

Multipart, through the same pipeline as the owner&#39;s upload: the file is decoded, bounded, re-encoded to JPEG, stripped and stored under a random key. Requires &#x60;admin.facilities.edit&#x60;.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminFacilitiesApi::class.java)
val facilityId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 
val file : java.io.File = BINARY_DATA_HERE // java.io.File | 

launch(Dispatchers.IO) {
    val result : OwnerFacilityImage = webService.adminFacilityImageCreate(facilityId, file)
}
```

### Parameters
| **facilityId** | **java.util.UUID**|  | |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **file** | **java.io.File**|  | |

### Return type

[**OwnerFacilityImage**](OwnerFacilityImage.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json


Remove a public photo from a facility

Requires &#x60;admin.facilities.edit&#x60;. The stored file goes once the row does.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminFacilitiesApi::class.java)
val facilityId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 
val imageId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 

launch(Dispatchers.IO) {
    webService.adminFacilityImageDelete(facilityId, imageId)
}
```

### Parameters
| **facilityId** | **java.util.UUID**|  | |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **imageId** | **java.util.UUID**|  | |

### Return type

null (empty response body)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


A facility&#39;s public photos

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminFacilitiesApi::class.java)
val facilityId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 

launch(Dispatchers.IO) {
    val result : OwnerFacilityImageList = webService.adminFacilityImagesList(facilityId)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **facilityId** | **java.util.UUID**|  | |

### Return type

[**OwnerFacilityImageList**](OwnerFacilityImageList.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Move a facility to another owner

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminFacilitiesApi::class.java)
val facilityId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 
val adminFacilityOwnerRequest : AdminFacilityOwnerRequest =  // AdminFacilityOwnerRequest | 

launch(Dispatchers.IO) {
    val result : AdminFacilityOwner = webService.adminFacilityOwnerTransfer(facilityId, adminFacilityOwnerRequest)
}
```

### Parameters
| **facilityId** | **java.util.UUID**|  | |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **adminFacilityOwnerRequest** | [**AdminFacilityOwnerRequest**](AdminFacilityOwnerRequest.md)|  | |

### Return type

[**AdminFacilityOwner**](AdminFacilityOwner.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


Reactivate a suspended facility

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminFacilitiesApi::class.java)
val facilityId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 
val adminDecisionRequest : AdminDecisionRequest =  // AdminDecisionRequest | 

launch(Dispatchers.IO) {
    val result : AdminFacility = webService.adminFacilityReactivate(facilityId, adminDecisionRequest)
}
```

### Parameters
| **facilityId** | **java.util.UUID**|  | |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **adminDecisionRequest** | [**AdminDecisionRequest**](AdminDecisionRequest.md)|  | [optional] |

### Return type

[**AdminFacility**](AdminFacility.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


Retrieve one facility

Everything the console shows and edits, with the quality score.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminFacilitiesApi::class.java)
val facilityId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 

launch(Dispatchers.IO) {
    val result : AdminFacilityDetail = webService.adminFacilityRetrieve(facilityId)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **facilityId** | **java.util.UUID**|  | |

### Return type

[**AdminFacilityDetail**](AdminFacilityDetail.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Suspend a facility

A suspended facility leaves public discovery and cannot self-reactivate.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminFacilitiesApi::class.java)
val facilityId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 
val adminDecisionRequest : AdminDecisionRequest =  // AdminDecisionRequest | 

launch(Dispatchers.IO) {
    val result : AdminFacility = webService.adminFacilitySuspend(facilityId, adminDecisionRequest)
}
```

### Parameters
| **facilityId** | **java.util.UUID**|  | |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **adminDecisionRequest** | [**AdminDecisionRequest**](AdminDecisionRequest.md)|  | [optional] |

### Return type

[**AdminFacility**](AdminFacility.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


Everything that happened to a facility, newest first

Merges applications (submitted, decided), problem reports (created, resolved or dismissed), audited changes to the facility and its applications, reports, images, evidence and duty shifts, and a summary of the next 14 days of duty. Up to 200 events.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminFacilitiesApi::class.java)
val facilityId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 

launch(Dispatchers.IO) {
    val result : AdminTimeline = webService.adminFacilityTimelineRetrieve(facilityId)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **facilityId** | **java.util.UUID**|  | |

### Return type

[**AdminTimeline**](AdminTimeline.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Correct a facility&#39;s details

Only the fields sent change. The status is left as it is: an operator&#39;s correction does not send a live facility back for re-verification. Moving it to another province clears its city unless one is sent; another category clears its specialties and services unless they are sent. Requires &#x60;admin.facilities.edit&#x60;; audited with both snapshots, and the owners are notified.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(AdminFacilitiesApi::class.java)
val facilityId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 
val patchedAdminFacilityWrite : PatchedAdminFacilityWrite =  // PatchedAdminFacilityWrite | 

launch(Dispatchers.IO) {
    val result : AdminFacilityDetail = webService.adminFacilityUpdate(facilityId, patchedAdminFacilityWrite)
}
```

### Parameters
| **facilityId** | **java.util.UUID**|  | |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **patchedAdminFacilityWrite** | [**PatchedAdminFacilityWrite**](PatchedAdminFacilityWrite.md)|  | [optional] |

### Return type

[**AdminFacilityDetail**](AdminFacilityDetail.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

