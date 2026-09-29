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
**lastVerifiedAt** | **Date** | When an operator last approved this facility&#39;s details (trust signal). | 
**infoConfirmedAt** | **Date** | The most recent of &#x60;lastVerifiedAt&#x60; and the owner&#39;s own confirmation that the opening hours are still right. Null when neither ever happened. | 
**updatedAt** | **Date** | Last change to the facility record. | 
**descriptionAr** | **String** |  | 
**descriptionEn** | **String** |  | 
**phone** | **String** |  | 
**whatsapp** | **String** | WhatsApp contact, E.164 Syrian mobile (+9639XXXXXXXX). | 
**addressAr** | **String** |  | 
**addressEn** | **String** |  | 
**neighborhood** | [**NamedRef**](NamedRef.md) |  | 
**location** | [**Coordinates**](Coordinates.md) |  | 
**images** | [FacilityImage] |  | 
**specialties** | [NamedIntRef] | Active specialties, in the operators&#39; order. | 
**services** | [NamedIntRef] | Active services, in the operators&#39; order. | 
**hours** | [PublicHoursEntry] |  | 

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


