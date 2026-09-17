
# AdminApplicationDetail

## Properties
| Name | Type | Description | Notes |
| ------------ | ------------- | ------------- | ------------- |
| **id** | [**java.util.UUID**](java.util.UUID.md) |  |  |
| **facilityId** | [**java.util.UUID**](java.util.UUID.md) |  |  |
| **facilityNameAr** | **kotlin.String** |  |  |
| **kind** | [**FacilityApplicationKindEnum**](FacilityApplicationKindEnum.md) |  |  |
| **status** | [**FacilityApplicationStatusEnum**](FacilityApplicationStatusEnum.md) |  |  |
| **provinceId** | [**java.util.UUID**](java.util.UUID.md) |  |  |
| **categoryId** | [**java.util.UUID**](java.util.UUID.md) |  |  |
| **submittedAt** | [**java.time.OffsetDateTime**](java.time.OffsetDateTime.md) |  |  |
| **reviewedAt** | [**java.time.OffsetDateTime**](java.time.OffsetDateTime.md) |  |  |
| **rejectionReason** | **kotlin.String** |  |  |
| **facility** | [**AdminFacility**](AdminFacility.md) |  |  |
| **snapshot** | [**kotlin.collections.Map&lt;kotlin.String, kotlin.Any&gt;**](kotlin.Any.md) | Redacted submission snapshot. |  |
| **publicImageIds** | [**kotlin.collections.List&lt;java.util.UUID&gt;**](java.util.UUID.md) |  |  |
| **evidence** | [**kotlin.collections.List&lt;AdminEvidenceRef&gt;**](AdminEvidenceRef.md) |  |  |
| **audit** | [**kotlin.collections.List&lt;AdminAuditTrailEntry&gt;**](AdminAuditTrailEntry.md) |  |  |



