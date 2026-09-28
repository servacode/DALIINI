
# AdminDutyShift

## Properties
| Name | Type | Description | Notes |
| ------------ | ------------- | ------------- | ------------- |
| **id** | [**java.util.UUID**](java.util.UUID.md) |  |  |
| **facilityId** | [**java.util.UUID**](java.util.UUID.md) |  |  |
| **facilityNameAr** | **kotlin.String** |  |  |
| **cityId** | [**java.util.UUID**](java.util.UUID.md) |  |  |
| **startsAt** | [**java.time.OffsetDateTime**](java.time.OffsetDateTime.md) |  |  |
| **endsAt** | [**java.time.OffsetDateTime**](java.time.OffsetDateTime.md) |  |  |
| **createdBy** | [**DutyShiftSourceEnum**](DutyShiftSourceEnum.md) | Who put the shift on the roster.  * &#x60;OWNER&#x60; - Pharmacy owner or manager * &#x60;ADMIN&#x60; - Platform operator * &#x60;IMPORT&#x60; - Bulk import |  |
| **status** | [**DutyShiftStatusEnum**](DutyShiftStatusEnum.md) |  |  |



