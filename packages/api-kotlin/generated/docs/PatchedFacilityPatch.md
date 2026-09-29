
# PatchedFacilityPatch

## Properties
| Name | Type | Description | Notes |
| ------------ | ------------- | ------------- | ------------- |
| **nameAr** | **kotlin.String** |  |  [optional] |
| **nameEn** | **kotlin.String** |  |  [optional] |
| **descriptionAr** | **kotlin.String** |  |  [optional] |
| **descriptionEn** | **kotlin.String** |  |  [optional] |
| **phone** | **kotlin.String** |  |  [optional] |
| **whatsapp** | **kotlin.String** | Optional Syrian mobile (09XXXXXXXX or +9639XXXXXXXX); blank clears it. |  [optional] |
| **addressAr** | **kotlin.String** |  |  [optional] |
| **addressEn** | **kotlin.String** |  |  [optional] |
| **cityId** | [**java.util.UUID**](java.util.UUID.md) |  |  [optional] |
| **neighborhoodId** | [**java.util.UUID**](java.util.UUID.md) |  |  [optional] |
| **specialtyIds** | **kotlin.collections.List&lt;kotlin.Int&gt;** | Replaces the facility&#39;s specialties. Ids come from the category&#39;s &#x60;specialties&#x60; in ownerConfigRetrieve; an empty list clears them. |  [optional] |
| **serviceTagIds** | **kotlin.collections.List&lt;kotlin.Int&gt;** | Replaces the facility&#39;s services. Ids come from the category&#39;s &#x60;services&#x60; in ownerConfigRetrieve; an empty list clears them. |  [optional] |



