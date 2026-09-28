
# PublicFacilityDetail

## Properties
| Name | Type | Description | Notes |
| ------------ | ------------- | ------------- | ------------- |
| **id** | [**java.util.UUID**](java.util.UUID.md) |  |  |
| **nameAr** | **kotlin.String** |  |  |
| **nameEn** | **kotlin.String** |  |  |
| **category** | [**BilingualRef**](BilingualRef.md) |  |  |
| **city** | [**NamedRef**](NamedRef.md) |  |  |
| **distanceMeters** | **kotlin.Double** | Great-circle distance in metres from the supplied coordinates. Null when no coordinates were supplied; clients never compute it locally. |  |
| **ratingAverage** | **kotlin.Double** |  |  |
| **ratingCount** | **kotlin.Int** |  |  |
| **availability** | [**Availability**](Availability.md) |  |  |
| **isFavorite** | **kotlin.Boolean** | Whether the caller has saved this facility. False for anonymous callers; resolved for a whole page in one subquery. |  |
| **imageUrl** | [**java.net.URI**](java.net.URI.md) | The facility&#39;s first photograph, in the order its owner arranged them, or null when it has none. A public media URL; clients never build one. |  |
| **lastVerifiedAt** | [**java.time.OffsetDateTime**](java.time.OffsetDateTime.md) | When an operator last approved this facility&#39;s details (trust signal). |  |
| **updatedAt** | [**java.time.OffsetDateTime**](java.time.OffsetDateTime.md) | Last change to the facility record. |  |
| **descriptionAr** | **kotlin.String** |  |  |
| **descriptionEn** | **kotlin.String** |  |  |
| **phone** | **kotlin.String** |  |  |
| **whatsapp** | **kotlin.String** | WhatsApp contact, E.164 Syrian mobile (+9639XXXXXXXX). |  |
| **addressAr** | **kotlin.String** |  |  |
| **addressEn** | **kotlin.String** |  |  |
| **neighborhood** | [**NamedRef**](NamedRef.md) |  |  |
| **location** | [**Coordinates**](Coordinates.md) |  |  |
| **images** | [**kotlin.collections.List&lt;FacilityImage&gt;**](FacilityImage.md) |  |  |
| **specialties** | [**kotlin.collections.List&lt;NamedRef&gt;**](NamedRef.md) |  |  |
| **services** | [**kotlin.collections.List&lt;NamedRef&gt;**](NamedRef.md) |  |  |
| **hours** | [**kotlin.collections.List&lt;PublicHoursEntry&gt;**](PublicHoursEntry.md) |  |  |



