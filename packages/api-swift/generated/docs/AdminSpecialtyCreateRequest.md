# AdminSpecialtyCreateRequest

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**nameAr** | **String** | Unique within its scope, retired items included. | 
**nameEn** | **String** |  | [optional] 
**active** | **Bool** |  | [optional] [default to true]
**sortOrder** | **Int** |  | [optional] [default to 0]
**scope** | [**SpecialtyScopeEnum**](SpecialtyScopeEnum.md) | CATEGORY scopes it to the category in the path. SPECIALIZATION shares it with every category of that category&#39;s specialization, and is refused for a GENERIC one.  * &#x60;CATEGORY&#x60; - One category * &#x60;SPECIALIZATION&#x60; - Every category of a specialization | 

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


