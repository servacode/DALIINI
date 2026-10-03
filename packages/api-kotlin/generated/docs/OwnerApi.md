# OwnerApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
| ------------- | ------------- | ------------- |
| [**ownerClaimRetrieve**](OwnerApi.md#ownerClaimRetrieve) | **GET** api/v1/owner/claims/{claim_id}/ | One of this account&#39;s claims |
| [**ownerClaimStart**](OwnerApi.md#ownerClaimStart) | **POST** api/v1/owner/claims/ | Start claiming a facility |
| [**ownerClaimSubmit**](OwnerApi.md#ownerClaimSubmit) | **POST** api/v1/owner/claims/{claim_id}/submit/ | Send a claim for review |
| [**ownerClaimWithdraw**](OwnerApi.md#ownerClaimWithdraw) | **DELETE** api/v1/owner/claims/{claim_id}/ | Withdraw a claim and delete its documents |
| [**ownerClaimableFacilitiesList**](OwnerApi.md#ownerClaimableFacilitiesList) | **GET** api/v1/owner/claimable-facilities/ | Find a published facility nobody owns yet |
| [**ownerClaimsList**](OwnerApi.md#ownerClaimsList) | **GET** api/v1/owner/claims/ | This account&#39;s claims, newest first |
| [**ownerConfigRetrieve**](OwnerApi.md#ownerConfigRetrieve) | **GET** api/v1/owner/config/ | List categories open for owner onboarding in a province |
| [**ownerFacilitiesList**](OwnerApi.md#ownerFacilitiesList) | **GET** api/v1/owner/facilities/ | List the facilities the caller belongs to |
| [**ownerFacilityCreate**](OwnerApi.md#ownerFacilityCreate) | **POST** api/v1/owner/facilities/ | Create a facility draft |
| [**ownerFacilityHoursConfirm**](OwnerApi.md#ownerFacilityHoursConfirm) | **POST** api/v1/owner/facilities/{facility_id}/confirm-hours/ | Confirm that the facility&#39;s opening hours are still right |
| [**ownerFacilityInsightsRetrieve**](OwnerApi.md#ownerFacilityInsightsRetrieve) | **GET** api/v1/owner/facilities/{facility_id}/insights/ | Engagement with a facility over the last 30 days |
| [**ownerFacilityInvitationCreate**](OwnerApi.md#ownerFacilityInvitationCreate) | **POST** api/v1/owner/facilities/{facility_id}/invitations/ | Invite someone to help run a facility, by phone number |
| [**ownerFacilityInvitationRevoke**](OwnerApi.md#ownerFacilityInvitationRevoke) | **DELETE** api/v1/owner/facilities/{facility_id}/invitations/{invitation_id}/ | Withdraw an invitation that has not been answered |
| [**ownerFacilityInvitationsList**](OwnerApi.md#ownerFacilityInvitationsList) | **GET** api/v1/owner/facilities/{facility_id}/invitations/ | Invitations sent for a facility |
| [**ownerFacilityLocationReplace**](OwnerApi.md#ownerFacilityLocationReplace) | **PUT** api/v1/owner/facilities/{facility_id}/location/ | Set the map point of a facility |
| [**ownerFacilityMemberDelete**](OwnerApi.md#ownerFacilityMemberDelete) | **DELETE** api/v1/owner/facilities/{facility_id}/members/{user_id}/ | Remove a member from a facility |
| [**ownerFacilityMemberUpsert**](OwnerApi.md#ownerFacilityMemberUpsert) | **POST** api/v1/owner/facilities/{facility_id}/members/ | Add a member or change a member role |
| [**ownerFacilityMembersList**](OwnerApi.md#ownerFacilityMembersList) | **GET** api/v1/owner/facilities/{facility_id}/members/ | List the members of a facility |
| [**ownerFacilityRetrieve**](OwnerApi.md#ownerFacilityRetrieve) | **GET** api/v1/owner/facilities/{facility_id}/ | Retrieve one facility the caller belongs to |
| [**ownerFacilitySubmit**](OwnerApi.md#ownerFacilitySubmit) | **POST** api/v1/owner/facilities/{facility_id}/submit/ | Submit a facility for review |
| [**ownerFacilityUpdate**](OwnerApi.md#ownerFacilityUpdate) | **PATCH** api/v1/owner/facilities/{facility_id}/ | Update the core fields of a facility |



One of this account&#39;s claims

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(OwnerApi::class.java)
val claimId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 

launch(Dispatchers.IO) {
    val result : Claim = webService.ownerClaimRetrieve(claimId)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **claimId** | **java.util.UUID**|  | |

### Return type

[**Claim**](Claim.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Start claiming a facility

Returns the open claim this account already has for the facility, if any. 404 when the facility is not claimable; 409 FACILITY_ALREADY_OWNED when it has an owner, TOO_MANY_CLAIMS past five open claims.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(OwnerApi::class.java)
val claimStart : ClaimStart =  // ClaimStart | 

launch(Dispatchers.IO) {
    val result : Claim = webService.ownerClaimStart(claimStart)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **claimStart** | [**ClaimStart**](ClaimStart.md)|  | |

### Return type

[**Claim**](Claim.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


Send a claim for review

Every required document must be uploaded to the claim. 409 CLAIM_PENDING while another claim on the same facility is being reviewed; FACILITY_ALREADY_OWNED if it gained an owner meanwhile.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(OwnerApi::class.java)
val claimId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 

launch(Dispatchers.IO) {
    val result : Claim = webService.ownerClaimSubmit(claimId)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **claimId** | **java.util.UUID**|  | |

### Return type

[**Claim**](Claim.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Withdraw a claim and delete its documents

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(OwnerApi::class.java)
val claimId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 

launch(Dispatchers.IO) {
    webService.ownerClaimWithdraw(claimId)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **claimId** | **java.util.UUID**|  | |

### Return type

null (empty response body)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Find a published facility nobody owns yet

For «هذه منشأتي». Matches the Arabic or English name, Arabic spelling folded as in search. At most 20 results; &#x60;q&#x60; needs two characters.

### Example
```kotlin
// Import classes:
//import com.servacode.directory.api.*
//import com.servacode.directory.api.infrastructure.*
//import com.servacode.directory.api.models.*

val apiClient = ApiClient()
apiClient.setBearerToken("TOKEN")
val webService = apiClient.createWebservice(OwnerApi::class.java)
val q : kotlin.String = q_example // kotlin.String | 
val categoryId : kotlin.String = categoryId_example // kotlin.String | Keep facilities of this category.
val provinceId : kotlin.String = provinceId_example // kotlin.String | Keep facilities in this province.

launch(Dispatchers.IO) {
    val result : ClaimableFacilityList = webService.ownerClaimableFacilitiesList(q, categoryId, provinceId)
}
```

### Parameters
| **q** | **kotlin.String**|  | |
| **categoryId** | **kotlin.String**| Keep facilities of this category. | [optional] |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **provinceId** | **kotlin.String**| Keep facilities in this province. | [optional] |

### Return type

[**ClaimableFacilityList**](ClaimableFacilityList.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


This account&#39;s claims, newest first

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
    val result : ClaimList = webService.ownerClaimsList()
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**ClaimList**](ClaimList.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


List categories open for owner onboarding in a province

Returns only categories whose per-province owner switch is on and whose capability set allows onboarding, together with the safe descriptors of the verification requirements the owner will have to satisfy, and the specialties and services the owner may pick for a facility of each.

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


Invite someone to help run a facility, by phone number

Owners only. The answer is the same whether or not the number has an account, so this cannot be used to find out who is registered. A person with an account is notified at once; anyone else finds the invitation when they sign up with that number. It lasts seven days; inviting the same number again renews it. 409 ALREADY_MEMBER when the number belongs to a member already.

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
val invitationRequest : InvitationRequest =  // InvitationRequest | 

launch(Dispatchers.IO) {
    val result : Invitation = webService.ownerFacilityInvitationCreate(facilityId, invitationRequest)
}
```

### Parameters
| **facilityId** | **java.util.UUID**|  | |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **invitationRequest** | [**InvitationRequest**](InvitationRequest.md)|  | |

### Return type

[**Invitation**](Invitation.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json


Withdraw an invitation that has not been answered

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
val invitationId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 

launch(Dispatchers.IO) {
    webService.ownerFacilityInvitationRevoke(facilityId, invitationId)
}
```

### Parameters
| **facilityId** | **java.util.UUID**|  | |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **invitationId** | **java.util.UUID**|  | |

### Return type

null (empty response body)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Invitations sent for a facility

Owners only. Newest first.

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
    val result : InvitationList = webService.ownerFacilityInvitationsList(facilityId)
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **facilityId** | **java.util.UUID**|  | |

### Return type

[**InvitationList**](InvitationList.md)

### Authorization


Configure bearerAccessToken:
    ApiClient().setBearerToken("TOKEN")

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Set the map point of a facility

WGS84 decimal degrees. PostGIS remains the source of truth for geo. On an ACTIVE facility the new point waits for review and the published one stays.

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

Only an owner may call this, and the last owner cannot be demoted. Adding a new member by account id is deprecated: invite them by phone number with ownerFacilityInvitationCreate, which they accept themselves. Changing the role of an existing member stays here.

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

Submission re-validates the current onboarding policy and the completeness of the current evidence requirements. Only one submitted application of a given kind can exist per facility at a time. An ACTIVE facility is never taken down to be reviewed: its edits are sent as they are saved, and submitting answers with the change already waiting, or 400 when there is none.

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

On an ACTIVE facility the facility stays published: its name, address, city, neighbourhood and map point wait for an operator as a CHANGE application (&#x60;pendingChange&#x60; in the response), and every other field applies at once. A second edit while one waits is merged into it. Elsewhere the edit applies as it stands and is reviewed at the next submission.

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

