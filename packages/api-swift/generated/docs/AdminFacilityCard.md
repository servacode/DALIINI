# AdminFacilityCard

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**id** | **UUID** |  | 
**nameAr** | **String** |  | 
**nameEn** | **String** |  | 
**categoryId** | **UUID** |  | 
**provinceId** | **UUID** |  | 
**cityId** | **UUID** |  | 
**status** | [**FacilityStatusEnum**](FacilityStatusEnum.md) |  | 
**location** | [**Coordinates**](Coordinates.md) |  | 
**updatedAt** | **Date** |  | 
**categoryNameAr** | **String** |  | 
**provinceNameAr** | **String** |  | 
**ownerName** | **String** | First owner membership. | 
**ownerPhone** | **String** | First owner membership. | 
**qualityScore** | **Int** | 100 minus a fixed penalty per issue. | 
**qualityIssues** | [FacilityQualityIssueEnum] | NO_PHOTOS 10, NO_HOURS 15, NO_LOCATION 20, NO_PHONE 20, STALE 10 (nothing changed or confirmed for 90 days), OPEN_REPORTS 15, NOT_VERIFIED_RECENTLY 10 (never approved, or not in 180 days). Photos and hours count only where the category supports them. | 
**phone** | **String** |  | 
**whatsapp** | **String** |  | 
**addressAr** | **String** |  | 
**cityNameAr** | **String** |  | 
**categoryIconKey** | **String** |  | 
**imageUrl** | **String** |  | 
**createdAt** | **Date** |  | 

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


