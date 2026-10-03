# AdminReviewsAPI

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**adminEvidenceContentRetrieve**](AdminReviewsAPI.md#adminevidencecontentretrieve) | **GET** /api/v1/admin/evidence/{evidence_id}/content/ | Stream one piece of private verification evidence
[**adminRejectionTemplateCreate**](AdminReviewsAPI.md#adminrejectiontemplatecreate) | **POST** /api/v1/admin/rejection-templates/ | Create a rejection template
[**adminRejectionTemplateDelete**](AdminReviewsAPI.md#adminrejectiontemplatedelete) | **DELETE** /api/v1/admin/rejection-templates/{template_id}/ | Delete a rejection template
[**adminRejectionTemplateUpdate**](AdminReviewsAPI.md#adminrejectiontemplateupdate) | **PUT** /api/v1/admin/rejection-templates/{template_id}/ | Edit, reorder or retire a rejection template
[**adminRejectionTemplatesList**](AdminReviewsAPI.md#adminrejectiontemplateslist) | **GET** /api/v1/admin/rejection-templates/ | List rejection templates
[**adminReviewApprove**](AdminReviewsAPI.md#adminreviewapprove) | **POST** /api/v1/admin/applications/{application_id}/approve/ | Approve an application
[**adminReviewReject**](AdminReviewsAPI.md#adminreviewreject) | **POST** /api/v1/admin/applications/{application_id}/reject/ | Reject an application
[**adminReviewRetrieve**](AdminReviewsAPI.md#adminreviewretrieve) | **GET** /api/v1/admin/applications/{application_id}/ | Retrieve one application with its review context
[**adminReviewsList**](AdminReviewsAPI.md#adminreviewslist) | **GET** /api/v1/admin/applications/ | List facility applications awaiting or past review


# **adminEvidenceContentRetrieve**
```swift
    open class func adminEvidenceContentRetrieve(evidenceId: UUID, completion: @escaping (_ data: URL?, _ error: Error?) -> Void)
```

Stream one piece of private verification evidence

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let evidenceId = 987 // UUID | 

// Stream one piece of private verification evidence
AdminReviewsAPI.adminEvidenceContentRetrieve(evidenceId: evidenceId) { (response, error) in
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
 **evidenceId** | **UUID** |  | 

### Return type

**URL**

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminRejectionTemplateCreate**
```swift
    open class func adminRejectionTemplateCreate(adminRejectionTemplateRequest: AdminRejectionTemplateRequest, completion: @escaping (_ data: AdminRejectionTemplate?, _ error: Error?) -> Void)
```

Create a rejection template

Requires admin.reviews.decide, re-checked inside the handler.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let adminRejectionTemplateRequest = AdminRejectionTemplateRequest(titleAr: "titleAr_example", bodyAr: "bodyAr_example", active: false, sortOrder: 123) // AdminRejectionTemplateRequest | 

// Create a rejection template
AdminReviewsAPI.adminRejectionTemplateCreate(adminRejectionTemplateRequest: adminRejectionTemplateRequest) { (response, error) in
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
 **adminRejectionTemplateRequest** | [**AdminRejectionTemplateRequest**](AdminRejectionTemplateRequest.md) |  | 

### Return type

[**AdminRejectionTemplate**](AdminRejectionTemplate.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminRejectionTemplateDelete**
```swift
    open class func adminRejectionTemplateDelete(templateId: UUID, completion: @escaping (_ data: Void?, _ error: Error?) -> Void)
```

Delete a rejection template

Past rejections keep their text; a template is only a starting point.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let templateId = 987 // UUID | 

// Delete a rejection template
AdminReviewsAPI.adminRejectionTemplateDelete(templateId: templateId) { (response, error) in
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
 **templateId** | **UUID** |  | 

### Return type

Void (empty response body)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminRejectionTemplateUpdate**
```swift
    open class func adminRejectionTemplateUpdate(templateId: UUID, adminRejectionTemplateRequest: AdminRejectionTemplateRequest, completion: @escaping (_ data: AdminRejectionTemplate?, _ error: Error?) -> Void)
```

Edit, reorder or retire a rejection template

Omitted fields keep their value.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let templateId = 987 // UUID | 
let adminRejectionTemplateRequest = AdminRejectionTemplateRequest(titleAr: "titleAr_example", bodyAr: "bodyAr_example", active: false, sortOrder: 123) // AdminRejectionTemplateRequest | 

// Edit, reorder or retire a rejection template
AdminReviewsAPI.adminRejectionTemplateUpdate(templateId: templateId, adminRejectionTemplateRequest: adminRejectionTemplateRequest) { (response, error) in
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
 **templateId** | **UUID** |  | 
 **adminRejectionTemplateRequest** | [**AdminRejectionTemplateRequest**](AdminRejectionTemplateRequest.md) |  | 

### Return type

[**AdminRejectionTemplate**](AdminRejectionTemplate.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminRejectionTemplatesList**
```swift
    open class func adminRejectionTemplatesList(active: Bool? = nil, completion: @escaping (_ data: AdminRejectionTemplateList?, _ error: Error?) -> Void)
```

List rejection templates

Ordered by `sortOrder`. `active=true` keeps only the active ones.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let active = true // Bool |  (optional)

// List rejection templates
AdminReviewsAPI.adminRejectionTemplatesList(active: active) { (response, error) in
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
 **active** | **Bool** |  | [optional] 

### Return type

[**AdminRejectionTemplateList**](AdminRejectionTemplateList.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminReviewApprove**
```swift
    open class func adminReviewApprove(applicationId: UUID, adminReviewDecisionRequest: AdminReviewDecisionRequest? = nil, completion: @escaping (_ data: AdminApplication?, _ error: Error?) -> Void)
```

Approve an application

Runs in one transaction: the application and the facility lifecycle are locked, the current requirements are re-checked, the change is audited and the realtime event is emitted only after commit.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let applicationId = 987 // UUID | 
let adminReviewDecisionRequest = AdminReviewDecisionRequest(reason: "reason_example", revision: 123) // AdminReviewDecisionRequest |  (optional)

// Approve an application
AdminReviewsAPI.adminReviewApprove(applicationId: applicationId, adminReviewDecisionRequest: adminReviewDecisionRequest) { (response, error) in
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
 **applicationId** | **UUID** |  | 
 **adminReviewDecisionRequest** | [**AdminReviewDecisionRequest**](AdminReviewDecisionRequest.md) |  | [optional] 

### Return type

[**AdminApplication**](AdminApplication.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminReviewReject**
```swift
    open class func adminReviewReject(applicationId: UUID, adminReviewDecisionRequest: AdminReviewDecisionRequest? = nil, completion: @escaping (_ data: AdminApplication?, _ error: Error?) -> Void)
```

Reject an application

A reason is recorded in the audit trail; nothing is silently deleted.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let applicationId = 987 // UUID | 
let adminReviewDecisionRequest = AdminReviewDecisionRequest(reason: "reason_example", revision: 123) // AdminReviewDecisionRequest |  (optional)

// Reject an application
AdminReviewsAPI.adminReviewReject(applicationId: applicationId, adminReviewDecisionRequest: adminReviewDecisionRequest) { (response, error) in
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
 **applicationId** | **UUID** |  | 
 **adminReviewDecisionRequest** | [**AdminReviewDecisionRequest**](AdminReviewDecisionRequest.md) |  | [optional] 

### Return type

[**AdminApplication**](AdminApplication.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminReviewRetrieve**
```swift
    open class func adminReviewRetrieve(applicationId: UUID, completion: @escaping (_ data: AdminApplicationDetail?, _ error: Error?) -> Void)
```

Retrieve one application with its review context

Evidence is referenced by identifier only; content is fetched separately through the audited evidence endpoint.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let applicationId = 987 // UUID | 

// Retrieve one application with its review context
AdminReviewsAPI.adminReviewRetrieve(applicationId: applicationId) { (response, error) in
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
 **applicationId** | **UUID** |  | 

### Return type

[**AdminApplicationDetail**](AdminApplicationDetail.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminReviewsList**
```swift
    open class func adminReviewsList(category: String? = nil, cursor: String? = nil, evidence: String? = nil, from: String? = nil, kind: String? = nil, limit: Int? = nil, province: String? = nil, status: String? = nil, to: String? = nil, completion: @escaping (_ data: AdminApplicationList?, _ error: Error?) -> Void)
```

List facility applications awaiting or past review

Newest submission first, in cursor pages. Every filter is optional and combines with the rest. A draft that was never submitted sorts by when it was started.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let category = "category_example" // String | Category id of the facility the application belongs to. (optional)
let cursor = "cursor_example" // String | Opaque token returned as `nextCursor` by the previous page. (optional)
let evidence = "evidence_example" // String | `complete` or `incomplete`: whether every required document is uploaded. (optional)
let from = "from_example" // String | Submitted on or after this day (YYYY-MM-DD, Damascus) or this ISO datetime. (optional)
let kind = "kind_example" // String | Application kind, for example REGISTRATION or REVERIFICATION. (optional)
let limit = 987 // Int | Page size, maximum 200, default 50. (optional)
let province = "province_example" // String | Province id of the facility the application belongs to. (optional)
let status = "status_example" // String | Application status, for example SUBMITTED or APPROVED. (optional)
let to = "to_example" // String | Submitted on or before this day (YYYY-MM-DD, Damascus) or before this datetime. (optional)

// List facility applications awaiting or past review
AdminReviewsAPI.adminReviewsList(category: category, cursor: cursor, evidence: evidence, from: from, kind: kind, limit: limit, province: province, status: status, to: to) { (response, error) in
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
 **category** | **String** | Category id of the facility the application belongs to. | [optional] 
 **cursor** | **String** | Opaque token returned as &#x60;nextCursor&#x60; by the previous page. | [optional] 
 **evidence** | **String** | &#x60;complete&#x60; or &#x60;incomplete&#x60;: whether every required document is uploaded. | [optional] 
 **from** | **String** | Submitted on or after this day (YYYY-MM-DD, Damascus) or this ISO datetime. | [optional] 
 **kind** | **String** | Application kind, for example REGISTRATION or REVERIFICATION. | [optional] 
 **limit** | **Int** | Page size, maximum 200, default 50. | [optional] 
 **province** | **String** | Province id of the facility the application belongs to. | [optional] 
 **status** | **String** | Application status, for example SUBMITTED or APPROVED. | [optional] 
 **to** | **String** | Submitted on or before this day (YYYY-MM-DD, Damascus) or before this datetime. | [optional] 

### Return type

[**AdminApplicationList**](AdminApplicationList.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

