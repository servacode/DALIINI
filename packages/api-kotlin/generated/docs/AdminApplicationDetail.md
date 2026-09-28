
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
| **categoryNameAr** | **kotlin.String** |  |  |
| **provinceNameAr** | **kotlin.String** |  |  |
| **ownerName** | **kotlin.String** |  |  |
| **ownerPhone** | **kotlin.String** |  |  |
| **facility** | [**AdminFacility**](AdminFacility.md) |  |  |
| **snapshot** | [**kotlin.collections.Map&lt;kotlin.String, kotlinx.serialization.json.JsonElement&gt;**](kotlinx.serialization.json.JsonElement.md) | Redacted submission snapshot. |  |
| **previous** | [**kotlin.collections.Map&lt;kotlin.String, kotlinx.serialization.json.JsonElement&gt;**](kotlinx.serialization.json.JsonElement.md) | Snapshot of the last approved application of this facility (plus &#x60;approvedAt&#x60;), for diffing a REVERIFICATION. Null when the facility was never approved. |  |
| **location** | [**Coordinates**](Coordinates.md) |  |  |
| **duplicates** | [**kotlin.collections.List&lt;AdminDuplicateCandidate&gt;**](AdminDuplicateCandidate.md) | Up to 5 other facilities with the same phone, or the same normalized Arabic name within 200 m. |  |
| **publicImageIds** | [**kotlin.collections.List&lt;java.util.UUID&gt;**](java.util.UUID.md) |  |  |
| **publicImages** | [**kotlin.collections.List&lt;AdminPublicImage&gt;**](AdminPublicImage.md) |  |  |
| **evidence** | [**kotlin.collections.List&lt;AdminEvidenceRef&gt;**](AdminEvidenceRef.md) |  |  |
| **audit** | [**kotlin.collections.List&lt;AdminAuditTrailEntry&gt;**](AdminAuditTrailEntry.md) |  |  |



