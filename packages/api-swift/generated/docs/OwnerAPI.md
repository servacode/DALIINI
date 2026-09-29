# OwnerAPI

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**ownerConfigRetrieve**](OwnerAPI.md#ownerconfigretrieve) | **GET** /api/v1/owner/config/ | List categories open for owner onboarding in a province
[**ownerFacilitiesList**](OwnerAPI.md#ownerfacilitieslist) | **GET** /api/v1/owner/facilities/ | List the facilities the caller belongs to
[**ownerFacilityCreate**](OwnerAPI.md#ownerfacilitycreate) | **POST** /api/v1/owner/facilities/ | Create a facility draft
[**ownerFacilityHoursConfirm**](OwnerAPI.md#ownerfacilityhoursconfirm) | **POST** /api/v1/owner/facilities/{facility_id}/confirm-hours/ | Confirm that the facility&#39;s opening hours are still right
[**ownerFacilityInsightsRetrieve**](OwnerAPI.md#ownerfacilityinsightsretrieve) | **GET** /api/v1/owner/facilities/{facility_id}/insights/ | Engagement with a facility over the last 30 days
[**ownerFacilityLocationReplace**](OwnerAPI.md#ownerfacilitylocationreplace) | **PUT** /api/v1/owner/facilities/{facility_id}/location/ | Set the map point of a facility
[**ownerFacilityMemberDelete**](OwnerAPI.md#ownerfacilitymemberdelete) | **DELETE** /api/v1/owner/facilities/{facility_id}/members/{user_id}/ | Remove a member from a facility
[**ownerFacilityMemberUpsert**](OwnerAPI.md#ownerfacilitymemberupsert) | **POST** /api/v1/owner/facilities/{facility_id}/members/ | Add a member or change a member role
[**ownerFacilityMembersList**](OwnerAPI.md#ownerfacilitymemberslist) | **GET** /api/v1/owner/facilities/{facility_id}/members/ | List the members of a facility
[**ownerFacilityRetrieve**](OwnerAPI.md#ownerfacilityretrieve) | **GET** /api/v1/owner/facilities/{facility_id}/ | Retrieve one facility the caller belongs to
[**ownerFacilitySubmit**](OwnerAPI.md#ownerfacilitysubmit) | **POST** /api/v1/owner/facilities/{facility_id}/submit/ | Submit a facility for review
[**ownerFacilityUpdate**](OwnerAPI.md#ownerfacilityupdate) | **PATCH** /api/v1/owner/facilities/{facility_id}/ | Update the core fields of a facility


# **ownerConfigRetrieve**
```swift
    open class func ownerConfigRetrieve(provinceId: String, completion: @escaping (_ data: OwnerConfig?, _ error: Error?) -> Void)
```

List categories open for owner onboarding in a province

Returns only categories whose per-province owner switch is on and whose capability set allows onboarding, together with the safe descriptors of the verification requirements the owner will have to satisfy, and the specialties and services the owner may pick for a facility of each.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let provinceId = "provinceId_example" // String | Province to inspect.

// List categories open for owner onboarding in a province
OwnerAPI.ownerConfigRetrieve(provinceId: provinceId) { (response, error) in
    guard error == nil else {
        print(error)
        return
    }

    if (response) {
        dump(response)
    }
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **provinceId** | **String** | Province to inspect. | 

### Return type

[**OwnerConfig**](OwnerConfig.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **ownerFacilitiesList**
```swift
    open class func ownerFacilitiesList(completion: @escaping (_ data: OwnerFacilitySummaryList?, _ error: Error?) -> Void)
```

List the facilities the caller belongs to

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI


// List the facilities the caller belongs to
OwnerAPI.ownerFacilitiesList() { (response, error) in
    guard error == nil else {
        print(error)
        return
    }

    if (response) {
        dump(response)
    }
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**OwnerFacilitySummaryList**](OwnerFacilitySummaryList.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **ownerFacilityCreate**
```swift
    open class func ownerFacilityCreate(facilityCreate: FacilityCreate, completion: @escaping (_ data: OwnerFacilityDetail?, _ error: Error?) -> Void)
```

Create a facility draft

The caller becomes the owner of the new draft. Creation re-checks the current province and category onboarding policy rather than any cached value.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let facilityCreate = FacilityCreate(provinceId: 123, categoryId: 123, nameAr: "nameAr_example", nameEn: "nameEn_example") // FacilityCreate | 

// Create a facility draft
OwnerAPI.ownerFacilityCreate(facilityCreate: facilityCreate) { (response, error) in
    guard error == nil else {
        print(error)
        return
    }

    if (response) {
        dump(response)
    }
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **facilityCreate** | [**FacilityCreate**](FacilityCreate.md) |  | 

### Return type

[**OwnerFacilityDetail**](OwnerFacilityDetail.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **ownerFacilityHoursConfirm**
```swift
    open class func ownerFacilityHoursConfirm(facilityId: UUID, completion: @escaping (_ data: OwnerHoursConfirmed?, _ error: Error?) -> Void)
```

Confirm that the facility's opening hours are still right

Any owner or manager may confirm. Sets `hoursConfirmedAt`, which also moves the public `infoConfirmedAt`; `lastVerifiedAt` keeps meaning an operator approval. Replacing the hours confirms them too. 409 HOURS_NOT_SUPPORTED when the category has no opening hours.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let facilityId = 987 // UUID | 

// Confirm that the facility's opening hours are still right
OwnerAPI.ownerFacilityHoursConfirm(facilityId: facilityId) { (response, error) in
    guard error == nil else {
        print(error)
        return
    }

    if (response) {
        dump(response)
    }
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **facilityId** | **UUID** |  | 

### Return type

[**OwnerHoursConfirmed**](OwnerHoursConfirmed.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **ownerFacilityInsightsRetrieve**
```swift
    open class func ownerFacilityInsightsRetrieve(facilityId: UUID, completion: @escaping (_ data: OwnerFacilityInsights?, _ error: Error?) -> Void)
```

Engagement with a facility over the last 30 days

Counts of product analytics events that reference this facility.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let facilityId = 987 // UUID | 

// Engagement with a facility over the last 30 days
OwnerAPI.ownerFacilityInsightsRetrieve(facilityId: facilityId) { (response, error) in
    guard error == nil else {
        print(error)
        return
    }

    if (response) {
        dump(response)
    }
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **facilityId** | **UUID** |  | 

### Return type

[**OwnerFacilityInsights**](OwnerFacilityInsights.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **ownerFacilityLocationReplace**
```swift
    open class func ownerFacilityLocationReplace(facilityId: UUID, facilityLocation: FacilityLocation, completion: @escaping (_ data: OwnerFacilityDetail?, _ error: Error?) -> Void)
```

Set the map point of a facility

WGS84 decimal degrees. PostGIS remains the source of truth for geo.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let facilityId = 987 // UUID | 
let facilityLocation = FacilityLocation(latitude: 123, longitude: 123) // FacilityLocation | 

// Set the map point of a facility
OwnerAPI.ownerFacilityLocationReplace(facilityId: facilityId, facilityLocation: facilityLocation) { (response, error) in
    guard error == nil else {
        print(error)
        return
    }

    if (response) {
        dump(response)
    }
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **facilityId** | **UUID** |  | 
 **facilityLocation** | [**FacilityLocation**](FacilityLocation.md) |  | 

### Return type

[**OwnerFacilityDetail**](OwnerFacilityDetail.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **ownerFacilityMemberDelete**
```swift
    open class func ownerFacilityMemberDelete(facilityId: UUID, userId: UUID, completion: @escaping (_ data: Void?, _ error: Error?) -> Void)
```

Remove a member from a facility

Only an owner may call this, and the last owner cannot be removed.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let facilityId = 987 // UUID | 
let userId = 987 // UUID | 

// Remove a member from a facility
OwnerAPI.ownerFacilityMemberDelete(facilityId: facilityId, userId: userId) { (response, error) in
    guard error == nil else {
        print(error)
        return
    }

    if (response) {
        dump(response)
    }
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **facilityId** | **UUID** |  | 
 **userId** | **UUID** |  | 

### Return type

Void (empty response body)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **ownerFacilityMemberUpsert**
```swift
    open class func ownerFacilityMemberUpsert(facilityId: UUID, facilityMember: FacilityMember, completion: @escaping (_ data: OwnerMemberUpserted?, _ error: Error?) -> Void)
```

Add a member or change a member role

Only an owner may call this, and the last owner cannot be demoted.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let facilityId = 987 // UUID | 
let facilityMember = FacilityMember(userId: 123, role: FacilityMemberRoleEnum()) // FacilityMember | 

// Add a member or change a member role
OwnerAPI.ownerFacilityMemberUpsert(facilityId: facilityId, facilityMember: facilityMember) { (response, error) in
    guard error == nil else {
        print(error)
        return
    }

    if (response) {
        dump(response)
    }
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **facilityId** | **UUID** |  | 
 **facilityMember** | [**FacilityMember**](FacilityMember.md) |  | 

### Return type

[**OwnerMemberUpserted**](OwnerMemberUpserted.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **ownerFacilityMembersList**
```swift
    open class func ownerFacilityMembersList(facilityId: UUID, completion: @escaping (_ data: OwnerMemberList?, _ error: Error?) -> Void)
```

List the members of a facility

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let facilityId = 987 // UUID | 

// List the members of a facility
OwnerAPI.ownerFacilityMembersList(facilityId: facilityId) { (response, error) in
    guard error == nil else {
        print(error)
        return
    }

    if (response) {
        dump(response)
    }
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **facilityId** | **UUID** |  | 

### Return type

[**OwnerMemberList**](OwnerMemberList.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **ownerFacilityRetrieve**
```swift
    open class func ownerFacilityRetrieve(facilityId: UUID, completion: @escaping (_ data: OwnerFacilityDetail?, _ error: Error?) -> Void)
```

Retrieve one facility the caller belongs to

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let facilityId = 987 // UUID | 

// Retrieve one facility the caller belongs to
OwnerAPI.ownerFacilityRetrieve(facilityId: facilityId) { (response, error) in
    guard error == nil else {
        print(error)
        return
    }

    if (response) {
        dump(response)
    }
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **facilityId** | **UUID** |  | 

### Return type

[**OwnerFacilityDetail**](OwnerFacilityDetail.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **ownerFacilitySubmit**
```swift
    open class func ownerFacilitySubmit(facilityId: UUID, completion: @escaping (_ data: OwnerSubmitResult?, _ error: Error?) -> Void)
```

Submit a facility for review

Submission re-validates the current onboarding policy and the completeness of the current evidence requirements. Only one submitted application of a given kind can exist per facility at a time.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let facilityId = 987 // UUID | 

// Submit a facility for review
OwnerAPI.ownerFacilitySubmit(facilityId: facilityId) { (response, error) in
    guard error == nil else {
        print(error)
        return
    }

    if (response) {
        dump(response)
    }
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **facilityId** | **UUID** |  | 

### Return type

[**OwnerSubmitResult**](OwnerSubmitResult.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **ownerFacilityUpdate**
```swift
    open class func ownerFacilityUpdate(facilityId: UUID, patchedFacilityPatch: PatchedFacilityPatch? = nil, completion: @escaping (_ data: OwnerFacilityDetail?, _ error: Error?) -> Void)
```

Update the core fields of a facility

Editing a sensitive field on an active facility moves it into REVERIFICATION_REQUIRED, so the change is reviewed before it becomes public.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let facilityId = 987 // UUID | 
let patchedFacilityPatch = PatchedFacilityPatch(nameAr: "nameAr_example", nameEn: "nameEn_example", descriptionAr: "descriptionAr_example", descriptionEn: "descriptionEn_example", phone: "phone_example", whatsapp: "whatsapp_example", addressAr: "addressAr_example", addressEn: "addressEn_example", cityId: 123, neighborhoodId: 123, specialtyIds: [123], serviceTagIds: [123]) // PatchedFacilityPatch |  (optional)

// Update the core fields of a facility
OwnerAPI.ownerFacilityUpdate(facilityId: facilityId, patchedFacilityPatch: patchedFacilityPatch) { (response, error) in
    guard error == nil else {
        print(error)
        return
    }

    if (response) {
        dump(response)
    }
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **facilityId** | **UUID** |  | 
 **patchedFacilityPatch** | [**PatchedFacilityPatch**](PatchedFacilityPatch.md) |  | [optional] 

### Return type

[**OwnerFacilityDetail**](OwnerFacilityDetail.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

