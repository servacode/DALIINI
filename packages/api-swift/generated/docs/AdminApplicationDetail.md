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
**evidenceComplete** | **Bool** | Whether every active, required document of the facility&#39;s category has its minimum number of files, as submission requires. False when a requirement was added after the application was sent. | 
**categoryNameAr** | **String** |  | 
**provinceNameAr** | **String** |  | 
**ownerName** | **String** |  | 
**ownerPhone** | **String** |  | 
**facility** | [**AdminFacility**](AdminFacility.md) |  | 
**snapshot** | **[String: AnyCodable]** | Redacted submission snapshot. | 
**previous** | **[String: AnyCodable]** | Snapshot of the last approved application of this facility (plus &#x60;approvedAt&#x60;), for diffing a REVERIFICATION; for a CHANGE, the facility as it is published now. Null when the facility was never approved. | 
**proposedFields** | **[String]** | CHANGE only: the fields the owner proposes to change; empty otherwise. | 
**revision** | **Int** | CHANGE only: send it back with the approval. 0 for other kinds. | 
**location** | [**Coordinates**](Coordinates.md) |  | 
**duplicates** | [AdminDuplicateCandidate] | Up to 5 other facilities with the same phone, or the same normalized Arabic name within 200 m. | 
**publicImageIds** | **[UUID]** |  | 
**publicImages** | [AdminPublicImage] |  | 
**evidence** | [AdminEvidenceRef] |  | 
**audit** | [AdminAuditTrailEntry] |  | 

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


