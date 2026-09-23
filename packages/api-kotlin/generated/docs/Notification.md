
# Notification

## Properties
| Name | Type | Description | Notes |
| ------------ | ------------- | ------------- | ------------- |
| **id** | [**java.util.UUID**](java.util.UUID.md) |  |  |
| **type** | **kotlin.String** | What happened, as a stable code. |  |
| **titleAr** | **kotlin.String** |  |  |
| **bodyAr** | **kotlin.String** |  |  |
| **destination** | [**DestinationEnum**](DestinationEnum.md) | Where opening this message takes the reader. The set is closed on purpose: a notification can never carry an arbitrary link.  * &#x60;NONE&#x60; - NONE * &#x60;FACILITY&#x60; - FACILITY * &#x60;OWNER_FACILITIES&#x60; - OWNER_FACILITIES |  |
| **facilityId** | [**java.util.UUID**](java.util.UUID.md) | Set only when the destination is FACILITY. |  |
| **isRead** | **kotlin.Boolean** |  |  |
| **createdAt** | [**java.time.OffsetDateTime**](java.time.OffsetDateTime.md) |  |  |



