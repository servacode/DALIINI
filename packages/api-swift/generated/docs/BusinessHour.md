# BusinessHour

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**id** | **UUID** |  | 
**weekday** | **Int** | 0 is Monday, matching Python weekday numbering. | 
**opensAt** | **String** |  | 
**closesAt** | **String** | A value earlier than opensAt denotes an overnight span. | 
**sequence** | **Int** | Ordering within a weekday, for categories that open in several spans. | 

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


