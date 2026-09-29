
# AdminSpecialty

## Properties
| Name | Type | Description | Notes |
| ------------ | ------------- | ------------- | ------------- |
| **id** | **kotlin.Int** |  |  |
| **scope** | [**SpecialtyScopeEnum**](SpecialtyScopeEnum.md) | CATEGORY: offered by one category. SPECIALIZATION: offered by every category of the specialization.  * &#x60;CATEGORY&#x60; - One category * &#x60;SPECIALIZATION&#x60; - Every category of a specialization |  |
| **categoryId** | [**java.util.UUID**](java.util.UUID.md) | Set for the CATEGORY scope. |  |
| **specialization** | [**CategorySpecializationEnum**](CategorySpecializationEnum.md) | Set for the SPECIALIZATION scope.  * &#x60;GENERIC&#x60; - Generic * &#x60;PHARMACY&#x60; - Pharmacy * &#x60;MEDICAL_CLINIC&#x60; - Medical clinic * &#x60;NURSING_CENTER&#x60; - Nursing center |  |
| **nameAr** | **kotlin.String** |  |  |
| **nameEn** | **kotlin.String** |  |  |
| **active** | **kotlin.Boolean** | False retires it: off the public pages and filters, and out of owners&#39; choices. |  |
| **sortOrder** | **kotlin.Int** |  |  |
| **facilityCount** | **kotlin.Int** | Facilities that list it. Only an item no facility lists can be deleted; one in use is retired with &#x60;active &#x3D; false&#x60;. |  |



