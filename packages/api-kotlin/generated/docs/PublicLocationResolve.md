
# PublicLocationResolve

## Properties
| Name | Type | Description | Notes |
| ------------ | ------------- | ------------- | ------------- |
| **province** | [**PublicProvince**](PublicProvince.md) |  |  |
| **city** | [**PublicPlace**](PublicPlace.md) |  |  |
| **neighborhood** | [**PublicPlace**](PublicPlace.md) |  |  |
| **label** | **kotlin.String** | What the app shows the user: the province, and the finer place when known. |  |
| **resolvedBy** | [**ResolvedByEnum**](ResolvedByEnum.md) | BOUNDARY when the point falls inside a seeded city or neighbourhood, NEAREST_PROVINCE when only the closest province centre could be used, NONE when the point is outside every province this platform serves.  * &#x60;BOUNDARY&#x60; - BOUNDARY * &#x60;NEAREST_PROVINCE&#x60; - NEAREST_PROVINCE * &#x60;NONE&#x60; - NONE |  |



