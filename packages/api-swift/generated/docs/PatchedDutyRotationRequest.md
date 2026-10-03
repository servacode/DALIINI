# PatchedDutyRotationRequest

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**name** | **String** |  | [optional] 
**provinceId** | **UUID** |  | [optional] 
**facilityIds** | **[UUID]** |  | [optional] 
**startsAt** | **String** | Damascus time, for example 20:00. | [optional] 
**endsAt** | **String** | At or before the start means the next morning. | [optional] 
**perDay** | **Int** |  | [optional] [default to 1]
**anchorDate** | **Date** | The day the first pharmacy of the list is on duty. | [optional] 

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


