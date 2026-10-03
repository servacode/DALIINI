# AdminApplication

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**id** | **UUID** |  | 
**facilityId** | **UUID** |  | 
**facilityNameAr** | **String** |  | 
**kind** | [**FacilityApplicationKindEnum**](FacilityApplicationKindEnum.md) |  | 
**status** | [**FacilityApplicationStatusEnum**](FacilityApplicationStatusEnum.md) |  | 
**provinceId** | **UUID** |  | 
**categoryId** | **UUID** |  | 
**submittedAt** | **Date** |  | 
**reviewedAt** | **Date** |  | 
**rejectionReason** | **String** |  | 
**evidenceComplete** | **Bool** | Whether every active, required document of the facility&#39;s category has its minimum number of files, as submission requires. False when a requirement was added after the application was sent. | 
**categoryNameAr** | **String** |  | 
**provinceNameAr** | **String** |  | 
**ownerName** | **String** |  | 
**ownerPhone** | **String** |  | 
**applicantName** | **String** | CLAIM only: who asks to own the facility. | 
**applicantPhone** | **String** | CLAIM only. | 

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


