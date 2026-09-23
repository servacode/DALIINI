
# CompactFacility

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



