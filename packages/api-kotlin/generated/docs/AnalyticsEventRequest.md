
# AnalyticsEventRequest

## Properties
| Name | Type | Description | Notes |
| ------------ | ------------- | ------------- | ------------- |
| **name** | **kotlin.String** | Registered event name. |  |
| **properties** | [**kotlin.collections.Map&lt;kotlin.String, kotlin.Any&gt;**](kotlin.Any.md) | Event properties, restricted to the keys declared for this event. |  [optional] |
| **occurredAt** | [**java.time.OffsetDateTime**](java.time.OffsetDateTime.md) | Client-side event time, ISO-8601. Defaults to receipt time. |  [optional] |



