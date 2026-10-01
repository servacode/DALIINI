
# AdminSpecialtyCreateRequest

## Properties
| Name | Type | Description | Notes |
| ------------ | ------------- | ------------- | ------------- |
| **nameAr** | **kotlin.String** | Unique within its scope, retired items included. |  |
| **scope** | [**SpecialtyScopeEnum**](SpecialtyScopeEnum.md) | CATEGORY scopes it to the category in the path. SPECIALIZATION shares it with every category of that category&#39;s specialization, and is refused for a GENERIC one.  * &#x60;CATEGORY&#x60; - One category * &#x60;SPECIALIZATION&#x60; - Every category of a specialization |  |
| **nameEn** | **kotlin.String** |  |  [optional] |
| **active** | **kotlin.Boolean** |  |  [optional] |
| **sortOrder** | **kotlin.Int** |  |  [optional] |



