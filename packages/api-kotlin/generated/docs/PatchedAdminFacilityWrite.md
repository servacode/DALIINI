
# PatchedAdminFacilityWrite

## Properties
| Name | Type | Description | Notes |
| ------------ | ------------- | ------------- | ------------- |
| **categoryId** | [**java.util.UUID**](java.util.UUID.md) |  |  [optional] |
| **provinceId** | [**java.util.UUID**](java.util.UUID.md) |  |  [optional] |
| **cityId** | [**java.util.UUID**](java.util.UUID.md) |  |  [optional] |
| **neighborhoodId** | [**java.util.UUID**](java.util.UUID.md) |  |  [optional] |
| **nameAr** | **kotlin.String** |  |  [optional] |
| **nameEn** | **kotlin.String** |  |  [optional] |
| **descriptionAr** | **kotlin.String** |  |  [optional] |
| **descriptionEn** | **kotlin.String** |  |  [optional] |
| **phone** | **kotlin.String** |  |  [optional] |
| **whatsapp** | **kotlin.String** | Optional Syrian mobile (09XXXXXXXX or +9639XXXXXXXX); blank clears it. |  [optional] |
| **addressAr** | **kotlin.String** |  |  [optional] |
| **addressEn** | **kotlin.String** |  |  [optional] |
| **location** | [**Coordinates**](Coordinates.md) | Null removes the pin. |  [optional] |
| **specialtyIds** | **kotlin.collections.List&lt;kotlin.Int&gt;** | Replaces the facility&#39;s specialties; an empty list clears them. |  [optional] |
| **serviceTagIds** | **kotlin.collections.List&lt;kotlin.Int&gt;** | Replaces the facility&#39;s services; an empty list clears them. |  [optional] |



