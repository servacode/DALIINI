
# AdminFacilityQuality

## Properties
| Name | Type | Description | Notes |
| ------------ | ------------- | ------------- | ------------- |
| **id** | [**java.util.UUID**](java.util.UUID.md) |  |  |
| **nameAr** | **kotlin.String** |  |  |
| **nameEn** | **kotlin.String** |  |  |
| **categoryId** | [**java.util.UUID**](java.util.UUID.md) |  |  |
| **provinceId** | [**java.util.UUID**](java.util.UUID.md) |  |  |
| **cityId** | [**java.util.UUID**](java.util.UUID.md) |  |  |
| **status** | [**FacilityStatusEnum**](FacilityStatusEnum.md) |  |  |
| **location** | [**Coordinates**](Coordinates.md) |  |  |
| **updatedAt** | [**java.time.OffsetDateTime**](java.time.OffsetDateTime.md) |  |  |
| **categoryNameAr** | **kotlin.String** |  |  |
| **provinceNameAr** | **kotlin.String** |  |  |
| **ownerName** | **kotlin.String** | First owner membership. |  |
| **ownerPhone** | **kotlin.String** | First owner membership. |  |
| **qualityScore** | **kotlin.Int** | 100 minus a fixed penalty per issue. |  |
| **qualityIssues** | [**kotlin.collections.List&lt;FacilityQualityIssueEnum&gt;**](FacilityQualityIssueEnum.md) | NO_PHOTOS 10, NO_HOURS 15, NO_LOCATION 20, NO_PHONE 20, STALE 10 (nothing changed or confirmed for 90 days), OPEN_REPORTS 15, NOT_VERIFIED_RECENTLY 10 (never approved, or not in 180 days). Photos and hours count only where the category supports them. |  |



