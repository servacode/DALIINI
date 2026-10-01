
# EmergencyNumber

## Properties
| Name | Type | Description | Notes |
| ------------ | ------------- | ------------- | ------------- |
| **id** | [**java.util.UUID**](java.util.UUID.md) |  |  |
| **scope** | [**EmergencyNumberScopeEnum**](EmergencyNumberScopeEnum.md) | NATIONAL numbers apply everywhere; PROVINCE ones only to &#x60;provinceId&#x60;.  * &#x60;NATIONAL&#x60; - National * &#x60;PROVINCE&#x60; - Province |  |
| **provinceId** | [**java.util.UUID**](java.util.UUID.md) |  |  |
| **labelAr** | **kotlin.String** |  |  |
| **phone** | **kotlin.String** | What to dial, digits with an optional leading +. |  |
| **kind** | [**EmergencyNumberKindEnum**](EmergencyNumberKindEnum.md) |  |  |
| **sortOrder** | **kotlin.Int** |  |  |



