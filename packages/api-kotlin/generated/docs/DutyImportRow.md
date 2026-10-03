
# DutyImportRow

## Properties
| Name | Type | Description | Notes |
| ------------ | ------------- | ------------- | ------------- |
| **line** | **kotlin.Int** | Row number in the file (the header is line 1). |  |
| **facilityId** | [**java.util.UUID**](java.util.UUID.md) |  |  |
| **facilityNameAr** | **kotlin.String** |  |  |
| **startsAt** | [**java.time.OffsetDateTime**](java.time.OffsetDateTime.md) |  |  |
| **endsAt** | [**java.time.OffsetDateTime**](java.time.OffsetDateTime.md) |  |  |
| **outcome** | [**DutyImportRowOutcomeEnum**](DutyImportRowOutcomeEnum.md) |  |  |
| **problems** | **kotlin.collections.List&lt;kotlin.String&gt;** | Why the row cannot be applied; empty when it can. |  |



