# OwnerFacilityDetail

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**id** | **UUID** |  | 
**nameAr** | **String** |  | 
**category** | [**NamedRef**](NamedRef.md) |  | 
**province** | [**NamedRef**](NamedRef.md) |  | 
**status** | [**FacilityStatusEnum**](FacilityStatusEnum.md) |  | 
**lastUpdate** | **Date** |  | 
**requiredAction** | [**OwnerRequiredActionEnum**](OwnerRequiredActionEnum.md) |  | 
**capabilities** | [**CategoryCapabilities**](CategoryCapabilities.md) |  | 
**nameEn** | **String** |  | 
**descriptionAr** | **String** |  | 
**descriptionEn** | **String** |  | 
**phone** | **String** |  | 
**whatsapp** | **String** | E.164 Syrian mobile. | 
**addressAr** | **String** |  | 
**addressEn** | **String** |  | 
**cityId** | **UUID** |  | 
**neighborhoodId** | **UUID** |  | 
**location** | [**Coordinates**](Coordinates.md) |  | 
**specialtyIds** | **[UUID]** |  | 
**serviceTagIds** | **[UUID]** |  | 
**evidence** | [OwnerEvidenceRef] |  | 
**hours** | [OwnerHoursEntry] |  | 
**hoursConfirmedAt** | **Date** | When a member last confirmed the opening hours (or replaced them). The app asks again once this is a week old. | 
**application** | [**OwnerApplication**](OwnerApplication.md) |  | 

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


