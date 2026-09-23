# Notification

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**id** | **UUID** |  | 
**type** | **String** | What happened, as a stable code. | 
**titleAr** | **String** |  | 
**bodyAr** | **String** |  | 
**destination** | [**DestinationEnum**](DestinationEnum.md) | Where opening this message takes the reader. The set is closed on purpose: a notification can never carry an arbitrary link.  * &#x60;NONE&#x60; - NONE * &#x60;FACILITY&#x60; - FACILITY * &#x60;OWNER_FACILITIES&#x60; - OWNER_FACILITIES | 
**facilityId** | **UUID** | Set only when the destination is FACILITY. | 
**isRead** | **Bool** |  | 
**createdAt** | **Date** |  | 

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


