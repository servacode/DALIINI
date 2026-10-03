
# FavoriteFacility

## Properties
| Name | Type | Description | Notes |
| ------------ | ------------- | ------------- | ------------- |
| **id** | [**java.util.UUID**](java.util.UUID.md) |  |  |
| **slug** | **kotlin.String** | Readable words for the facility&#39;s link, from its Arabic name (&#x60;/f/{id}/{slug}&#x60;). Decoration only: the id is the address, so a rename never breaks a link. |  |
| **nameAr** | **kotlin.String** |  |  |
| **nameEn** | **kotlin.String** |  |  |
| **category** | [**BilingualRef**](BilingualRef.md) |  |  |
| **city** | [**NamedRef**](NamedRef.md) |  |  |
| **distanceMeters** | **kotlin.Double** | Great-circle distance in metres from the supplied coordinates. Null when no coordinates were supplied; clients never compute it locally. |  |
| **ratingAverage** | **kotlin.Double** |  |  |
| **ratingCount** | **kotlin.Int** |  |  |
| **availability** | [**Availability**](Availability.md) |  |  |
| **addressAr** | **kotlin.String** | Street address, so a row says where it is without being opened. |  |
| **neighborhood** | [**NamedRef**](NamedRef.md) |  |  |
| **phone** | **kotlin.String** | Public telephone number, so a row can be called without opening it. |  |
| **whatsapp** | **kotlin.String** | WhatsApp contact, E.164 Syrian mobile (+9639XXXXXXXX). |  |
| **location** | [**Coordinates**](Coordinates.md) | Where it is, so a row can be navigated to without opening it. |  |
| **isFavorite** | **kotlin.Boolean** | Whether the caller has saved this facility. False for anonymous callers; resolved for a whole page in one subquery. |  |
| **imageUrl** | [**java.net.URI**](java.net.URI.md) | The facility&#39;s first photograph, in the order its owner arranged them, or null when it has none. A public media URL; clients never build one. |  |
| **lastVerifiedAt** | [**java.time.OffsetDateTime**](java.time.OffsetDateTime.md) | When an operator last approved this facility&#39;s details (trust signal). |  |
| **infoConfirmedAt** | [**java.time.OffsetDateTime**](java.time.OffsetDateTime.md) | The most recent of &#x60;lastVerifiedAt&#x60; and the owner&#39;s own confirmation that the opening hours are still right. Null when neither ever happened. |  |
| **updatedAt** | [**java.time.OffsetDateTime**](java.time.OffsetDateTime.md) | Last change to the facility record. |  |
| **favoritedAt** | [**java.time.OffsetDateTime**](java.time.OffsetDateTime.md) | When the caller saved this facility. |  |



