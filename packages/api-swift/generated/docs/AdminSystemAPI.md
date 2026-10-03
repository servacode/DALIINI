# AdminSystemAPI

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**adminAlertsList**](AdminSystemAPI.md#adminalertslist) | **GET** /api/v1/admin/alerts/ | Smart alerts: problems worth acting on now
[**adminDashboardRetrieve**](AdminSystemAPI.md#admindashboardretrieve) | **GET** /api/v1/admin/dashboard/ | Operational counters for the review desk
[**adminMeRetrieve**](AdminSystemAPI.md#adminmeretrieve) | **GET** /api/v1/admin/me/ | The current operator and the permissions they hold
[**adminSearchRetrieve**](AdminSystemAPI.md#adminsearchretrieve) | **GET** /api/v1/admin/search/ | Search facilities, users and applications at once
[**adminSystemStatusRetrieve**](AdminSystemAPI.md#adminsystemstatusretrieve) | **GET** /api/v1/admin/system/status/ | Every dependency, asked directly
[**adminTasksRetrieve**](AdminSystemAPI.md#admintasksretrieve) | **GET** /api/v1/admin/tasks/ | The operator&#39;s queue: what is waiting, oldest first


# **adminAlertsList**
```swift
    open class func adminAlertsList(completion: @escaping (_ data: AdminAlertList?, _ error: Error?) -> Void)
```

Smart alerts: problems worth acting on now

DUTY_GAP: per province offering a duty category, the Damascus days of the next 14 with no duty shift of any ACTIVE pharmacy (critical when the first gap is today or tomorrow). STALE_FACILITY: ACTIVE facilities with no change, owner confirmation or approval for 90 days. REPORTED_FACILITY: 3 or more open reports (critical from 5). ZERO_RESULT_SEARCH: searches without results in the last 7 days, grouped by province and category because search text is never recorded. REVIEW_OVERDUE: submitted applications past the SLA (critical past twice it). MAINTENANCE_ON.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI


// Smart alerts: problems worth acting on now
AdminSystemAPI.adminAlertsList() { (response, error) in
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

[**AdminAlertList**](AdminAlertList.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminDashboardRetrieve**
```swift
    open class func adminDashboardRetrieve(completion: @escaping (_ data: AdminDashboard?, _ error: Error?) -> Void)
```

Operational counters for the review desk

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI


// Operational counters for the review desk
AdminSystemAPI.adminDashboardRetrieve() { (response, error) in
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

[**AdminDashboard**](AdminDashboard.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminMeRetrieve**
```swift
    open class func adminMeRetrieve(completion: @escaping (_ data: AdminMe?, _ error: Error?) -> Void)
```

The current operator and the permissions they hold

Drives navigation visibility and action gating in the Admin. A UI gate is not authorization: every endpoint re-checks, and a permission revoked mid-session surfaces as a 403 on the next call.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI


// The current operator and the permissions they hold
AdminSystemAPI.adminMeRetrieve() { (response, error) in
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

[**AdminMe**](AdminMe.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminSearchRetrieve**
```swift
    open class func adminSearchRetrieve(q: String, completion: @escaping (_ data: AdminSearchResult?, _ error: Error?) -> Void)
```

Search facilities, users and applications at once

Up to 5 hits per group. FACILITY (admin.facilities.read): Arabic or English name, or phone digits. USER (admin.users.read, or admin.facilities.read with the phone masked to its last 4 digits): name or phone digits. APPLICATION (admin.reviews.read): facility name. A group the caller may not read is left out, not returned empty.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let q = "q_example" // String | At least 2 chars.

// Search facilities, users and applications at once
AdminSystemAPI.adminSearchRetrieve(q: q) { (response, error) in
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
 **q** | **String** | At least 2 chars. | 

### Return type

[**AdminSearchResult**](AdminSearchResult.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminSystemStatusRetrieve**
```swift
    open class func adminSystemStatusRetrieve(completion: @escaping (_ data: AdminSystemStatus?, _ error: Error?) -> Void)
```

Every dependency, asked directly

The database, Redis and the workers, the scheduler's heartbeat, storage, the verification-code channel, push, backups, error reporting and maintenance mode, each with a status and a sentence (DECISION-073). Probes time out after two seconds. No host, URL, credential or exception text is returned.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI


// Every dependency, asked directly
AdminSystemAPI.adminSystemStatusRetrieve() { (response, error) in
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

[**AdminSystemStatus**](AdminSystemStatus.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminTasksRetrieve**
```swift
    open class func adminTasksRetrieve(completion: @escaping (_ data: AdminTasks?, _ error: Error?) -> Void)
```

The operator's queue: what is waiting, oldest first

Submitted applications split into INITIAL and REVERIFICATION, open problem reports grouped by facility (facilities with 2 or more open reports first) and facilities waiting in REVERIFICATION_REQUIRED. Each bucket has its count, how many are past the SLA (platform setting `review.slaHours`, default 48) and up to 10 oldest items with their age in hours and an `overdue` flag.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI


// The operator's queue: what is waiting, oldest first
AdminSystemAPI.adminTasksRetrieve() { (response, error) in
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

[**AdminTasks**](AdminTasks.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

