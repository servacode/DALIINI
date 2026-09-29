
# OwnerFacilityDetail

## Properties
| Name | Type | Description | Notes |
| ------------ | ------------- | ------------- | ------------- |
| **id** | [**java.util.UUID**](java.util.UUID.md) |  |  |
| **nameAr** | **kotlin.String** |  |  |
| **category** | [**NamedRef**](NamedRef.md) |  |  |
| **province** | [**NamedRef**](NamedRef.md) |  |  |
| **status** | [**FacilityStatusEnum**](FacilityStatusEnum.md) |  |  |
| **lastUpdate** | [**java.time.OffsetDateTime**](java.time.OffsetDateTime.md) |  |  |
| **requiredAction** | [**OwnerRequiredActionEnum**](OwnerRequiredActionEnum.md) |  |  |
| **capabilities** | [**CategoryCapabilities**](CategoryCapabilities.md) |  |  |
| **nameEn** | **kotlin.String** |  |  |
| **descriptionAr** | **kotlin.String** |  |  |
| **descriptionEn** | **kotlin.String** |  |  |
| **phone** | **kotlin.String** |  |  |
| **whatsapp** | **kotlin.String** | E.164 Syrian mobile. |  |
| **addressAr** | **kotlin.String** |  |  |
| **addressEn** | **kotlin.String** |  |  |
| **cityId** | [**java.util.UUID**](java.util.UUID.md) |  |  |
| **neighborhoodId** | [**java.util.UUID**](java.util.UUID.md) |  |  |
| **location** | [**Coordinates**](Coordinates.md) |  |  |
| **specialtyIds** | **kotlin.collections.List&lt;kotlin.Int&gt;** | The facility&#39;s active specialties, in order; retired ones are left out. |  |
| **serviceTagIds** | **kotlin.collections.List&lt;kotlin.Int&gt;** | The facility&#39;s active services, in order; retired ones are left out. |  |
| **evidence** | [**kotlin.collections.List&lt;OwnerEvidenceRef&gt;**](OwnerEvidenceRef.md) |  |  |
| **hours** | [**kotlin.collections.List&lt;OwnerHoursEntry&gt;**](OwnerHoursEntry.md) |  |  |
| **hoursConfirmedAt** | [**java.time.OffsetDateTime**](java.time.OffsetDateTime.md) | When a member last confirmed the opening hours (or replaced them). The app asks again once this is a week old. |  |
| **application** | [**OwnerApplication**](OwnerApplication.md) |  |  |



