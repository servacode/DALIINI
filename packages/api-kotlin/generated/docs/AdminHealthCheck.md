
# AdminHealthCheck

## Properties
| Name | Type | Description | Notes |
| ------------ | ------------- | ------------- | ------------- |
| **key** | [**AdminHealthCheckKeyEnum**](AdminHealthCheckKeyEnum.md) |  |  |
| **status** | [**AdminHealthStatusEnum**](AdminHealthStatusEnum.md) | &#x60;warning&#x60;: working, but someone should look. &#x60;off&#x60;: not used by this deployment (a development stack).  * &#x60;ok&#x60; - ok * &#x60;warning&#x60; - warning * &#x60;failed&#x60; - failed * &#x60;off&#x60; - off |  |
| **summary** | **kotlin.String** | One sentence for the operator, in Arabic. |  |
| **latencyMs** | **kotlin.Int** |  |  |
| **lastOkAt** | [**java.time.OffsetDateTime**](java.time.OffsetDateTime.md) |  |  |
| **lastFailureAt** | [**java.time.OffsetDateTime**](java.time.OffsetDateTime.md) |  |  |
| **metrics** | [**kotlin.collections.List&lt;AdminHealthMetric&gt;**](AdminHealthMetric.md) |  |  |



