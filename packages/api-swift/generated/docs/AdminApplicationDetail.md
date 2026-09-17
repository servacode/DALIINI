# AdminApplicationDetail

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
**facility** | [**AdminFacility**](AdminFacility.md) |  | 
**snapshot** | **[String: AnyCodable]** | Redacted submission snapshot. | 
**publicImageIds** | **[UUID]** |  | 
**evidence** | [AdminEvidenceRef] |  | 
**audit** | [AdminAuditTrailEntry] |  | 

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


