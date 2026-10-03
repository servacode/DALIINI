# AdminDutyAPI

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**adminDutyImport**](AdminDutyAPI.md#admindutyimport) | **POST** /api/v1/admin/duty/import/ | Read a duty roster from a spreadsheet; preview it, or apply it
[**adminDutyRosterRetrieve**](AdminDutyAPI.md#admindutyrosterretrieve) | **GET** /api/v1/admin/duty/ | The duty roster of a province (or city), day by day
[**adminDutyRotationCreate**](AdminDutyAPI.md#admindutyrotationcreate) | **POST** /api/v1/admin/duty/rotations/ | Save a duty rotation
[**adminDutyRotationDelete**](AdminDutyAPI.md#admindutyrotationdelete) | **DELETE** /api/v1/admin/duty/rotations/{rotation_id}/ | Delete a saved duty rotation
[**adminDutyRotationGenerate**](AdminDutyAPI.md#admindutyrotationgenerate) | **POST** /api/v1/admin/duty/rotations/{rotation_id}/generate/ | Generate a period&#39;s shifts from a rotation; preview them, or apply them
[**adminDutyRotationUpdate**](AdminDutyAPI.md#admindutyrotationupdate) | **PATCH** /api/v1/admin/duty/rotations/{rotation_id}/ | Change a saved duty rotation
[**adminDutyRotationsList**](AdminDutyAPI.md#admindutyrotationslist) | **GET** /api/v1/admin/duty/rotations/ | Saved duty rotations
[**adminDutyShiftCreate**](AdminDutyAPI.md#admindutyshiftcreate) | **POST** /api/v1/admin/duty/ | Put a duty shift on a pharmacy&#39;s roster
[**adminDutyShiftDelete**](AdminDutyAPI.md#admindutyshiftdelete) | **DELETE** /api/v1/admin/duty/{shift_id}/ | Cancel a duty shift
[**adminDutyShiftUpdate**](AdminDutyAPI.md#admindutyshiftupdate) | **PATCH** /api/v1/admin/duty/{shift_id}/ | Move a duty shift


# **adminDutyImport**
```swift
    open class func adminDutyImport(file: URL, provinceId: UUID, apply: Bool? = nil, completion: @escaping (_ data: DutyImportResult?, _ error: Error?) -> Void)
```

Read a duty roster from a spreadsheet; preview it, or apply it

Columns in Arabic or English: the pharmacy (`facilityId`, `pharmacy`/`الصيدلية` by name, or `phone`/`الهاتف`) and either `date`/`التاريخ` with `from`/`من` and `to`/`إلى` in Damascus time (an end at or before the start is the next morning), or `startsAt` and `endsAt`. Every row is checked against the province's pharmacies and the stored shifts. `apply` writes all rows or none, and only when no row has an error; re-applying the same file changes nothing. At most 2000 rows.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let file = URL(string: "https://example.com")! // URL | CSV (UTF-8) or XLSX, first sheet, header row first.
let provinceId = 987 // UUID | 
let apply = true // Bool | False previews; true writes, refused if any row has an error. (optional) (default to false)

// Read a duty roster from a spreadsheet; preview it, or apply it
AdminDutyAPI.adminDutyImport(file: file, provinceId: provinceId, apply: apply) { (response, error) in
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
 **file** | **URL** | CSV (UTF-8) or XLSX, first sheet, header row first. | 
 **provinceId** | **UUID** |  | 
 **apply** | **Bool** | False previews; true writes, refused if any row has an error. | [optional] [default to false]

### Return type

[**DutyImportResult**](DutyImportResult.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminDutyRosterRetrieve**
```swift
    open class func adminDutyRosterRetrieve(provinceId: String, cityId: String? = nil, from: String? = nil, to: String? = nil, completion: @escaping (_ data: AdminDutyRoster?, _ error: Error?) -> Void)
```

The duty roster of a province (or city), day by day

Days are Damascus calendar days from `from` to `to` inclusive, by default today and the next 13 days, at most 62. Each day lists the shifts of ACTIVE duty pharmacies overlapping it, and `gap` is true when there is none: the rule behind the DUTY_GAP alert. With `cityId`, shifts and gaps are those of that city's pharmacies.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let provinceId = "provinceId_example" // String | 
let cityId = "cityId_example" // String |  (optional)
let from = "from_example" // String | YYYY-MM-DD (optional)
let to = "to_example" // String | YYYY-MM-DD (optional)

// The duty roster of a province (or city), day by day
AdminDutyAPI.adminDutyRosterRetrieve(provinceId: provinceId, cityId: cityId, from: from, to: to) { (response, error) in
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
 **provinceId** | **String** |  | 
 **cityId** | **String** |  | [optional] 
 **from** | **String** | YYYY-MM-DD | [optional] 
 **to** | **String** | YYYY-MM-DD | [optional] 

### Return type

[**AdminDutyRoster**](AdminDutyRoster.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminDutyRotationCreate**
```swift
    open class func adminDutyRotationCreate(dutyRotationRequest: DutyRotationRequest, completion: @escaping (_ data: DutyRotation?, _ error: Error?) -> Void)
```

Save a duty rotation

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let dutyRotationRequest = DutyRotationRequest(name: "name_example", provinceId: 123, facilityIds: [123], startsAt: "startsAt_example", endsAt: "endsAt_example", perDay: 123, anchorDate: Date()) // DutyRotationRequest | 

// Save a duty rotation
AdminDutyAPI.adminDutyRotationCreate(dutyRotationRequest: dutyRotationRequest) { (response, error) in
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
 **dutyRotationRequest** | [**DutyRotationRequest**](DutyRotationRequest.md) |  | 

### Return type

[**DutyRotation**](DutyRotation.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminDutyRotationDelete**
```swift
    open class func adminDutyRotationDelete(rotationId: UUID, completion: @escaping (_ data: Void?, _ error: Error?) -> Void)
```

Delete a saved duty rotation

The shifts it generated stay; only the template goes.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let rotationId = 987 // UUID | 

// Delete a saved duty rotation
AdminDutyAPI.adminDutyRotationDelete(rotationId: rotationId) { (response, error) in
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
 **rotationId** | **UUID** |  | 

### Return type

Void (empty response body)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminDutyRotationGenerate**
```swift
    open class func adminDutyRotationGenerate(rotationId: UUID, dutyRotationGenerate: DutyRotationGenerate, completion: @escaping (_ data: DutyImportResult?, _ error: Error?) -> Void)
```

Generate a period's shifts from a rotation; preview them, or apply them

Up to three months at a time. The same checks and all-or-nothing writing as an import; applying a period twice changes nothing.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let rotationId = 987 // UUID | 
let dutyRotationGenerate = DutyRotationGenerate(fromDate: Date(), toDate: Date(), apply: false) // DutyRotationGenerate | 

// Generate a period's shifts from a rotation; preview them, or apply them
AdminDutyAPI.adminDutyRotationGenerate(rotationId: rotationId, dutyRotationGenerate: dutyRotationGenerate) { (response, error) in
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
 **rotationId** | **UUID** |  | 
 **dutyRotationGenerate** | [**DutyRotationGenerate**](DutyRotationGenerate.md) |  | 

### Return type

[**DutyImportResult**](DutyImportResult.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminDutyRotationUpdate**
```swift
    open class func adminDutyRotationUpdate(rotationId: UUID, patchedDutyRotationRequest: PatchedDutyRotationRequest? = nil, completion: @escaping (_ data: DutyRotation?, _ error: Error?) -> Void)
```

Change a saved duty rotation

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let rotationId = 987 // UUID | 
let patchedDutyRotationRequest = PatchedDutyRotationRequest(name: "name_example", provinceId: 123, facilityIds: [123], startsAt: "startsAt_example", endsAt: "endsAt_example", perDay: 123, anchorDate: Date()) // PatchedDutyRotationRequest |  (optional)

// Change a saved duty rotation
AdminDutyAPI.adminDutyRotationUpdate(rotationId: rotationId, patchedDutyRotationRequest: patchedDutyRotationRequest) { (response, error) in
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
 **rotationId** | **UUID** |  | 
 **patchedDutyRotationRequest** | [**PatchedDutyRotationRequest**](PatchedDutyRotationRequest.md) |  | [optional] 

### Return type

[**DutyRotation**](DutyRotation.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminDutyRotationsList**
```swift
    open class func adminDutyRotationsList(completion: @escaping (_ data: DutyRotationList?, _ error: Error?) -> Void)
```

Saved duty rotations

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI


// Saved duty rotations
AdminDutyAPI.adminDutyRotationsList() { (response, error) in
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

[**DutyRotationList**](DutyRotationList.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminDutyShiftCreate**
```swift
    open class func adminDutyShiftCreate(adminDutyShiftCreateRequest: AdminDutyShiftCreateRequest, completion: @escaping (_ data: AdminDutyShift?, _ error: Error?) -> Void)
```

Put a duty shift on a pharmacy's roster

Same rules as the owner endpoint: the category must support duty (409 DUTY_NOT_SUPPORTED), the shift must not overlap another of the same pharmacy or be invalid (409 DUTY_OVERLAP_OR_INVALID) and must not fall in a temporary closure (409 DUTY_DURING_CLOSURE). Recorded with `createdBy` ADMIN, audited, and the pharmacy's owners are notified. Sending a shift identical to an existing one returns it with 200 and changes nothing.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let adminDutyShiftCreateRequest = AdminDutyShiftCreateRequest(facilityId: 123, startsAt: Date(), endsAt: Date()) // AdminDutyShiftCreateRequest | 

// Put a duty shift on a pharmacy's roster
AdminDutyAPI.adminDutyShiftCreate(adminDutyShiftCreateRequest: adminDutyShiftCreateRequest) { (response, error) in
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
 **adminDutyShiftCreateRequest** | [**AdminDutyShiftCreateRequest**](AdminDutyShiftCreateRequest.md) |  | 

### Return type

[**AdminDutyShift**](AdminDutyShift.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminDutyShiftDelete**
```swift
    open class func adminDutyShiftDelete(shiftId: UUID, completion: @escaping (_ data: Void?, _ error: Error?) -> Void)
```

Cancel a duty shift

Audited; the pharmacy's owners are notified.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let shiftId = 987 // UUID | 

// Cancel a duty shift
AdminDutyAPI.adminDutyShiftDelete(shiftId: shiftId) { (response, error) in
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
 **shiftId** | **UUID** |  | 

### Return type

Void (empty response body)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adminDutyShiftUpdate**
```swift
    open class func adminDutyShiftUpdate(shiftId: UUID, patchedAdminDutyShiftUpdateRequest: PatchedAdminDutyShiftUpdateRequest? = nil, completion: @escaping (_ data: AdminDutyShift?, _ error: Error?) -> Void)
```

Move a duty shift

Same validation as creating one. `createdBy` keeps who created the shift. Audited; the pharmacy's owners are notified when the times change.

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import ServaDirectoryAPI

let shiftId = 987 // UUID | 
let patchedAdminDutyShiftUpdateRequest = PatchedAdminDutyShiftUpdateRequest(startsAt: Date(), endsAt: Date()) // PatchedAdminDutyShiftUpdateRequest |  (optional)

// Move a duty shift
AdminDutyAPI.adminDutyShiftUpdate(shiftId: shiftId, patchedAdminDutyShiftUpdateRequest: patchedAdminDutyShiftUpdateRequest) { (response, error) in
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
 **shiftId** | **UUID** |  | 
 **patchedAdminDutyShiftUpdateRequest** | [**PatchedAdminDutyShiftUpdateRequest**](PatchedAdminDutyShiftUpdateRequest.md) |  | [optional] 

### Return type

[**AdminDutyShift**](AdminDutyShift.md)

### Authorization

[bearerAccessToken](../README.md#bearerAccessToken)

### HTTP request headers

 - **Content-Type**: application/json, application/x-www-form-urlencoded, multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

