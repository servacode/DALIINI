# MediaApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
| ------------- | ------------- | ------------- |
| [**ownerFacilityEvidenceCreate**](MediaApi.md#ownerFacilityEvidenceCreate) | **POST** api/v1/owner/facilities/{facility_id}/evidence/ | Upload private verification evidence |
| [**ownerFacilityEvidenceDelete**](MediaApi.md#ownerFacilityEvidenceDelete) | **DELETE** api/v1/owner/facilities/{facility_id}/evidence/{evidence_id}/ | Delete a piece of verification evidence |
| [**ownerFacilityImageCreate**](MediaApi.md#ownerFacilityImageCreate) | **POST** api/v1/owner/facilities/{facility_id}/images/ | Upload a public facility image |
| [**ownerFacilityImageDelete**](MediaApi.md#ownerFacilityImageDelete) | **DELETE** api/v1/owner/facilities/{facility_id}/images/{image_id}/ | Delete a public facility image |
| [**ownerFacilityImagesList**](MediaApi.md#ownerFacilityImagesList) | **GET** api/v1/owner/facilities/{facility_id}/images/ | List the public images of a facility |



Upload private verification evidence

Sent as multipart/form-data and stored in the private namespace. The response carries identifiers only: evidence is never served through a public URL and its storage key is never returned.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(MediaApi::class.java)
val facilityId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 
val requirementId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 
val file : java.net.URI = file_example // java.net.URI | 

launch(Dispatchers.IO) {
    val result : OwnerEvidenceCreated = webService.ownerFacilityEvidenceCreate(facilityId, requirementId, file)
}
```

### Parameters
| **facilityId** | **java.util.UUID**|  | |
| **requirementId** | **java.util.UUID**|  | |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **file** | **java.net.URI**|  | |

### Return type

[**OwnerEvidenceCreated**](OwnerEvidenceCreated.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json


Delete a piece of verification evidence

Evidence is locked while an application is under review.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(MediaApi::class.java)
val evidenceId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 
val facilityId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 

launch(Dispatchers.IO) {
    webService.ownerFacilityEvidenceDelete(evidenceId, facilityId)
}
```

### Parameters
| **evidenceId** | **java.util.UUID**|  | |
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


Upload a public facility image

Sent as multipart/form-data. The server decodes the file, enforces byte and pixel limits, re-encodes to JPEG, strips metadata and stores it under a random key. The declared extension and MIME type are not trusted.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(MediaApi::class.java)
val facilityId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 
val file : java.net.URI = file_example // java.net.URI | 

launch(Dispatchers.IO) {
    val result : OwnerFacilityImage = webService.ownerFacilityImageCreate(facilityId, file)
}
```

### Parameters
| **facilityId** | **java.util.UUID**|  | |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **file** | **java.net.URI**|  | |

### Return type

[**OwnerFacilityImage**](OwnerFacilityImage.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json


Delete a public facility image

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(MediaApi::class.java)
val facilityId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 
val imageId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 

launch(Dispatchers.IO) {
    webService.ownerFacilityImageDelete(facilityId, imageId)
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


List the public images of a facility

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(MediaApi::class.java)
val facilityId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 

launch(Dispatchers.IO) {
    val result : OwnerFacilityImageList = webService.ownerFacilityImagesList(facilityId)
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

