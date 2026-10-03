# AdminHealthCheck

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**key** | [**AdminHealthCheckKeyEnum**](AdminHealthCheckKeyEnum.md) |  | 
**status** | [**AdminHealthStatusEnum**](AdminHealthStatusEnum.md) | &#x60;warning&#x60;: working, but someone should look. &#x60;off&#x60;: not used by this deployment (a development stack).  * &#x60;ok&#x60; - ok * &#x60;warning&#x60; - warning * &#x60;failed&#x60; - failed * &#x60;off&#x60; - off | 
**summary** | **String** | One sentence for the operator, in Arabic. | 
**latencyMs** | **Int** |  | 
**lastOkAt** | **Date** |  | 
**lastFailureAt** | **Date** |  | 
**metrics** | [AdminHealthMetric] |  | 

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


