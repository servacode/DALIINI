
# AdminApplication

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
| **evidenceComplete** | **kotlin.Boolean** | Whether every active, required document of the facility&#39;s category has its minimum number of files, as submission requires. False when a requirement was added after the application was sent. |  |
| **categoryNameAr** | **kotlin.String** |  |  |
| **provinceNameAr** | **kotlin.String** |  |  |
| **ownerName** | **kotlin.String** |  |  |
| **ownerPhone** | **kotlin.String** |  |  |
| **applicantName** | **kotlin.String** | CLAIM only: who asks to own the facility. |  |
| **applicantPhone** | **kotlin.String** | CLAIM only. |  |



