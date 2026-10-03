
# Invitation

## Properties
| Name | Type | Description | Notes |
| ------------ | ------------- | ------------- | ------------- |
| **id** | [**java.util.UUID**](java.util.UUID.md) |  |  |
| **phone** | **kotlin.String** |  |  |
| **role** | [**FacilityMemberRoleEnum**](FacilityMemberRoleEnum.md) |  |  |
| **status** | [**InvitationStatusEnum**](InvitationStatusEnum.md) | EXPIRED is a PENDING invitation past &#x60;expiresAt&#x60;.  * &#x60;PENDING&#x60; - PENDING * &#x60;ACCEPTED&#x60; - ACCEPTED * &#x60;DECLINED&#x60; - DECLINED * &#x60;REVOKED&#x60; - REVOKED * &#x60;EXPIRED&#x60; - EXPIRED |  |
| **createdAt** | [**java.time.OffsetDateTime**](java.time.OffsetDateTime.md) |  |  |
| **expiresAt** | [**java.time.OffsetDateTime**](java.time.OffsetDateTime.md) |  |  |
| **respondedAt** | [**java.time.OffsetDateTime**](java.time.OffsetDateTime.md) |  |  |



