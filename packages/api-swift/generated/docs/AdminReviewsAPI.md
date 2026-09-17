# AdminReviewsAPI

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**adminEvidenceContentRetrieve**](AdminReviewsAPI.md#adminevidencecontentretrieve) | **GET** /api/v1/admin/evidence/{evidence_id}/content/ | Stream one piece of private verification evidence
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

# **adminReviewApprove**
```swift
    open class func adminReviewApprove(applicationId: UUID, adminDecisionRequest: AdminDecisionRequest? = nil, completion: @escaping (_ data: AdminApplication?, _ error: Error?) -> Void)
```

Approve an application

Runs in one transaction: the application and the facility lifecycle are locked, the current requirements are re-checked, the change is audited and the realtime event is emitted only after commit.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let applicationId = 987 // UUID | 
let adminDecisionRequest = AdminDecisionRequest(reason: "reason_example") // AdminDecisionRequest |  (optional)

// Approve an application
AdminReviewsAPI.adminReviewApprove(applicationId: applicationId, adminDecisionRequest: adminDecisionRequest) { (response, error) in
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
 **adminDecisionRequest** | [**AdminDecisionRequest**](AdminDecisionRequest.md) |  | [optional] 

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
    open class func adminReviewReject(applicationId: UUID, adminDecisionRequest: AdminDecisionRequest? = nil, completion: @escaping (_ data: AdminApplication?, _ error: Error?) -> Void)
```

Reject an application

A reason is recorded in the audit trail; nothing is silently deleted.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let applicationId = 987 // UUID | 
let adminDecisionRequest = AdminDecisionRequest(reason: "reason_example") // AdminDecisionRequest |  (optional)

// Reject an application
AdminReviewsAPI.adminReviewReject(applicationId: applicationId, adminDecisionRequest: adminDecisionRequest) { (response, error) in
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
 **adminDecisionRequest** | [**AdminDecisionRequest**](AdminDecisionRequest.md) |  | [optional] 

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
    open class func adminReviewsList(completion: @escaping (_ data: AdminApplicationList?, _ error: Error?) -> Void)
```

List facility applications awaiting or past review

Capped at 200 rows.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI


// List facility applications awaiting or past review
AdminReviewsAPI.adminReviewsList() { (response, error) in
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

[**AdminApplicationList**](AdminApplicationList.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

