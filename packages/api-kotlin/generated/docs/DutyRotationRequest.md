
# DutyRotationRequest

## Properties
| Name | Type | Description | Notes |
| ------------ | ------------- | ------------- | ------------- |
| **name** | **kotlin.String** |  |  |
| **provinceId** | [**java.util.UUID**](java.util.UUID.md) |  |  |
| **facilityIds** | [**kotlin.collections.List&lt;java.util.UUID&gt;**](java.util.UUID.md) |  |  |
| **startsAt** | **kotlin.String** | Damascus time, for example 20:00. |  |
| **endsAt** | **kotlin.String** | At or before the start means the next morning. |  |
| **anchorDate** | [**java.time.LocalDate**](java.time.LocalDate.md) | The day the first pharmacy of the list is on duty. |  |
| **perDay** | **kotlin.Int** |  |  [optional] |



