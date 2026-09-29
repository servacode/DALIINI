# AdminSpecialty

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**id** | **Int** |  | 
**scope** | [**SpecialtyScopeEnum**](SpecialtyScopeEnum.md) | CATEGORY: offered by one category. SPECIALIZATION: offered by every category of the specialization.  * &#x60;CATEGORY&#x60; - One category * &#x60;SPECIALIZATION&#x60; - Every category of a specialization | 
**categoryId** | **UUID** | Set for the CATEGORY scope. | 
**specialization** | [**CategorySpecializationEnum**](CategorySpecializationEnum.md) | Set for the SPECIALIZATION scope.  * &#x60;GENERIC&#x60; - Generic * &#x60;PHARMACY&#x60; - Pharmacy * &#x60;MEDICAL_CLINIC&#x60; - Medical clinic * &#x60;NURSING_CENTER&#x60; - Nursing center | 
**nameAr** | **String** |  | 
**nameEn** | **String** |  | 
**active** | **Bool** | False retires it: off the public pages and filters, and out of owners&#39; choices. | 
**sortOrder** | **Int** |  | 
**facilityCount** | **Int** | Facilities that list it. Only an item no facility lists can be deleted; one in use is retired with &#x60;active &#x3D; false&#x60;. | 

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


