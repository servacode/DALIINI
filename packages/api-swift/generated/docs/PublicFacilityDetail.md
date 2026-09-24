# PublicFacilityDetail

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**id** | **UUID** |  | 
**nameAr** | **String** |  | 
**nameEn** | **String** |  | 
**category** | [**BilingualRef**](BilingualRef.md) |  | 
**city** | [**NamedRef**](NamedRef.md) |  | 
**distanceMeters** | **Double** | Great-circle distance in metres from the supplied coordinates. Null when no coordinates were supplied; clients never compute it locally. | 
**ratingAverage** | **Double** |  | 
**ratingCount** | **Int** |  | 
**availability** | [**Availability**](Availability.md) |  | 
**isFavorite** | **Bool** | Whether the caller has saved this facility. False for anonymous callers; resolved for a whole page in one subquery. | 
**imageUrl** | **String** | The facility&#39;s first photograph, in the order its owner arranged them, or null when it has none. A public media URL; clients never build one. | 
**descriptionAr** | **String** |  | 
**descriptionEn** | **String** |  | 
**phone** | **String** |  | 
**addressAr** | **String** |  | 
**addressEn** | **String** |  | 
**neighborhood** | [**NamedRef**](NamedRef.md) |  | 
**location** | [**Coordinates**](Coordinates.md) |  | 
**images** | [FacilityImage] |  | 
**specialties** | [NamedRef] |  | 
**services** | [NamedRef] |  | 
**hours** | [PublicHoursEntry] |  | 

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


