
# AdminFacilityCreate

## Properties
| Name | Type | Description | Notes |
| ------------ | ------------- | ------------- | ------------- |
| **categoryId** | [**java.util.UUID**](java.util.UUID.md) |  |  |
| **provinceId** | [**java.util.UUID**](java.util.UUID.md) |  |  |
| **nameAr** | **kotlin.String** |  |  |
| **cityId** | [**java.util.UUID**](java.util.UUID.md) |  |  [optional] |
| **neighborhoodId** | [**java.util.UUID**](java.util.UUID.md) |  |  [optional] |
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
| **status** | [**AdminFacilityCreateStatusEnum**](AdminFacilityCreateStatusEnum.md) | ACTIVE (the default) publishes it at once; DRAFT keeps it hidden.  * &#x60;ACTIVE&#x60; - ACTIVE * &#x60;DRAFT&#x60; - DRAFT |  [optional] |



