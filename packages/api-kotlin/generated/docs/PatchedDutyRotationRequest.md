
# PatchedDutyRotationRequest

## Properties
| Name | Type | Description | Notes |
| ------------ | ------------- | ------------- | ------------- |
| **name** | **kotlin.String** |  |  [optional] |
| **provinceId** | [**java.util.UUID**](java.util.UUID.md) |  |  [optional] |
| **facilityIds** | [**kotlin.collections.List&lt;java.util.UUID&gt;**](java.util.UUID.md) |  |  [optional] |
| **startsAt** | **kotlin.String** | Damascus time, for example 20:00. |  [optional] |
| **endsAt** | **kotlin.String** | At or before the start means the next morning. |  [optional] |
| **perDay** | **kotlin.Int** |  |  [optional] |
| **anchorDate** | [**java.time.LocalDate**](java.time.LocalDate.md) | The day the first pharmacy of the list is on duty. |  [optional] |



