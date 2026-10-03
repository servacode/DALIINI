# AdminFacilityCreate

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**categoryId** | **UUID** |  | 
**provinceId** | **UUID** |  | 
**cityId** | **UUID** |  | [optional] 
**neighborhoodId** | **UUID** |  | [optional] 
**nameAr** | **String** |  | 
**nameEn** | **String** |  | [optional] 
**descriptionAr** | **String** |  | [optional] 
**descriptionEn** | **String** |  | [optional] 
**phone** | **String** |  | [optional] 
**whatsapp** | **String** | Optional Syrian mobile (09XXXXXXXX or +9639XXXXXXXX); blank clears it. | [optional] 
**addressAr** | **String** |  | [optional] 
**addressEn** | **String** |  | [optional] 
**location** | [**Coordinates**](Coordinates.md) | Null removes the pin. | [optional] 
**specialtyIds** | **[Int]** | Replaces the facility&#39;s specialties; an empty list clears them. | [optional] 
**serviceTagIds** | **[Int]** | Replaces the facility&#39;s services; an empty list clears them. | [optional] 
**status** | [**AdminFacilityCreateStatusEnum**](AdminFacilityCreateStatusEnum.md) | ACTIVE (the default) publishes it at once; DRAFT keeps it hidden.  * &#x60;ACTIVE&#x60; - ACTIVE * &#x60;DRAFT&#x60; - DRAFT | [optional] 

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


