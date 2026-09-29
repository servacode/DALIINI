# Notification

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**id** | **UUID** |  | 
**type** | **String** | What happened, as a stable code. | 
**titleAr** | **String** |  | 
**bodyAr** | **String** |  | 
**destination** | [**DestinationEnum**](DestinationEnum.md) | Where opening this message takes the reader. The set is closed on purpose: a notification can never carry an arbitrary link.  * &#x60;NONE&#x60; - NONE * &#x60;FACILITY&#x60; - FACILITY * &#x60;OWNER_FACILITIES&#x60; - OWNER_FACILITIES | 
**facilityId** | **UUID** | The facility the message is about: set for a FACILITY destination, and for an OWNER_FACILITIES notice about one of the owner&#39;s facilities (an hours reminder, a duty change); null otherwise. | 
**isRead** | **Bool** |  | 
**createdAt** | **Date** |  | 

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


