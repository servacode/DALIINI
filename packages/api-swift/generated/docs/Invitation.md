# Invitation

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**id** | **UUID** |  | 
**phone** | **String** |  | 
**role** | [**FacilityMemberRoleEnum**](FacilityMemberRoleEnum.md) |  | 
**status** | [**InvitationStatusEnum**](InvitationStatusEnum.md) | EXPIRED is a PENDING invitation past &#x60;expiresAt&#x60;.  * &#x60;PENDING&#x60; - PENDING * &#x60;ACCEPTED&#x60; - ACCEPTED * &#x60;DECLINED&#x60; - DECLINED * &#x60;REVOKED&#x60; - REVOKED * &#x60;EXPIRED&#x60; - EXPIRED | 
**createdAt** | **Date** |  | 
**expiresAt** | **Date** |  | 
**respondedAt** | **Date** |  | 

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


